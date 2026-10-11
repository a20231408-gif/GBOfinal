package com.tuempresa.inventariovial.auth

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Non-secret UI hint only. Server checks the live permission on EVERY operation. */
class TabletAdminPermissions(context: Context) {
    private val prefs=context.getSharedPreferences("tablet_admin_permission_hints",Context.MODE_PRIVATE)
    private val state=MutableStateFlow(prefs.getStringSet("users",emptySet()).orEmpty().toSet())
    val users=state.asStateFlow()
    fun replace(response:JSONObject) {
        val a=response.optJSONArray("tabletAdminUserIds") ?: return // Compatible with the previous deployment.
        val ids=(0 until a.length()).map {a.getString(it)}.toSet()
        prefs.edit().putStringSet("users",ids).apply();state.value=ids
    }
}

class TabletAdminRepository(private val service:UserService,private val sessions:AuthSessionManager,
    private val now:()->Long=SystemClock::elapsedRealtime) {
    private var token:String?=null
    private var expiresAt=0L
    private var owner:String?=null
    val authorized get()=token!=null && now()<expiresAt && sessions.current.value?.let {it.id==owner && !it.locked && !it.closing}==true
    val remainingMillis get()=maxOf(0L,expiresAt-now())
    fun forget() {token=null;owner=null;expiresAt=0}
    suspend fun authenticate(pin:CharArray) {
        forget()
        val user=sessions.requireUser()
        val response=try {service.call("authenticateTabletAdmin",JSONObject().put("userId",user.id).put("pin",String(pin)))} finally {pin.fill('\u0000')}
        check(sessions.requireUser().id==user.id)
        require(response.getString("scope")=="TABLET_ADMIN_ONLY")
        val received=response.getString("tabletAdminToken").also {require(it.matches(Regex("[A-Za-z0-9_-]{43}")))}
        require(response.getLong("expiresInSeconds") in 600..900)
        token=received;owner=user.id;expiresAt=now()+response.getLong("expiresInSeconds")*1000
    }
    suspend fun call(action:String,fields:JSONObject=JSONObject()):JSONObject {
        require(action in setOf("listTablets","provisionTabletAdmin","revokeTabletAdmin","reactivateTabletAdmin","rotateTabletToken","getTabletAudit"))
        if(!authorized){forget();throw UserServiceException("SESSION_EXPIRED")}
        try {return service.call(action,JSONObject(fields.toString()).put("tabletAdminToken",token))}
        catch(e:UserServiceException) {if(e.code in setOf("SESSION_EXPIRED","TABLET_ADMIN_REQUIRED","DEVICE_REVOKED"))forget();throw e}
    }
}
