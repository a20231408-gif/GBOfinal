package com.tuempresa.inventariovial

/**
 * Catálogo de códigos de señal vertical tomado del Manual de Dispositivos de
 * Control del Tránsito Automotor para Calles y Carreteras (MTC, 2016).
 *
 * SIC-22 usa typeCode: 1 = reglamentaria, 2 = preventiva, 3 = informativa.
 * Las denominaciones se mantienen deliberadamente cortas para uso en campo;
 * el código persistido sigue siendo únicamente R-xx / P-xx / I-xx.
 */
object TrafficSignCatalog {
    data class Sign(val code: String, val name: String, val description: String = "") {
        val option: String get() = "$code - $name"
    }

    private val regulatory = listOf(
        Sign("R-1", "PARE"),
        Sign("R-2", "CEDA EL PASO"),
        Sign("R-3", "DIRECCIÓN OBLIGADA"),
        Sign("R-4", "NO ENTRE"),
        Sign("R-5", "GIRO SOLAMENTE A LA IZQUIERDA"),
        Sign("R-5-1", "CARRIL EXCLUSIVO PARA VOLTEO OBLIGADO A LA IZQUIERDA"),
        Sign("R-5-2", "CARRIL PERMITIDO PARA VOLTEO Y SEGUIR DE FRENTE"),
        Sign("R-5-3", "CARRIL EXCLUSIVO PARA VOLTEO OBLIGADO Y CARRIL DE VOLTEO CON SEGUIR DE FRENTE"),
        Sign("R-5-4", "VOLTEO A LA IZQUIERDA EN AMBOS SENTIDOS"),
        Sign("R-6", "PROHIBIDO VOLTEAR A LA IZQUIERDA"),
        Sign("R-6A", "PROHIBIDO GIRAR A LA IZQUIERDA CON LUZ ROJA"),
        Sign("R-7", "GIRO SOLAMENTE A LA DERECHA"),
        Sign("R-8", "PROHIBIDO VOLTEAR A LA DERECHA"),
        Sign("R-8A", "PROHIBIDO GIRAR A LA DERECHA CON LUZ ROJA"),
        Sign("R-9", "GIRO SOLAMENTE EN U"),
        Sign("R-10", "PROHIBIDO VOLTEAR EN U"),
        Sign("R-11", "CIRCULACIÓN EN AMBOS SENTIDOS"),
        Sign("R-11A", "TRES CARRILES, UNO EN CONTRAFLUJO"),
        Sign("R-11B", "TRES CARRILES, DOS EN CONTRAFLUJO"),
        Sign("R-12", "PROHIBIDO CAMBIAR DE CARRIL"),
        Sign("R-14", "CIRCULACIÓN OBLIGATORIA"),
        Sign("R-14A", "TRÁNSITO EN UN SENTIDO"),
        Sign("R-14B", "TRÁNSITO EN AMBOS SENTIDOS"),
        Sign("R-16", "PROHIBIDO ADELANTAR"),
        Sign("R-16A", "FIN DE ZONA DE PROHIBIDO ADELANTAR"),
        Sign("R-17", "PROHIBIDO CIRCULACIÓN DE VEHÍCULOS AUTOMOTORES"),
        Sign("R-18", "VEHÍCULOS PESADOS A LA DERECHA"),
        Sign("R-19", "PROHIBIDO CIRCULACIÓN DE VEHÍCULOS DE CARGA"),
        Sign("R-20", "PEATONES DEBEN CIRCULAR POR LA IZQUIERDA"),
        Sign("R-21", "PROHIBIDO PASO/CIRCULACIÓN DE PEATONES"),
        Sign("R-22", "PROHIBIDO CIRCULACIÓN DE BICICLETAS Y MOTOCICLOS"),
        Sign("R-22A", "PROHIBIDO CIRCULACIÓN DE TRICICLOS"),
        Sign("R-23", "PROHIBIDO CIRCULACIÓN DE MOTOCICLETAS"),
        Sign("R-24", "PROHIBIDO CIRCULACIÓN DE MAQUINARIA AGRÍCOLA"),
        Sign("R-25", "PROHIBIDO CIRCULACIÓN DE VEHÍCULOS DE TRACCIÓN ANIMAL"),
        Sign("R-25A", "PROHIBIDO CIRCULACIÓN DE CARRETILLAS MANUALES"),
        Sign("R-25B", "PROHIBIDO CIRCULACIÓN ECUESTRE"),
        Sign("R-25C", "PROHIBIDO CIRCULACIÓN DE BUSES"),
        Sign("R-25D", "PROHIBIDO CIRCULACIÓN DE CUATRIMOTOS"),
        Sign("R-26", "PERMITIDO ESTACIONAR"),
        Sign("R-27", "PROHIBIDO ESTACIONAR"),
        Sign("R-27A", "PROHIBIDO ESTACIONAR - ZONA DE REMOLQUE"),
        Sign("R-28", "PROHIBIDO DETENERSE"),
        Sign("R-29", "PROHIBIDO USO DE LA BOCINA"),
        Sign("R-30", "VELOCIDAD MÁXIMA PERMITIDA"),
        Sign("R-30B", "VELOCIDAD MÍNIMA PERMITIDA"),
        Sign("R-30C", "VELOCIDAD MÁXIMA PERMITIDA DE SALIDA"),
        Sign("R-30D", "VELOCIDAD MÁXIMA PERMITIDA PARA CAMIÓN"),
        Sign("R-30E", "VELOCIDAD MÁXIMA PERMITIDA PARA BUS"),
        Sign("R-30F", "VELOCIDAD MÁXIMA PERMITIDA EN CURVA"),
        Sign("R-30G", "VELOCIDAD MÁXIMA SEGÚN TIPO DE VEHÍCULO"),
        Sign("R-31", "PESO MÁXIMO PERMITIDO POR EJE"),
        Sign("R-32", "PESO MÁXIMO BRUTO PERMITIDO POR VEHÍCULO"),
        Sign("R-33", "LARGO MÁXIMO PERMITIDO"),
        Sign("R-34", "CIRCULACIÓN SOLO DE BUSES"),
        Sign("R-35", "ALTURA MÁXIMA PERMITIDA"),
        Sign("R-36", "ANCHO MÁXIMO PERMITIDO"),
        Sign("R-37", "CONTROL"),
        Sign("R-40", "CIRCULACIÓN CON LUCES BAJAS"),
        Sign("R-42", "CICLOVÍA"),
        Sign("R-42A", "CICLOVÍA - CONSERVE LA DERECHA"),
        Sign("R-42B", "CICLOVÍA - OBLIGATORIO DESCENDER DE LA BICICLETA"),
        Sign("R-42C", "CIRCULACIÓN NO COMPARTIDA BICICLETA-PEATÓN"),
        Sign("R-43", "USO OBLIGATORIO DE CADENAS"),
        Sign("R-44", "PARADERO PROHIBIDO"),
        Sign("R-45", "PROHIBIDO CIRCULACIÓN DE MOTOTAXIS"),
        Sign("R-45A", "PROHIBIDO CIRCULACIÓN DE MOTOCARGA"),
        Sign("R-47", "PARADERO"),
        Sign("R-48", "ZONA DE CARGA Y DESCARGA"),
        Sign("R-49", "MANTENER DISTANCIA DE SEGURIDAD"),
        Sign("R-50", "PREFERENCIA AL SENTIDO CONTRARIO"),
        Sign("R-52", "PROHIBIDO CARGA Y DESCARGA"),
        Sign("R-53", "NO BLOQUEAR CRUCE"),
        Sign("R-54", "SOLO MOTOCICLETAS"),
        Sign("R-54A", "SOLO CIRCULACIÓN DE MOTOTAXIS"),
        Sign("R-54B", "SOLO CIRCULACIÓN DE MOTOCARGA"),
        Sign("R-55A", "VÍA SEGREGADA PARA BUSES"),
        Sign("R-55B", "VÍA SEGREGADA PARA BUSES"),
        Sign("R-56", "SOLO TRANSPORTE PÚBLICO"),
        Sign("R-58A", "VÍA SEGREGADA MOTORIZADOS-BICICLETAS"),
        Sign("R-58B", "VÍA SEGREGADA MOTORIZADOS-BICICLETAS"),
        Sign("R-62", "ESTACIONAMIENTO SOLO TAXIS"),
        Sign("R-64A", "PERMITIDO GIRAR CON LUZ ROJA - IZQUIERDA"),
        Sign("R-64B", "PERMITIDO GIRAR CON LUZ ROJA - DERECHA")
    )

