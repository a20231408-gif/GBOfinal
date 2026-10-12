package com.tuempresa.inventariovial.field

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.tuempresa.inventariovial.validation.TechnicalLength
import java.util.Locale

@Composable fun TechnicalLengthField(value:TechnicalLength,onApply:((Double)->Unit)?=null) {
    Text("Longitud calculada (m): ${value.metres?.let {String.format(Locale.US,"%.2f",it)} ?: "Pendiente"}")
    value.warning?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    if(value.metres!=null && onApply!=null) OutlinedButton(onClick={onApply(value.metres)}) {Text("Usar longitud calculada")}
}
