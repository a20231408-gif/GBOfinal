package com.tuempresa.inventariovial.core

import android.content.Context
import com.tuempresa.inventariovial.road.AssetFamily
import org.json.JSONObject

/** V4 names derive from coded catalogs, never material/condition/user text. */
object DriveInventorySchema {
    const val PATH_VERSION = 4
    const val EXCEL_FOLDER = "EXCEL_SIC"
    fun familyPath(family: AssetFamily): String = when(family) {
        AssetFamily.SENALIZACION_HORIZONTAL -> "SEÑALIZACION_Y_SEGURIDAD_VIAL/MARCAS"
        AssetFamily.TACHAS -> "SEÑALIZACION_Y_SEGURIDAD_VIAL/TACHAS"
        AssetFamily.SEGURIDAD_VIAL -> "SEÑALIZACION_Y_SEGURIDAD_VIAL/SEGURIDAD_VIAL"
        AssetFamily.CALZADA_FAJAS -> "FAJAS_CALZADA"
        AssetFamily.ANCHOS_FAJA -> "ANCHOS_FAJA"
        AssetFamily.SENALIZACION_VERTICAL -> "SENALIZACION_VERTICAL"
        else -> family.mapFolder
    }.split('/').joinToString("/") { DriveAssetPathRouter.component(it) }
    fun elementLabel(family: AssetFamily) = when(family) {
        AssetFamily.ANCHOS_FAJA -> "ANCHO FAJA"
        AssetFamily.SENALIZACION_HORIZONTAL -> "MARCA"
        AssetFamily.TACHAS -> "TACHA"
        AssetFamily.SEGURIDAD_VIAL -> "DISPOSITIVO"
        AssetFamily.SENALIZACION_VERTICAL -> "SENAL"
        else -> family.folderLabel
    }
    private val categories = mapOf(
        "CULVERT_PANORAMIC" to "PANORAMICA", "CULVERT_INLET" to "ENTRADA", "CULVERT_OUTLET" to "SALIDA",
        "RIGHT_SIDE" to "VISTA_LADO_DERECHO", "LEFT_SIDE" to "VISTA_LADO_IZQUIERDO",
        "UPSTREAM" to "AGUAS_ARRIBA", "DOWNSTREAM" to "AGUAS_ABAJO", "ROAD_DECK" to "CARRETERA_TABLERO",
        "INFORMATIVE_SIGN" to "SENAL_INFORMATIVA", "RAILINGS_SIDEWALKS_PIPES" to "BARANDAS_VEREDAS_TUBERIAS",
        "DEFECT" to "ANOMALIAS_FALLAS", "BRIDGE_ELEMENT" to "OTROS_ELEMENTOS_PUENTE",
        "TRANSVERSE" to "VISTA_TRANSVERSAL", "ELEMENT" to "ELEMENTO", "ACCESS" to "ACCESO",
        "CHANNEL" to "CAUCE", "OTHER" to "OTRA")
    fun category(code: String?) = categories[code] ?: DriveAssetPathRouter.component(code?.takeIf { it.isNotBlank() } ?: "GENERAL")
    fun bind(context:Context,asset:com.tuempresa.inventariovial.road.OrderedAsset) = asset.copy(remoteLogicalPath=receipt(context,asset.recordId)?.optString("logicalPath"))
    fun receipt(context: Context, identity: String): JSONObject? = context.getSharedPreferences("drive_bindings_v4",Context.MODE_PRIVATE)
        .getString(identity,null)?.let(::JSONObject)
    fun remember(context: Context,payload:JSONObject,ack:JSONObject) {
        if(payload.optInt("pathVersion") != PATH_VERSION || payload.optString("kind")=="PAVEMENT") return
        check(context.getSharedPreferences("drive_bindings_v4",Context.MODE_PRIVATE).edit()
            .putString(payload.optString("identityId",payload.getString("recordId")), JSONObject()
                .put("folderId",ack.getString("folderId")).put("logicalPath",ack.getString("logicalPath")) .toString()).commit())
    }
}
