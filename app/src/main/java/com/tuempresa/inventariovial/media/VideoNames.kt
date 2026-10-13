package com.tuempresa.inventariovial.media

import android.content.Context
import org.json.JSONObject

/** Optional labels from the server; never rename originals or change Room identity. */
internal object VideoNames {
    private const val PREFS="video_readable_names_v2"
    fun validate(ack:JSONObject,video:VideoEntity,requested:Int=0) {
        val version=ack.optInt("videoNamingVersion")
        if(version!=2) {
            require(requested!=2 || version==1 && ack.optBoolean("historicalName"))
            return
        }
        val suffix=when(video.mimeType){"video/mp4"->"mp4";"video/webm"->"webm";"video/3gpp"->"3gp";else->error("Formato no admitido")}
        val number=ack.getInt("videoNumber");val element=ack.getInt("elementNumber");val code=ack.getString("categoryCode")
        require(number in 1..999 && element>0 && code.matches(Regex("[A-Z0-9]{1,4}")))
        val name="${number.toString().padStart(3,'0')}_PUE${element}_$code.$suffix"
        require(name.length<=20 && ack.getString("fileName")==name)
        require(ack.getString("categoryFolder").matches(Regex("[A-Z0-9_]{1,80}")))
    }
    fun accept(context:Context,video:VideoEntity,ack:JSONObject) {
        validate(ack,video)
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val previous=prefs.getString(video.id,null)?.let(::JSONObject)
        if(ack.optInt("videoNamingVersion")!=2) {require(previous==null);return}
        val label=JSONObject().put("parentKey",video.parentKey).put("sha256",video.sha256)
            .put("fileId",ack.getString("fileId")).put("fileName",ack.getString("fileName"))
            .put("categoryFolder",ack.getString("categoryFolder"))
        if(previous!=null)require(label.keys().asSequence().all {previous.optString(it)==label.getString(it)})
        check(prefs.edit().putString(video.id,label.toString()).commit())
    }
    fun display(context:Context,id:String):String? = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        .getString(id,null)?.let {runCatching {JSONObject(it).getString("fileName")}.getOrNull()}
}
