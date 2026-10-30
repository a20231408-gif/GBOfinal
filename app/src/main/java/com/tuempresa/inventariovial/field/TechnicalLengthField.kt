package com.tuempresa.inventariovial.field

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.tuempresa.inventariovial.validation.LengthEndpoint
import com.tuempresa.inventariovial.validation.ContractualLength
import com.tuempresa.inventariovial.validation.TechnicalLength
import java.util.Locale

@Composable fun TechnicalLengthField(value:TechnicalLength,onApply:((Double)->Unit)?=null,
    kilometreStart:LengthEndpoint?=null,kilometreEnd:LengthEndpoint?=null,direction:String="INCREASING") {
    Text("Longitud calculada (m): ${value.metres?.let {String.format(Locale.US,"%.2f",it)} ?: "Pendiente"}")
    value.warning?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    if(value.metres!=null && onApply!=null) OutlinedButton(onClick={onApply(value.metres)}) {Text("Usar longitud calculada")}
    if(kilometreStart!=null && kilometreEnd!=null) {
        var explicit by rememberSaveable {mutableStateOf(false)}
        FilterChip(explicit,{explicit=!explicit},label={Text("Los extremos son kilómetros reales")})
        if(explicit) {
            val km=ContractualLength.kilometres(kilometreStart,kilometreEnd,direction)
            Text("Longitud kilométrica (m): ${km.metres?.let {String.format(Locale.US,"%.2f",it)} ?: "Pendiente"}")
            km.warning?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Text("Cálculo auxiliar: conserva los PR contractuales y la longitud guardada.")
        }
    }
}
