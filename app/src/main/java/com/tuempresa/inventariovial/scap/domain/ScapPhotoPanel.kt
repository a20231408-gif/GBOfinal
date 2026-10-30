package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import java.text.Normalizer
import java.util.Locale

data class ScapPanelCategory(val order:Int,val label:String,val abbreviation:String) {
    val number get()=order.toString().padStart(2,'0')
    val key get()="SCAP20_$number"
    val shortName get()="${number}_$abbreviation"
    val folder get()="${number}_"+ScapPhotoPanel.normalize(label).replace(' ','_')
    val displayLabel get()="$number. $label"
    fun fileName(index:Int,extension:String="jpg",element:Int?=null):String {
        require(index>0 && extension in setOf("jpg","jpeg","png","webp","heic","mp4","webm","3gp","bin"))
        require(element==null || element>0)
        return (if(element==null)shortName else "${number}_PUE${element}_$abbreviation")+"_${index.toString().padStart(3,'0')}.$extension"
    }
}

/** Official panel v2. Persisted media UUID/category bindings are never migrated on read. */
object ScapPhotoPanel {
    const val VERSION=2
    val categories=listOf(
        "VISTA PANORAMICA LADO DERECHO" to "PAN_DER",
        "VISTA PANORAMICA LADO IZQUIERDO" to "PAN_IZQ",
        "VISTA PERFIL" to "PERFIL",
        "ACCESO IZQUIERDO DEL PUENTE" to "ACC_IZQ",
        "ACCESO DERECHO DEL PUENTE" to "ACC_DER",
        "SEÑALES INFORMATIVAS ACCESO IZQUIERDO" to "SEN_ACC_IZQ",
        "SEÑALES INFORMATIVAS ACCESO DERECHO" to "SEN_ACC_DER",
        "SUPERFICIE DE RODADURA" to "RODADURA",
        "IMAGEN AGUAS ARRIBA SUPERESTRUCTURA" to "SUPER_AGUAS_ARRIBA",
        "IMAGEN AGUAS ABAJO SUPERESTRUCTURA" to "SUPER_AGUAS_ABAJO",
        "VEREDA, BARANDAS Y GUARDAVIA AGUAS ARRIBA" to "VEREDA_BARANDA_GUARDAVIA_ARRIBA",
        "VEREDA, BARANDAS Y GUARDAVIA AGUAS ABAJO" to "VEREDA_BARANDA_GUARDAVIA_ABAJO",
        "TUBERIA DE DRENAJE AGUAS ARRIBA" to "DRENAJE_ARRIBA",
        "TUBERIA DE DRENAJE AGUAS ABAJO" to "DRENAJE_ABAJO",
        "ESTRIBO IZQUIERDO" to "ESTRIBO_IZQ",
        "ESTRIBO DERECHO" to "ESTRIBO_DER",
        "PILARES" to "PILARES",
        "TABLERO VISTA INFERIOR" to "TABLERO_INF",
        "VISTA DE CAUCE AGUAS ARRIBA" to "CAUCE_ARRIBA",
        "VISTA DE CAUCE AGUAS ABAJO" to "CAUCE_ABAJO"
    ).mapIndexed{i,(label,short)->ScapPanelCategory(i+1,label,short)}
    fun normalize(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
        .uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]+")," ").trim()
    fun forCode(key:String?)=categories.find{it.key==key}
    private val aliases=mapOf(
        "VISTA LADO DERECHO" to 1,"VISTA LADO IZQUIERDO" to 2,
        "ACCESO IZQUIERDO" to 4,"AFIRMADO ACCESO IZQUIERDO" to 4,
        "ACCESO DERECHO" to 5,"AFIRMADO ACCESO DERECHO" to 5,
        "SEÑAL INFORMATIVA DEL ACCESO IZQUIERDO" to 6,
        "SEÑAL INFORMATIVA DEL ACCESO DERECHO" to 7,
        "SUPERFICIE DE RODARURA" to 8,"CARRETERA TABLERO" to 8,
        "VEREDA AGUAS ARRIBA" to 11,"BARANDA AGUAS ARRIBA" to 11,
        "VEREDA AGUAS ABAJO" to 12,"BARANDAS AGUAS ABAJO" to 12,
        "PANTALLA DE ESTRIBO IZQUIERDO" to 15,"PANTALLA DE ESTRIBO DERECHO" to 16,
        "LOSA CARA INFERIOR" to 18,"CAUCE AGUAS ARRIBA" to 19,"CAUCE AGUAS ABAJO" to 20
    ).mapKeys{normalize(it.key)}
    fun resolve(key:String?,label:String?):ScapPanelCategory? {
        forCode(key)?.let{return it}
        val normalized=normalize(label.orEmpty())
        categories.find{normalize(it.label)==normalized || normalize(it.displayLabel)==normalized || normalize(it.shortName)==normalized}?.let{return it}
        aliases[normalized]?.let{return categories[it-1]}
        // Generic upstream/downstream/access/drainage labels are ambiguous: retain as historical.
        return when(key){"RIGHT_SIDE"->categories[0];"LEFT_SIDE"->categories[1];"ROAD_DECK"->categories[7];else->null}
    }
    fun category(s:ScapInspectionSnapshot,p:PhotoEntity)=resolve(p.photoCategory,s.values(p.id)["photoName"])
    fun ordered(s:ScapInspectionSnapshot)=s.photos.sortedWith(compareBy<PhotoEntity>({category(s,it)?.order ?: 21},{it.photoIndex},{it.id}))
    fun photoPath(s:ScapInspectionSnapshot,p:PhotoEntity,extension:String="jpg",root:String="fotos"):String {
        val cat=category(s,p)
        val group=ordered(s).filter{category(s,it)?.key==cat?.key}
        val index=group.indexOfFirst{it.id==p.id}+1
        require(index>0)
        return if(cat!=null) "$root/${cat.folder}/${cat.fileName(index,extension)}"
        else "$root/HISTORICO_SIN_CLASIFICAR/HISTORICO_${index.toString().padStart(3,'0')}.$extension"
    }
}
