package com.tuempresa.inventariovial

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.supplementary.FieldKind
import java.io.File

internal object SicConsolidationFixtures {
    fun db(c:Context)=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    fun repo(c:Context,db:InventoryDatabase)=ScapRepository(db,ScapCatalog.load(c))
    suspend fun bridge(r:ScapRepository,name:String,progressive:String="10+100",route:String="PE-28H"):String {
        val id=r.create(name,"F-$name",null,ScapBridgeType.MODULAR)
        r.setField(id,"inspection","bridgeName",name);r.setField(id,"inspection","bridgeCode","F-$name")
        r.setField(id,"inspection","route",route);r.setField(id,"inspection","progressive",progressive)
        r.saveSicLocation(id,route,"CD",progressive.substringBefore('+').padStart(4,'0'),progressive.substringAfter('+'),"0012","0")
        for(format in BridgeSicContract.formats)for(key in BridgeSicContract.fields(format).map{it.key}.filter{it !in BridgeSicContract.unresolved(format) && !(format=="SIC-17" && it in BridgeSicContract.locationKeys)}) {
            val f=BridgeSicContract.fields(format,BridgeSicContract.manualValues(r.dao.snapshot(id)!!,format)).single{it.key==key}
            val value=when(f.kind){
                FieldKind.CODE,FieldKind.DESCRIBED_CODE->f.options.first()
                FieldKind.INTEGER->"2";FieldKind.YEAR->"2001"
                FieldKind.DECIMAL,FieldKind.SIGNED_DECIMAL->"8.4"
                FieldKind.LATITUDE->"-12.1";FieldKind.LONGITUDE->"-77.1"
                else->when(key){"surveyDate"->"26/10/2026";"bridgeName","singularityName"->name;"bridgeCode"->"F-$name";else->"FICTICIO_$key"}
            };r.setSicField(id,format,key,value)
        }
        return id
    }
}

/** Storage fault injection only; Android provider contract has separate tests. */
internal class SicTestStorage:SharedDocumentStore {
    data class Entry(val uri:Uri,var name:String,var parent:String,var bytes:ByteArray=byteArrayOf(),var published:Boolean=false)
    val entries=linkedMapOf<Uri,Entry>();private var sequence=0;var shortWrite=false;var writes=0;var stopAfterBackup=false;var stopAfterPromote=false;var deny=false
    fun find(uri:Uri)=entries.getValue(uri)
    fun canonical(format:String,route:String="PE-28H")=entries.values.single{it.name=="$format.xlsx"&&it.parent.contains("/$route/")}
    override fun reserve(file:LocalSharedFile,knownUri:Uri?):SharedDocumentStore.Slot {
        if(deny)throw SecurityException("FICTICIO_DENIED")
        val e=knownUri?.let(entries::get) ?: entries.values.singleOrNull{it.name==file.name&&it.parent==file.relativePath}
            ?: Entry(Uri.parse("content://ficticio.sic/${++sequence}"),file.name,file.relativePath).also{entries[it.uri]=it}
        return SharedDocumentStore.Slot(e.uri,e.published)
    }
    override fun checkSlot(slot:SharedDocumentStore.Slot,file:LocalSharedFile) {
        val e=find(slot.uri);require(e.name==file.name&&e.parent==file.relativePath)
    }
    override fun write(uri:Uri,source:File){writes++;find(uri).bytes=source.readBytes().let{if(shortWrite)it.take(7).toByteArray()else it}}
    override fun digest(uri:Uri)=find(uri).bytes.let{it.size.toLong() to com.tuempresa.inventariovial.delivery.sha256(it)}
    override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile):Uri {find(slot.uri).published=true;return slot.uri}
    override fun renameOwned(slot:SharedDocumentStore.Slot,from:LocalSharedFile,to:LocalSharedFile):SharedDocumentStore.Slot {
        val e=find(slot.uri);require(e.name in setOf(from.name,to.name)&&e.parent==from.relativePath)
        require(digest(slot.uri)==(from.size to from.sha256))
        require(entries.values.none{it.uri!=e.uri&&it.parent==to.relativePath&&it.name==to.name}){"FICTICIO_NAME_OCCUPIED"}
        e.name=to.name
        if(stopAfterBackup && (to.name.contains(".anterior-")||to.name.startsWith(".Historial - "))){stopAfterBackup=false;throw kotlinx.coroutines.CancellationException("FICTICIO_STOP_AFTER_BACKUP")}
        if(stopAfterPromote && !to.name.startsWith('.')){stopAfterPromote=false;throw kotlinx.coroutines.CancellationException("FICTICIO_LOST_PROMOTION_RESULT")}
        return SharedDocumentStore.Slot(e.uri,true)
    }
}
