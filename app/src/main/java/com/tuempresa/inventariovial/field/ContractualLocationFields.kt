package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.tuempresa.inventariovial.validation.ContractualLocationInput

@Composable fun ContractualPrField(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier.fillMaxWidth(),
    label:@Composable ()->Unit,placeholder:(@Composable ()->Unit)?=null,readOnly:Boolean=false,enabled:Boolean=true) {
    ContractualNumberField(value,onValueChange,true,modifier,label,placeholder,readOnly,enabled)
}
@Composable fun PrDistanceField(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier.fillMaxWidth(),
    label:@Composable ()->Unit,placeholder:(@Composable ()->Unit)?=null,readOnly:Boolean=false,enabled:Boolean=true) {
    ContractualNumberField(value,onValueChange,false,modifier,label,placeholder,readOnly,enabled)
}
@Composable private fun ContractualNumberField(value:String,onChange:(String)->Unit,pr:Boolean,modifier:Modifier,
    label:@Composable ()->Unit,placeholder:(@Composable ()->Unit)?,readOnly:Boolean,enabled:Boolean) {
    var rejected by remember {mutableStateOf(false)}
    val error=if(pr) ContractualLocationInput.prError(value) else ContractualLocationInput.distanceError(value)
    val message=if(rejected) (if(pr) "PR: solo dígitos, máximo 4. Entrada rechazada." else "Distancia: máximo 3 cifras enteras. Entrada rechazada.") else error
    OutlinedTextField(value,{next->
        val accepted=if(pr) ContractualLocationInput.acceptPr(next) else ContractualLocationInput.acceptDistance(next)
        rejected=!accepted
        if(accepted) onChange(next)
    },modifier=modifier,label=label,placeholder=placeholder,readOnly=readOnly,enabled=enabled,singleLine=true,
        isError=message!=null,supportingText={message?.let {Text(it,color=MaterialTheme.colorScheme.error)}},
        keyboardOptions=KeyboardOptions(keyboardType=if(pr)KeyboardType.Number else KeyboardType.Decimal))
}
