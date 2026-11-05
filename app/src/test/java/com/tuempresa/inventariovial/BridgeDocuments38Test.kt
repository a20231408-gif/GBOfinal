package com.tuempresa.inventariovial

import android.app.Application
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BridgeDocuments38Test {
    @get:Rule val ui=createComposeRule()
    private fun job(id:String="old",revision:Long=1,state:String="READY")=LocalExportJob(id,"SCAP/FICTITIOUS","SCAP","FICTITIOUS",revision,scope="FULL",state=state,createdAt=revision)
    private fun file(id:String="old",name:String="Puente_ficticio.xlsx",mime:String="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")=
        LocalSharedFile(id,id,"SCAP/FICTITIOUS","Download/GBO/Puente ficticio/",name,mime,1,"a".repeat(64),"FICTITIOUS_SOURCE","content://media/external/downloads/123","READY")
    @Test fun oneDocumentHasNoInventedPreviousVersionAndDoesNotChangeInputs() {
        val jobs=listOf(job());val files=listOf(file())
        ui.setContent{MaterialTheme{BridgeDocumentsContent("Puente ficticio","PE-12A","42+995",1,jobs,files){}}}
        ui.onNodeWithText("Puente ficticio").assertExists()
        ui.onNodeWithText("No hay versiones anteriores disponibles.").assertExists()
        assertEquals(files,BridgeDocuments.visible(jobs,files,false));assertEquals("READY",jobs.single().state)
    }
    @Test fun previousGenerationRemainsAccessibleAndHistoryIsExplicit() {
        val old=job();val now=job("new",2);val a=file();val b=file("new","Actual.xlsx").copy(uri="content://media/external/downloads/124")
        assertEquals(listOf(b),BridgeDocuments.visible(listOf(old,now),listOf(a,b),false))
        assertEquals(listOf(a,b),BridgeDocuments.visible(listOf(old,now),listOf(a,b),true))
        ui.setContent{MaterialTheme{BridgeDocumentsContent("Puente ficticio","R","42+995",2,listOf(old,now),listOf(a,b)){}}}
        ui.onNodeWithText("Ver revisiones anteriores").performClick()
        ui.onNodeWithText("Ocultar revisiones anteriores").assertExists()
    }
    @Test fun failedNewAttemptKeepsVerifiedOldFileAndRealFailure() {
        val old=job();val failed=job("failed",2,"FAILED").copy(message="Fallo sintético de espacio")
        assertEquals(listOf(file()),BridgeDocuments.visible(listOf(old,failed),listOf(file()),false))
        ui.setContent{MaterialTheme{BridgeDocumentsContent("Puente","R","1+0",2,listOf(old,failed),listOf(file())){}}}
        ui.onNodeWithText("Fallo sintético de espacio").assertExists()
    }
    @Test fun provisionalAndVerifiedDoNotClaimRemoteFinalAck() {
        assertTrue(BridgeDocuments.label(file(name="BORRADOR.xlsx")).startsWith("BORRADOR"))
        assertTrue(BridgeDocuments.label(file(name="paquete.zip"),true).startsWith("BORRADOR"))
        assertEquals("Verificado localmente · no acredita ACK remoto",BridgeDocuments.label(file()))
    }
    @Test fun technicalHistoricalProvisionalsStayPreservedButOutsideUsefulDocuments() {
        val jobs=listOf(job());val technical=file(name="SCAP_PROVISIONAL.xlsx").copy(source="private/SCAP_PROVISIONAL.xlsx")
        val working=file("working","SCAP - PE-3NG - PRUEBA 2.xlsx").copy(jobId="old",source="private/BORRADOR/SCAP.xlsx",uri="content://media/external/downloads/125")
        val sic=file("sic","SIC-17.xlsx").copy(jobId="old",uri="content://media/external/downloads/126")
        val inputs=listOf(technical,working,sic)
        assertEquals(listOf(working),BridgeDocuments.visible(jobs,inputs,false))
        assertTrue(BridgeDocuments.visible(jobs,inputs,true).isEmpty())
        assertEquals("BORRADOR · copia de trabajo",BridgeDocuments.label(working))
        assertEquals(3,inputs.size);assertTrue(inputs.contains(technical)) // Projection never deletes sources or references.
        ui.setContent{MaterialTheme{BridgeDocumentsContent("PRUEBA 2","PE-3NG","10+100",1,jobs,inputs){}}}
        ui.onNodeWithText("SCAP_PROVISIONAL.xlsx",substring=true).assertDoesNotExist()
        ui.onNodeWithTag("bridge-documents").performScrollToNode(hasText("BORRADOR · copia de trabajo"))
        ui.onNodeWithText("BORRADOR · copia de trabajo").assertExists()
    }
    @Test fun categoriesSeparateSketchesFromPhotographsAndDocuments() {
        val cases=listOf("Excel" to file(),"PDF" to file(name="f.pdf",mime="application/pdf"),"ZIP" to file(name="f.zip",mime="application/zip"),"Fotografías" to file(name="f.jpg",mime="image/jpeg"),"Croquis" to file(name="croquis.png",mime="image/png"))
        cases.forEach{(category,f)->assertEquals(category,BridgeDocuments.category(f))}
    }
    @Test fun queuedCopyDoesNotInventFilesOrScheduleAnotherJob() {
        val jobs=listOf(job(state="QUEUED"));assertTrue(BridgeDocuments.visible(jobs,emptyList(),false).isEmpty())
        ui.setContent{MaterialTheme{BridgeDocumentsContent("Puente","R","1+0",1,jobs,emptyList()){}}}
        ui.onNodeWithText("Todavía no hay copia local verificada.",substring=true).assertExists()
        assertEquals(1,jobs.size);assertEquals("QUEUED",jobs.single().state)
    }
    @Test fun exactFileUriAndFolderAvailabilityAreDifferentContracts() {
        val uri=Uri.parse(file().uri)
        assertEquals(uri,SharedFileActions.view(uri,file().mimeType).data)
        assertEquals(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS,SharedFileActions.location(uri)!!.action)
        val tree=Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments/document/primary%3ADocuments%2Fchild%2Ff.pdf")
        val root=SharedFileActions.location(tree)!!
        assertEquals("vnd.android.document/directory",root.type)
        assertFalse(root.data.toString().contains("child")) // Authorized tree is not an invented exact parent.
        assertNull(SharedFileActions.location(Uri.parse("content://unknown.provider/document/123")))
    }
}
