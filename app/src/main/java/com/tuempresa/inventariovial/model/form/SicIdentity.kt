package com.tuempresa.inventariovial.model.form

/** Fail before touching photos or a raw draft if a recovered form has the wrong identity. */
internal fun requireSicDetail(code:String,detail:SicFormDetail) {
    val expected=when(detail) {
        is SicFormDetail.Sic17->"SIC-17";is SicFormDetail.Sic18->"SIC-18"
        is SicFormDetail.Sic19->"SIC-19";is SicFormDetail.Sic20->"SIC-20"
        is SicFormDetail.Sic21->"SIC-21";is SicFormDetail.Sic22->"SIC-22"
        is SicFormDetail.Sic23->"SIC-23"
    }
    require(code==expected) {"El formulario no corresponde al tipo SIC de la ficha. Conserva tus cambios y vuelve a abrirla."}
}

internal fun requireSicSnapshot(snapshot:com.tuempresa.inventariovial.data.entity.InventorySnapshot) {
    val codes=listOfNotNull(snapshot.sic17?.let{"SIC-17"},snapshot.sic18?.let{"SIC-18"},snapshot.sic19?.let{"SIC-19"},
        snapshot.sic20?.let{"SIC-20"},snapshot.sic21?.let{"SIC-21"},snapshot.sic22?.let{"SIC-22"},snapshot.sic23?.let{"SIC-23"})
    require(codes.all{it==snapshot.record.sicCode}) {
        "La ficha contiene detalles de otro tipo SIC. Conserva el registro y sus fotografías; requiere revisión antes de editar."
    }
    if(snapshot.record.sicCode=="SIC-20")snapshot.sic20?.let{Sic20Identity.resolve(snapshot.record.assetType,it.classCode)}
}

internal fun requireSicRawDetail(code:String,type:String) {
    val number=code.removePrefix("SIC-")
    require(number in setOf("17","18","19","20","21","22","23") && type=="Sic${number}FormState") {
        "El tipo del borrador guardado no coincide con la ficha. Conserva la evidencia y solicita revisión."
    }
}
