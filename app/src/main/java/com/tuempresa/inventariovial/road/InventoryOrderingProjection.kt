package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

/** Read-only projections. Never persist synthetic coordinates or send virtual map rows to the SIC queue. */
object InventoryOrderingProjection {
    fun pavement(sample:PavementSampleEntity,damages:List<PavementDamageEntity>):List<InventorySnapshot> {
        if(sample.status!="CLOSED")return emptyList()
        return PavementSicFormat.entries.filter {it.surface==sample.surface}.flatMap {format->
            PavementSic.status(sample,damages,format).rows.map {row->
                val values=row.values
                val r=InventoryRecordEntity("PAVEMENT_${sample.id}_${row.damageId}_${format.name}",format.label,"PAVEMENT",
                    sample.ruta,sample.calzada,values[3]?.toString().orEmpty(),(values[4] as? Number)?.toDouble() ?: 0.0,
                    values[5]?.toString(),(values[6] as? Number)?.toDouble(),null,null,null,null,null,sample.fecha,
                    "Muestra ${sample.id}; daño ${row.damageId}; sin coordenadas geográficas en el modelo de pavimentos.",
                    "ACTIVE","PENDING","PENDING",sample.createdAt,sample.updatedAt,locationSource="PENDING_EXTERNAL")
                InventorySnapshot(r,emptyList(),emptyList(),null,null,null,null,null,null,null,null,null,null)
            }
        }
    }
    fun bridge(snapshot:ScapInspectionSnapshot):InventorySnapshot? {
        if(snapshot.inspection.status!="COMPLETE")return null
        val base=snapshot.roadRecord ?: return null
        val r=base.copy(sicCode="SIC-17",assetType="PUENTE",status="ACTIVE",
            latitude=snapshot.inspection.latitude ?: base.latitude,longitude=snapshot.inspection.longitude ?: base.longitude)
        return InventorySnapshot(r,snapshot.photos,emptyList(),null,null,null,null,null,null,null,null,null,null)
    }
}
