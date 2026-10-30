package com.tuempresa.inventariovial.media

import android.content.Context
import org.json.JSONObject

/** Optional labels from the server; never rename originals or change Room identity. */
internal object VideoNames {
    private const val PREFS="video_readable_names_v2"
    fun validate(ack:JSONObject,video:VideoEntity,requested:Int=0) {
        val version=ack.optInt("videoNamingVersion")
        if(com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel.forCode(video.category)!=null)require(version==2) {"El panel oficial requiere un nombre confirmado compatible."}
        if(version!=2) {
            require(requested!=2 || version==1 && ack.optBoolean("historicalName"))
            return
        }
        val suffix=when(video.mimeType){"video/mp4"->"mp4";"video/webm"->"webm";"video/3gpp"->"3gp";else->error("Formato no admitido")}
        val number=ack.getInt("videoNumber");val element=ack.getInt("elementNumber");val code=ack.getString("categoryCode")
        val panel=com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel.forCode(video.category)
        if(panel!=null) {
            require(number in 1..999 && element>0 && code==panel.key)
            require(ack.getString("fileName")==panel.fileName(number,suffix,element) && ack.getString("fileName").length<=80)
            require(ack.getString("categoryFolder")==panel.folder)
            return
        }
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
    /** Read only: a parent photo ACK is never sufficient to accredit a video's public name. */
    fun confirmedRelativePath(context:Context,video:VideoEntity):String? {
        if(video.state!="SYNCED" || video.retired || video.fileId.isNullOrBlank() || video.uploadedBytes!=video.sizeBytes)return null
        val label=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(video.id,null)?.let{runCatching{JSONObject(it)}.getOrNull()} ?: return null
        if(label.optString("parentKey")!=video.parentKey || label.optString("sha256")!=video.sha256 || label.optString("fileId")!=video.fileId)return null
        val folder=label.optString("categoryFolder");val name=label.optString("fileName")
        val panel=com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel.forCode(video.category)
        if(panel!=null)return if(folder==panel.folder && name.length<=80 && name.matches(Regex("${panel.number}_PUE[1-9][0-9]*_${panel.abbreviation}_[0-9]{3}\\.(mp4|webm|3gp)"))) "VIDEOS/$folder/$name" else null
        if(!folder.matches(Regex("[A-Z0-9_]{1,80}")) || !name.matches(Regex("[0-9]{3}_PUE[1-9][0-9]*_[A-Z0-9]{1,4}\\.(mp4|webm|3gp)")))return null
        return "VIDEOS/$folder/$name"
    }
}
