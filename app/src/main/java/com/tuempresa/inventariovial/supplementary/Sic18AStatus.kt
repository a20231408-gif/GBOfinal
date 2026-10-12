package com.tuempresa.inventariovial.supplementary

import com.tuempresa.inventariovial.catalog.EngineeringConditions
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic18AEntity

object Sic18AStatus {
    fun inherited(parent: Sic18Entity, detail: Sic18AEntity?) = detail?.values().orEmpty() + mapOf(
        "classCode" to parent.classCode, "typeCode" to parent.typeCode, "spans" to parent.spans?.toString().orEmpty(),
        "structuralConditionCode" to parent.structuralConditionCode, "functionalConditionCode" to parent.functionalConditionCode)
    fun label(parent: Sic18Entity, detail: Sic18AEntity?): String = when {
        detail != null && SupplementaryFormState(SupplementaryFormat.SIC18A, inherited(parent,detail)).validate().isEmpty() -> "Completo"
        detail != null || EngineeringConditions.culvertFailuresRequired(parent.structuralConditionCode,parent.functionalConditionCode) -> "Pendiente"
        else -> "No requerido"
    }
}
