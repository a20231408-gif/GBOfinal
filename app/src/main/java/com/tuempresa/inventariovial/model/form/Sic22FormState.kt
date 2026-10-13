package com.tuempresa.inventariovial.model.form

data class Sic22FormState(

    // =====================================================
    // SIC-22 · SEÑALIZACIÓN VERTICAL
    // =====================================================

    // La clase de señalización vertical
    // es siempre 20.
    val classCode: String = "20",


    // 1 - Reglamento
    // 2 - Preventivo
    // 3 - Informativo
    // 4 - Poste Kilométrico
    // 5 - Semáforos
    // 6 - Postes SOS
    val typeCode: String = "2",


    // 1 - Fibra de vidrio
    // 2 - Acero
    // 3 - Concreto
    // 4 - Madera
    // 5 - Otro
    val materialCode: String = "2",


    // Aplica a:
    // 1 Reglamento
    // 2 Preventivo
    // 3 Informativo
    val signalCode: String = "",


    // Aplica a tipo 4:
    // Poste Kilométrico.
    val kilometerPostNumber: String = "",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val conditionCode: String = "1",


    // =====================================================
    // CAMPOS ADICIONALES SOLICITADOS POR EL CLIENTE
    //
    // NO SON CAMPOS OFICIALES DEL SIC-22.
    // =====================================================

    val signWidthM: String = "",

    val signHeightM: String = "",

    val lowerEdgeHeightM: String = "",
    val signalCodeSource: String = SignalCodeSource.CATALOG,
    val signalDescription: String = "",
    val signalObservation: String = ""
) : java.io.Serializable {

    val usesSignalCode: Boolean
        get() =
            typeCode == "1" ||
                    typeCode == "2" ||
                    typeCode == "3"


    val usesKilometerPostNumber: Boolean
        get() = isKilometerMarker

    val isKilometerMarker: Boolean
        get() = SignalIdentificationPolicy.isKilometerMarker(typeCode, signalCode, signalCodeSource)

    fun selectInformativeElement(kilometerMarker: Boolean): Sic22FormState =
        if (kilometerMarker) copy(typeCode="3",signalCode="I-2A",signalCodeSource=SignalCodeSource.CATALOG,signalDescription="")
        else copy(typeCode="3",signalCode=if (isKilometerMarker) "" else signalCode)

    /** A conceptual unidentified plate never occupies a contractual code column. */
    val persistedSignalCode: String?
        get() = if (signalCodeSource == SignalCodeSource.UNIDENTIFIABLE) null else signalCode.trim().uppercase(java.util.Locale.ROOT).ifBlank { null }

    fun selectSignalSource(source: String): Sic22FormState = if(source == signalCodeSource) this
        else copy(signalCodeSource = source, signalCode = "", signalDescription = "")

    fun selectCatalogSignal(code: String): Sic22FormState = copy(signalCodeSource = SignalCodeSource.CATALOG,
        signalCode = code.trim().uppercase(java.util.Locale.ROOT), signalDescription = "")
}

object SignalCodeSource {
    const val CATALOG = "CATALOG"
    const val MANUAL = "MANUAL"
    const val UNIDENTIFIABLE = "UNIDENTIFIABLE"
}

object SignalIdentificationPolicy {
    fun isKilometerMarker(typeCode: String, code: String?, source: String = SignalCodeSource.CATALOG): Boolean =
        typeCode == "4" || (typeCode == "3" && source == SignalCodeSource.CATALOG && code?.trim()?.uppercase(java.util.Locale.ROOT) == "I-2A")

    /** Completeness is separate from saving: absent catalogue codes are pending at final review. */
    fun completionPending(state: Sic22FormState): List<String> = buildList {
        if (state.usesSignalCode && state.signalCodeSource != SignalCodeSource.UNIDENTIFIABLE && state.signalCode.isBlank())
            add("Código de señal")
        if (state.signalCodeSource == SignalCodeSource.MANUAL && state.signalDescription.isBlank()) add("Descripción de señal manual")
        if (state.usesSignalCode && state.signalCodeSource == SignalCodeSource.CATALOG && state.signalCode.isNotBlank() &&
            !com.tuempresa.inventariovial.TrafficSignCatalog.contains(state.typeCode,state.signalCode)) add("Código ajeno al catálogo del tipo de señal")
        if (state.signalCodeSource == SignalCodeSource.MANUAL && com.tuempresa.inventariovial.TrafficSignCatalog.containsCode(state.signalCode))
            add("Código manual que corresponde al catálogo")
        if (state.signalCodeSource == SignalCodeSource.UNIDENTIFIABLE && state.signalCode.isNotBlank()) add("Señal sin código identificable con código guardado")
        if (state.signalCodeSource !in setOf(SignalCodeSource.CATALOG,SignalCodeSource.MANUAL,SignalCodeSource.UNIDENTIFIABLE)) add("Identificación de señal")
        if (state.isKilometerMarker && state.kilometerPostNumber.isBlank()) add("Número del poste kilométrico")
    }

    fun completionPending(entity: com.tuempresa.inventariovial.data.entity.Sic22Entity): List<String> = completionPending(
        Sic22FormState(classCode=entity.classCode,typeCode=entity.typeCode,materialCode=entity.materialCode,
            signalCode=entity.signalCode.orEmpty(),kilometerPostNumber=entity.kilometerPostNumber.orEmpty(),conditionCode=entity.conditionCode,
            signalCodeSource=entity.signalCodeSource,signalDescription=entity.signalDescription,signalObservation=entity.signalObservation))
}
