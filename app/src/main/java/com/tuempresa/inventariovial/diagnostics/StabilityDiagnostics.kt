package com.tuempresa.inventariovial.diagnostics

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.tuempresa.inventariovial.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Strict allowlist: never serialize Throwable.message, request payloads, state, or OS description/trace. */
object StabilityDiagnostics {
    @Volatile private var screen="STARTUP"
    private var installed=false
    private val modules=setOf("STARTUP","HOME","AUTH","TABLET_ADMIN","DIAGNOSTICS","SCAP","PAVEMENT","INVENTORY","SIGNALS","MAP","CAMERA","BACKGROUND")
    fun screen(value:String){screen=value.takeIf {it in modules} ?: "INVENTORY"}
    fun memory(context:Context):JSONObject {
        val runtime=Runtime.getRuntime()
        val info=ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        return JSONObject().put("heapUsedBytes",runtime.totalMemory()-runtime.freeMemory()).put("heapMaxBytes",runtime.maxMemory())
            .put("systemAvailableBytes",info.availMem).put("systemLowMemory",info.lowMemory)
    }
    fun safeStack(error:Throwable):JSONArray = JSONArray(error.stackTrace.take(24).map {
        // No exception message, source paths, function arguments or nested arbitrary strings.
        "${it.className.replace(Regex("[^A-Za-z0-9_.$]"),"")}.${it.methodName.replace(Regex("[^A-Za-z0-9_$<>]"),"")}:${it.lineNumber}"
    })
    fun reason(code:Int):String=when(code) {
        ApplicationExitInfo.REASON_CRASH,ApplicationExitInfo.REASON_CRASH_NATIVE->"CRASH"
        ApplicationExitInfo.REASON_ANR->"ANR"
        ApplicationExitInfo.REASON_LOW_MEMORY->"LOW_MEMORY"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE->"EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_USER_REQUESTED->"USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED->"USER_STOPPED"
        ApplicationExitInfo.REASON_SIGNALED->"SIGNALED"
        ApplicationExitInfo.REASON_UNKNOWN->"Causa no disponible."
        else->"OTHER"
    }
    private fun base(context:Context,event:String)=JSONObject().put("timestamp",System.currentTimeMillis()).put("event",event)
        .put("versionName",BuildConfig.VERSION_NAME).put("versionCode",BuildConfig.VERSION_CODE).put("androidApi",Build.VERSION.SDK_INT)
        .put("androidVersion",Build.VERSION.RELEASE).put("manufacturer",Build.MANUFACTURER).put("model",Build.MODEL)
        .put("module",screen).put("memory",memory(context))
    @Synchronized fun record(context:Context,event:String,error:Throwable?=null,trimLevel:Int?=null) {
        runCatching {
            require(event in setOf("START","FOREGROUND","BACKGROUND","TRIM_MEMORY","UNCAUGHT_EXCEPTION"))
            val prefs=context.getSharedPreferences("stability_diagnostics",Context.MODE_PRIVATE)
            val old=JSONArray(prefs.getString("events","[]"));val row=base(context,event)
            if(error!=null)row.put("exceptionClass",error.javaClass.name).put("stack",safeStack(error))
            trimLevel?.let{row.put("trimLevel",it)}
            val next=JSONArray();for(i in maxOf(0,old.length()-63) until old.length())next.put(old.getJSONObject(i));next.put(row)
            // Crash handler commits the tiny bounded record before delegating to Android's handler.
            prefs.edit().putString("events",next.toString()).commit()
            if(event=="UNCAUGHT_EXCEPTION")prefs.edit().putString("lastUncaught",JSONObject().put("timestamp",row.getLong("timestamp")).put("reason","CRASH").put("source","LOCAL_UNCAUGHT_HANDLER").toString()).commit()
        }
    }
    @Synchronized fun install(app:Application) {
        if(installed)return;installed=true
        val original=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler {thread,error->
            try{record(app,"UNCAUGHT_EXCEPTION",error)} finally{original?.uncaughtException(thread,error)}
        }
        val prefs=app.getSharedPreferences("stability_diagnostics",Context.MODE_PRIVATE)
        if(Build.VERSION.SDK_INT>=30)runCatching {
            val info=app.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(app.packageName,0,1).firstOrNull()
            if(info!=null)prefs.edit().putString("lastExit",JSONObject().put("timestamp",info.timestamp)
                .put("reasonCode",info.reason).put("reason",reason(info.reason)).put("sampledPssKb",info.pss).put("sampledRssKb",info.rss).toString()).apply()
        }
        record(app,"START")
    }
    fun report(context:Context):JSONObject {
        val prefs=context.getSharedPreferences("stability_diagnostics",Context.MODE_PRIVATE)
        val local=prefs.getString("lastUncaught",null)?.let{JSONObject(it)}
        val os=prefs.getString("lastExit",null)?.let{JSONObject(it)}
        val last=if(os!=null && (local==null||os.optLong("timestamp")>=local.optLong("timestamp")))os else local
        return base(context,"REPORT").put("lastExit",last ?: JSONObject().put("reason","Causa no disponible."))
            .put("events",JSONArray(prefs.getString("events","[]")))
    }
    fun export(context:Context):File {
        val dir=File(context.cacheDir,"diagnostics").apply{mkdirs()}
        return File(dir,"gbo_diagnostics_${SimpleDateFormat("yyyyMMdd",Locale.US).format(Date())}.json").apply {writeText(report(context).toString(2))}
    }
}

class GboApplication:Application() {
    override fun onCreate(){super.onCreate();StabilityDiagnostics.install(this)
        com.tuempresa.inventariovial.core.AutomaticSync.wake(this)
        com.tuempresa.inventariovial.media.VideoSync.enqueue(this)
        registerActivityLifecycleCallbacks(object:ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity:Activity){if(activity is com.tuempresa.inventariovial.core.FieldCameraActivity)StabilityDiagnostics.screen("CAMERA")}
            override fun onActivityCreated(activity:Activity,state:Bundle?){}
            override fun onActivityStarted(activity:Activity){}
            override fun onActivityPaused(activity:Activity){}
            override fun onActivityStopped(activity:Activity){}
            override fun onActivitySaveInstanceState(activity:Activity,state:Bundle){}
            override fun onActivityDestroyed(activity:Activity){}
        })
    }
    override fun onTrimMemory(level:Int) {
        super.onTrimMemory(level)
        StabilityDiagnostics.record(this,"TRIM_MEMORY",trimLevel=level)
        com.tuempresa.inventariovial.camera.FieldThumbnailCache.clear()
        com.tuempresa.inventariovial.field.SignalAssetCatalog.clearThumbnails()
    }
    override fun onLowMemory(){super.onLowMemory();StabilityDiagnostics.record(this,"TRIM_MEMORY",trimLevel=android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE);com.tuempresa.inventariovial.camera.FieldThumbnailCache.clear();com.tuempresa.inventariovial.field.SignalAssetCatalog.clearThumbnails()}
}
