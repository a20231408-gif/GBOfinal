package com.tuempresa.inventariovial.scap.domain

data class ScapFieldRoute(val section:String,val subsection:String=section,val repeatableItemId:String="inspection",val fieldKey:String="")
enum class ScapIssueSeverity { WARNING, BLOCKING }
