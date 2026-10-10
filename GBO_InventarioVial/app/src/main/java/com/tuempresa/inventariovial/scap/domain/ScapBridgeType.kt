package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

/** Stored in scap_values, so pre-existing v12 drafts retain every value. */
enum class ScapBridgeType(val label:String,val assetName:String,val templateAsset:String) {
    MODULAR("MODULAR","scap_modular_catalog.json","scap_modular_template.xlsx"),
    SLAB("LOSA","scap_slab_catalog.json","scap_slab_template.xlsx");
    companion object {
        const val FIELD="bridgeType"
        fun from(snapshot:ScapInspectionSnapshot):ScapBridgeType? = entries.firstOrNull {it.name==snapshot.values()[FIELD]}
    }
}

/** Independent capture models, with shared visual field components. */
sealed interface ScapCaptureModel {
    val type:ScapBridgeType
    val sections:List<String>
    val category:String
    val structuralType:String
}
data object ModularScapCapture:ScapCaptureModel {
    override val type=ScapBridgeType.MODULAR
    override val sections=listOf("A","B","C1","C2","C3","C4","C5","C6","C7","C8","C9","C10","D1","D2","D3","D4","E","F1","F2","G1","G2","SIC","REVIEW")
    override val category="PROVISIONALES"
    override val structuralType="Modular"
}
data object SlabScapCapture:ScapCaptureModel {
    override val type=ScapBridgeType.SLAB
    override val sections=listOf("A","B","C1","C2","C3","C4","C5","C6","C7","C8","C9","C10","D1","D2","D3","D4","E","F1","F2","G1","G2","SIC","REVIEW")
    override val category="DEFINITIVO"
    override val structuralType="Losa"
}
object ScapCaptureModels {
    fun forType(type:ScapBridgeType):ScapCaptureModel = when(type) {
        ScapBridgeType.MODULAR->ModularScapCapture
        ScapBridgeType.SLAB->SlabScapCapture
    }
}
