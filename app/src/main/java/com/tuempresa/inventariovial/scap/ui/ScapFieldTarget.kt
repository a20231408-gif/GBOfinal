package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.tuempresa.inventariovial.scap.domain.ScapFieldRoute
import kotlinx.coroutines.delay

internal val LocalScapDestination=staticCompositionLocalOf<Pair<Long,ScapFieldRoute>?>{null}
internal val LocalScapFocus=staticCompositionLocalOf<FocusRequester?>{null}
@Composable internal fun Modifier.scapFocus()=LocalScapFocus.current?.let{focusRequester(it)} ?: this
@Composable internal fun ScapFieldTarget(owner:String,key:String,content:@Composable ()->Unit) {
    val destination=LocalScapDestination.current
    val target=destination?.second?.let{it.repeatableItemId==owner && it.fieldKey==key}==true
    val bring=remember{BringIntoViewRequester()};val focus=remember{FocusRequester()}
    var highlight by remember{mutableStateOf(false)}
    LaunchedEffect(destination,target){if(target){highlight=true;delay(150);bring.bringIntoView();runCatching{focus.requestFocus()};delay(1800);highlight=false}}
    Column(Modifier.fillMaxWidth().testTag("scap:$owner:$key").bringIntoViewRequester(bring)
        .background(if(highlight)MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
        CompositionLocalProvider(LocalScapFocus provides focus){content()}
    }
}
