package com.tuempresa.inventariovial.scap.ui

import androidx.compose.runtime.Composable
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

@Composable
fun ScapSicSupplementPanel(s:ScapInspectionSnapshot,c:ScapController,editable:Boolean) = ScapBridgeSicPanel(s,c,editable)
