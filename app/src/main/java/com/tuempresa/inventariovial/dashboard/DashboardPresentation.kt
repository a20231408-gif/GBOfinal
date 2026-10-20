package com.tuempresa.inventariovial.dashboard

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.core.DriveSyncIntent
import com.tuempresa.inventariovial.road.*
import java.time.*
import java.util.Locale

/** Read-only presentation. PR comparison never estimates contractual metres. */
object DashboardDrafts {
    fun date(record:InventoryRecordEntity,sessions:List<FieldSession>,zone:ZoneId=ZoneId.systemDefault()):LocalDate =
        Instant.ofEpochMilli(sessions.find {it.sessionId==record.sessionId}?.startTime ?: record.createdAt).atZone(zone).toLocalDate()
    fun title(record:InventoryRecordEntity):String = when(record.sicCode) {
        "SIC-19"->"Cuneta"; "SIC-21"->"Señalización"; "SIC-22"->"Elemento de seguridad"
        "SIC-17"->"Puente"; "SIC-23"->"Derecho de vía"
        "SIC-20"->when(record.assetType.uppercase()) {"WALL","MURO"->"Muro";"TUNNEL","TUNEL"->"Túnel";else->"Badén"}
        else->com.tuempresa.inventariovial.sic.InventoryElements.title(record.sicCode)
    }
    fun progressive(record:InventoryRecordEntity):String {
        if(record.startPrCode.isBlank() || !record.startDistanceM.isFinite() || record.startDistanceM<0) return "PROGRESIVA PENDIENTE"
        val start="PR ${normalizedPr(record.startPrCode)}+${String.format(Locale.ROOT,"%06.2f",record.startDistanceM)}"
        return if(!record.endPrCode.isNullOrBlank() && record.endDistanceM?.isFinite()==true)
            "$start → ${normalizedPr(record.endPrCode)}+${String.format(Locale.ROOT,"%06.2f",record.endDistanceM)}" else start
    }
    fun context(record:InventoryRecordEntity,sessions:List<FieldSession>):String {
        val session=sessions.find {it.sessionId==record.sessionId}
        val whenStarted=session?.let {Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5)}
        return "${record.routeCode} · ${record.roadbedCode}" + if(session==null) " · Sin sesión histórica" else
            " · ${session.project} · ${session.operator} · sesión $whenStarted · ${if(session.direction=="DECREASING") "↓" else "↑"}"
    }
    fun order(records:List<InventoryRecordEntity>,sessions:List<FieldSession>,prs:List<RoadPr>):List<InventoryRecordEntity> {
        val sessionMap=sessions.associateBy {it.sessionId}
        fun position(r:InventoryRecordEntity):Pair<Double,Double>? {
            if(r.startPrCode.isBlank() || !r.startDistanceM.isFinite() || r.startDistanceM<0) return null
            val catalog=ContractualPrInput.catalog(prs,r.routeCode,r.roadbedCode).sortedBy {it.chainageM}
            val rank=catalog.indexOfFirst {normalizedPr(it.prCode)==normalizedPr(r.startPrCode)}
            val code=if(catalog.isNotEmpty()) rank.takeIf {it>=0}?.toDouble() else r.startPrCode.trim().toDoubleOrNull()
            return code?.takeIf {it.isFinite()}?.let {it to r.startDistanceM}
        }
        return records.sortedWith {a,b->
            val pa=position(a);val pb=position(b)
            // Missing/unknown catalogue PR always follows all positioned records.
            var result=compareValues(pa==null,pb==null)
            if(result==0) result=compareValuesBy(a,b,{normalizedRoadCode(it.routeCode)},{normalizedRoadCode(it.roadbedCode)},
                {sessionMap[it.sessionId]?.startTime ?: 0L},{it.sessionId.orEmpty()},
                {sessionMap[it.sessionId]?.direction ?: it.surveyDirection.orEmpty()})
            if(result==0 && pa!=null && pb!=null) {
                result=compareValues(pa.first,pb.first).takeIf {it!=0} ?: compareValues(pa.second,pb.second)
                val direction=sessionMap[a.sessionId]?.direction ?: a.surveyDirection
                if(direction=="DECREASING") result = -result
            }
            if(result==0) result=compareValuesBy(a,b,{it.createdAt},{it.id})
            result
        }
    }
    fun filter(records:List<InventoryRecordEntity>,sessions:List<FieldSession>,day:LocalDate?=null,
        route:String="",bed:String="",type:String=""):List<InventoryRecordEntity> = records.filter {
        (day==null || date(it,sessions)==day) && (route.isBlank() || it.routeCode.equals(route,true)) &&
            (bed.isBlank() || it.roadbedCode.equals(bed,true)) && title(it).contains(type,true)
    }
}

data class DashboardDeliveryCounts(val synced:Int,val pending:Int,val errors:Int,val updates:Int) {
    companion object {
        fun from(intents:List<DriveSyncIntent>):DashboardDeliveryCounts {
            val eligible=intents.filter {it.state!="DRAFT"}
            fun confirmed(i:DriveSyncIntent)=com.tuempresa.inventariovial.core.SyncIntentPresentation.confirmed(i)
            fun error(i:DriveSyncIntent)=i.state in setOf("CONFLICT","BLOCKED") || i.state=="ERROR" && i.error?.contains("(BUSY)")!=true || i.state=="SYNCED" && !confirmed(i)
            val errors=eligible.count(::error)
            val synced=eligible.count(::confirmed)
            val updates=eligible.count {!confirmed(it) && !error(it) && it.syncedRevision>0 && it.revision>it.syncedRevision}
            return DashboardDeliveryCounts(synced,eligible.size-synced-errors-updates,errors,updates)
        }
    }
}