    private val preventive = listOf(
        Sign("P-1A", "CURVA PRONUNCIADA A LA DERECHA"),
        Sign("P-1B", "CURVA PRONUNCIADA A LA IZQUIERDA"),
        Sign("P-2A", "CURVA A LA DERECHA"),
        Sign("P-2B", "CURVA A LA IZQUIERDA"),
        Sign("P-3A", "CURVA Y CONTRA-CURVA PRONUNCIADA A LA DERECHA"),
        Sign("P-3B", "CURVA Y CONTRA-CURVA PRONUNCIADA A LA IZQUIERDA"),
        Sign("P-4A", "CURVA Y CONTRA-CURVA A LA DERECHA"),
        Sign("P-4B", "CURVA Y CONTRA-CURVA A LA IZQUIERDA"),
        Sign("P-5-1", "CAMINO SINUOSO A LA DERECHA"),
        Sign("P-5-1A", "CAMINO SINUOSO A LA IZQUIERDA"),
        Sign("P-5-2A", "CURVA EN U A LA DERECHA"),
        Sign("P-5-2B", "CURVA EN U A LA IZQUIERDA"),
        Sign("P-6", "CRUCE DE VÍAS A NIVEL"),
        Sign("P-6A", "INTERSECCIÓN ESCALONADA PRIMERA DERECHA"),
        Sign("P-6B", "INTERSECCIÓN ESCALONADA PRIMERA IZQUIERDA"),
        Sign("P-7", "INTERSECCIÓN EN T"),
        Sign("P-8", "BIFURCACIÓN EN Y"),
        Sign("P-9A", "EMPALME EN ÁNGULO RECTO CON VÍA LATERAL A LA DERECHA"),
        Sign("P-9B", "EMPALME EN ÁNGULO RECTO CON VÍA LATERAL A LA IZQUIERDA"),
        Sign("P-10A", "EMPALME EN ÁNGULO AGUDO A LA DERECHA"),
        Sign("P-10B", "EMPALME EN ÁNGULO AGUDO A LA IZQUIERDA"),
        Sign("P-15", "INTERSECCIÓN ROTATORIA"),
        Sign("P-16A", "INCORPORACIÓN DE TRÁNSITO A LA DERECHA"),
        Sign("P-16B", "INCORPORACIÓN DE TRÁNSITO A LA IZQUIERDA"),
        Sign("P-17A", "REDUCCIÓN DE CALZADA A AMBOS LADOS"),
        Sign("P-17B", "REDUCCIÓN DE CALZADA LADO DERECHO"),
        Sign("P-17C", "REDUCCIÓN DE CALZADA LADO IZQUIERDO"),
        Sign("P-18A", "REDUCCIÓN DEL CARRIL EXTERNO AL LADO DERECHO"),
        Sign("P-18B", "REDUCCIÓN DEL CARRIL EXTERNO AL LADO IZQUIERDO"),
        Sign("P-21", "ENSANCHAMIENTO DE CALZADA EN AMBOS LADOS"),
        Sign("P-21A", "ENSANCHAMIENTO DE CALZADA A LA DERECHA"),
        Sign("P-21B", "ENSANCHAMIENTO DE CALZADA A LA IZQUIERDA"),
        Sign("P-22C", "CARRIL ADICIONAL"),
        Sign("P-25", "DOS SENTIDOS DE TRÁNSITO"),
        Sign("P-25A", "TRES CARRILES - DOS EN CONTRAFLUJO"),
        Sign("P-25B", "TRES CARRILES - UNO EN CONTRAFLUJO"),
        Sign("P-28", "INICIO DE VÍA DE DOBLE SENTIDO CON SEPARADOR CENTRAL"),
        Sign("P-28A", "INICIO DE VÍA DE UN SENTIDO CON SEPARADOR CENTRAL"),
        Sign("P-29", "FINAL DE VÍA DE DOBLE SENTIDO CON SEPARADOR CENTRAL"),
        Sign("P-29A", "FINAL DE VÍA DE UN SENTIDO CON SEPARADOR CENTRAL"),
        Sign("P-31", "FINAL DE VÍA PAVIMENTADA"),
        Sign("P-31A", "FINAL DE VÍA"),
        Sign("P-33A", "PROXIMIDAD REDUCTOR DE VELOCIDAD TIPO RESALTO"),
        Sign("P-33B", "UBICACIÓN DE REDUCTOR DE VELOCIDAD TIPO RESALTO"),
        Sign("P-34", "PROXIMIDAD DE BADÉN"),
        Sign("P-34A", "UBICACIÓN DE BADÉN"),
        Sign("P-35", "FUERTE PENDIENTE EN DESCENSO"),
        Sign("P-35C", "FUERTE PENDIENTE EN ASCENSO"),
        Sign("P-36", "SUPERFICIE DESLIZANTE"),
        Sign("P-38", "ALTURA MÁXIMA PERMITIDA"),
        Sign("P-39", "ANCHO MÁXIMO PERMITIDO"),
        Sign("P-41", "TÚNEL"),
        Sign("P-42", "CRUCE FERROVIARIO A NIVEL SIN BARRERAS"),
        Sign("P-43", "CRUCE FERROVIARIO A NIVEL CON BARRERAS"),
        Sign("P-44", "CRUCE FERROVIARIO A NIVEL - CRUZ DE SAN ANDRÉS"),
        Sign("P-44A", "CRUCE FERROVIARIO A NIVEL - CRUCE OBLICUO"),
        Sign("P-44B", "CRUCE FERROVIARIO A NIVEL - NO TOCAR PITO"),
        Sign("P-45", "VUELO DE AVIONES A BAJA ALTURA"),
        Sign("P-46", "CICLISTAS EN LA VÍA"),
        Sign("P-46A", "CRUCE DE CICLOVÍA"),
        Sign("P-46B", "UBICACIÓN CRUCE DE CICLISTAS"),
        Sign("P-46C", "VEHÍCULOS EN LA CICLOVÍA"),
        Sign("P-46D", "CICLOVÍA - TRAMO EN DESCENSO"),
        Sign("P-46E", "CICLOVÍA - TRAMO EN ASCENSO"),
        Sign("P-48", "ZONA DE PRESENCIA DE PEATONES"),
        Sign("P-48A", "PROXIMIDAD DE CRUCE PEATONAL"),
        Sign("P-48B", "CRUCE PEATONAL"),
        Sign("P-49", "ZONA ESCOLAR"),
        Sign("P-49A", "PROXIMIDAD A CRUCE ESCOLAR"),
        Sign("P-49B", "UBICACIÓN DE CRUCE ESCOLAR"),
        Sign("P-50", "NIÑOS JUGANDO"),
        Sign("P-51", "MAQUINARIA AGRÍCOLA EN LA VÍA"),
        Sign("P-52", "SALIDA DE VEHÍCULOS DE BOMBEROS"),
        Sign("P-53", "ANIMALES EN LA VÍA"),
        Sign("P-55", "PROXIMIDAD DE SEMÁFORO"),
        Sign("P-56", "ZONA URBANA"),
        Sign("P-58", "PROXIMIDAD DE PARE"),
        Sign("P-59", "PROXIMIDAD DE CEDA EL PASO"),
        Sign("P-60", "PROHIBIDO ADELANTAR"),
        Sign("P-61", "DELINEADOR DE CURVA HORIZONTAL - CHEVRON"),
        Sign("P-62", "PESO BRUTO MÁXIMO PERMITIDO"),
        Sign("P-66", "RÁFAGAS DE VIENTO LATERAL"),
        Sign("P-66A", "ZONA DE ARENAMIENTO EN LA VÍA")
    )

