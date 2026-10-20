package com.tuempresa.inventariovial.model.form

/** Semantic aliases only. A wall and a ford never share an identity. No persisted data is repaired here. */
object Sic20Identity {
    fun classOf(asset:String):String?=when(asset.trim().uppercase(java.util.Locale.ROOT)) {
        "FORD","BADEN","BADÉN"->"12"
        "TUNNEL","TUNEL","TÚNEL"->"13"
        "WALL","MURO"->"14"
        else->null
    }
    fun contractualAsset(classCode:String):String=when(classCode) {
        "12"->"BADEN";"13"->"TUNEL";"14"->"MURO"
        else->error("Subtipo SIC-20 no reconocido. Conserva la ficha y solicita revisión.")
    }
    fun resolve(asset:String,vararg persistedClasses:String?):String {
        val classes=persistedClasses.filterNotNull().filter{it.isNotBlank()}+listOfNotNull(classOf(asset))
        require(classes.isNotEmpty() && classes.all{it in setOf("12","13","14")} && classes.distinct().size==1) {
            "Conflicto de subtipo SIC-20 entre la ficha y el formulario. Se conservan UUID, fotografías y borrador; solicita revisión."
        }
        return classes.first()
    }
    fun requireDetail(asset:String,classCode:String) {
        require(classOf(asset)!=null) {"Identidad SIC-20 desconocida; conserva el borrador y solicita revisión."}
        resolve(asset,classCode)
    }
}

/** Used at the same UUID guards as before; only SIC-20 synonyms are equivalent. */
internal fun sameInventoryIdentity(old:com.tuempresa.inventariovial.data.entity.InventoryRecordEntity,code:String,asset:String):Boolean =
    old.sicCode==code && if(code=="SIC-20") {
        val previous=Sic20Identity.classOf(old.assetType)
        previous!=null && previous==Sic20Identity.classOf(asset)
    } else old.assetType==asset
