package com.tuempresa.inventariovial

import org.json.JSONArray
import org.json.JSONObject

/** Fictitious V2 reservation for offline transport tests; no release flag or production code is changed. */
internal object ReleasePhotoV2Fixture {
    fun reservation(p:JSONObject,folder:String,number:Int=1):JSONObject {
        val prefix=if(p.getString("kind")=="PAVEMENT")
            "${p.getString("routeCode")}/EVALUACION_SUPERFICIAL/${p.getString("tramo")}/${p.getString("surface")}/MUESTRA "
        else "${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} "
        val files=p.getJSONArray("files")
        val photos=(0 until files.length()).map{files.getJSONObject(it)}.filter{it.getString("key").startsWith("photo/")}
        return JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",p.getString("key"))
            .put("revision",p.getLong("revision")).put("identityId",p.optString("identityId",p.getString("recordId")))
            .put("folderId",folder).put("logicalPath",prefix+number)
            .put("photos",JSONArray(photos.mapIndexed{n,f->JSONObject()
                .put("id",f.getString("key").substringAfter("photo/")).put("category",f.optString("photoCategory","GENERAL"))
                .put("name","${(n+1).toString().padStart(3,'0')}_TST${number}_GEN.jpg").put("historical",false)}))
    }
}
