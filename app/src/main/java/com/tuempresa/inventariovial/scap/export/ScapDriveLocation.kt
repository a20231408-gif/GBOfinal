package com.tuempresa.inventariovial.scap.export

import android.content.Context
import com.tuempresa.inventariovial.road.ChainageCalculator
import com.tuempresa.inventariovial.road.ProjectRouteCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.validation.ContractualLocationInput

object ScapDriveLocation {
    const val MESSAGE="No se puede preparar la entrega SCAP a Drive porque falta una progresiva válida. Ingresa el código PR y la distancia correspondiente en la ubicación SIC del puente. La ficha y sus fotografías permanecen guardadas en la tablet."
    fun issues(c:Context,s:ScapInspectionSnapshot):List<String> {
        val r=s.roadRecord ?: return listOf("Falta el registro vial vinculado")
        return buildList {
            if(r.routeCode.isBlank())add("Falta la ruta")
            else if(ProjectRouteCatalog.load(c).find(r.routeCode)==null)add("Ruta inválida: selecciona una carretera del proyecto")
            if(ProjectRouteCatalog.load(c).find(s.values()["route"])==null)add("Falta una ruta de proyecto válida en Identificación SCAP")
            if(r.startPrCode.isBlank())add("Falta el código PR")
            else if(!r.startPrCode.matches(Regex("[0-9]{1,4}")))add("Código PR inválido: utiliza de 1 a 4 dígitos")
            if(s.values()["sicLocation.startDistanceM"]=="" && s.values()["sicLocation.startPrCode"].isNullOrBlank())add("Falta la distancia desde PR en la ubicación pendiente")
            if(!r.startDistanceM.isFinite()||r.startDistanceM<0||r.startDistanceM>=1000)add("Distancia inválida: ingresa metros entre 0 y menos de 1000")
            if(isEmpty()&&ChainageCalculator().calculate(r).chainageM==null)add("PR y distancia no permiten calcular la progresiva")
        }
    }
}