    private val informative = listOf(
        Sign("I-1A", "ESCUDO INDICADOR DE CARRETERA DEL SISTEMA VIAL INTERAMERICANO"),
        Sign("I-1B", "RED VIAL NACIONAL DEL SINAC"),
        Sign("I-1C", "RED VIAL DEPARTAMENTAL O REGIONAL DEL SINAC"),
        Sign("I-1D", "RED VIAL VECINAL O RURAL DEL SINAC"),
        Sign("I-2A", "POSTES DE KILOMETRAJE"),
        Sign("I-2B", "BALIZA DE ACERCAMIENTO"),
        Sign("I-3A", "CONTROL DE VELOCIDAD EN CARRETERA"),
        Sign("I-3B", "CONTROL DE VELOCIDAD EN TRAMO DE CARRETERA"),
        Sign("I-4A", "CONTROL DE VELOCIDAD EN AUTOPISTA"),
        Sign("I-4B", "CONTROL DE VELOCIDAD EN TRAMO DE AUTOPISTA"),
        Sign("I-5", "SITIO DE PARQUEO"),
        Sign("I-6", "PARADERO DE BUSES"),
        Sign("I-7", "ESTACIONAMIENTO DE TAXIS"),
        Sign("I-8", "VÍA PARA CICLISTAS"),
        Sign("I-9", "ZONA MILITAR"),
        Sign("I-10", "IGLESIA"),
        Sign("I-11", "AEROPUERTO"),
        Sign("I-12", "HOSPEDAJE"),
        Sign("I-13", "PRIMEROS AUXILIOS"),
        Sign("I-14", "HOSPITAL"),
        Sign("I-15", "SERVICIOS SANITARIOS"),
        Sign("I-16", "RESTAURANTE"),
        Sign("I-17", "TELÉFONO"),
        Sign("I-18", "SERVICIO MECÁNICO"),
        Sign("I-19", "GRIFO"),
        Sign("I-20", "LLANTERÍA"),
        Sign("I-21", "PERSONAS CON MOVILIDAD REDUCIDA"),
        Sign("I-22", "SERVICIO DE INFORMACIÓN"),
        Sign("I-23", "SERVICIOS HIGIÉNICOS"),
        Sign("I-24", "TRANSPORTE FERROVIARIO"),
        Sign("I-25", "TRANSPORTE MASIVO DE PASAJEROS"),
        Sign("I-26", "ZONA RECREATIVA"),
        Sign("I-27", "TSUNAMI - RUTA DE EVACUACIÓN"),
        Sign("I-28", "ZONA DE RIESGO POR TSUNAMI"),
        Sign("I-29", "PUNTO DE ENCUENTRO POR TSUNAMI"),
        Sign("I-31", "ESTACIONAMIENTO PARA EMERGENCIAS"),
        Sign("I-32", "EXTINTOR CONTRA INCENDIOS"),
        Sign("I-33", "HIDRANTE Y MANGUERA CONTRA INCENDIOS"),
        Sign("I-34", "SALIDA DE EMERGENCIA"),
        Sign("I-35", "RUTA DE EMERGENCIA")
    )

    fun signsForType(typeCode: String): List<Sign> = when (typeCode) {
        "1" -> regulatory
        "2" -> preventive
        "3" -> informative
        else -> emptyList()
    }

    fun optionsForType(typeCode: String): List<String> = signsForType(typeCode).map(Sign::option)

    /** Search the existing catalogue; accents and case do not affect field searches. */
    fun search(typeCode: String, query: String): List<Sign> {
        val terms = normalize(query).split(Regex("\\s+")).filter(String::isNotBlank)
        return signsForType(typeCode).filter { matches(it, terms) }
    }

    fun matches(sign: Sign, query: String): Boolean =
        matches(sign, normalize(query).split(Regex("\\s+")).filter(String::isNotBlank))

    private fun matches(sign: Sign, terms: List<String>): Boolean {
        val searchable = normalize("${sign.code} ${sign.name} ${sign.description}")
        return terms.all(searchable::contains)
    }

    private fun normalize(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").uppercase(java.util.Locale.ROOT).trim()

    fun contains(typeCode: String, code: String): Boolean = signsForType(typeCode).any { it.code == code.trim().uppercase() }
    fun containsCode(code: String): Boolean = listOf("1", "2", "3").any { contains(it, code) }
}
