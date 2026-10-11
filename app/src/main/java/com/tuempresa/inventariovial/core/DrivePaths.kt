package com.tuempresa.inventariovial.core

import android.content.Context
import com.tuempresa.inventariovial.PlannedAssetFolder
import com.tuempresa.inventariovial.pavement.PavementSampleEntity
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

/** Persist identity before network. Sequence never depends on a photo name or current sort. */
object DrivePaths {
    @Synchronized private fun element(context: Context, route: String, family: String, prefix: String, id: String): String {
        val store=context.getSharedPreferences("drive_element_identity_v2",Context.MODE_PRIVATE)
        val scope="${DriveAssetPathRouter.component(route)}/${DriveAssetPathRouter.component(family)}"
        val key="$scope/$id"
        val existing=store.getString(key,null)
        if(existing!=null) return existing
        val next=store.getInt("counter/$scope",0)+1
        val name="${prefix}_${next.toString().padStart(3,'0')}"
        check(store.edit().putInt("counter/$scope",next).putString(key,name).commit()) { "No se guardó la identidad local" }
        return name
    }
    fun category(stored: String?): PhotoCategory = when(stored) {
        "CULVERT_PANORAMIC" -> PhotoCategory.PANORAMICA
        "CULVERT_INLET" -> PhotoCategory.ENTRADA
        "CULVERT_OUTLET" -> PhotoCategory.SALIDA
        else -> PhotoCategory.fromStored(stored)
    }
    fun inventory(context: Context, folder: PlannedAssetFolder, photoCategory: String? = null): String {
        val a=folder.asset
        val family=DriveInventorySchema.familyPath(a.assetFamily)
        val bound=DriveInventorySchema.receipt(context,a.recordId)?.optString("logicalPath")
        val path=bound ?: "${DriveAssetPathRouter.component(a.routeCode)}/$family/${DriveInventorySchema.elementLabel(a.assetFamily)} UUID_${a.recordId}"
        return path+(photoCategory?.let { "/"+DriveInventorySchema.category(it) } ?: "")
    }
    fun bridge(context: Context,s: ScapInspectionSnapshot): String {
        val route=s.values()["route"]?.takeIf { it.isNotBlank() } ?: s.roadRecord?.routeCode.orEmpty()
        return DriveInventorySchema.receipt(context,s.inspection.roadRecordId)?.optString("logicalPath")
            ?: "$route/PUENTES/PUENTE UUID_${s.inspection.roadRecordId}"
    }
    fun pavement(context: Context,s: PavementSampleEntity,category: String?=null): String {
        val surface=when(s.surface) {"ASPHALT"->"ASFALTICO";"RIGID"->"RIGIDO";else->"NO_PAVIMENTADO"}
        return DriveAssetPathRouter.path(AssetPathRequest(s.ruta,"PAVIMENTOS",
            element(context,s.ruta,"PAVIMENTOS/$surface".replace('/','_'),"MUESTRA",s.id),category?.let(::PhotoCategory),pavementSurface=surface))
    }
}
