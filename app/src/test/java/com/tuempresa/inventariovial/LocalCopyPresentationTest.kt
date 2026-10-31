package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.localfiles.*
import org.junit.Assert.*
import org.junit.Test

class LocalCopyPresentationTest {
    private fun job(id:String,scope:String="FULL",revision:Long=1,created:Long=1,key:String="SCAP/FICTITIOUS")=
        LocalExportJob(id,key,"SCAP",key.substringAfter('/'),revision,scope,createdAt=created)
    @Test fun newerLayoutReplacesOnlyItsOwnCategoryAndNeverAnotherRecordOrConfirmedMirror(){
        val old=job("OLD");val current=job("CURRENT","FULL_LAYOUT_V4",created=2)
        val photos=job("PHOTOS","PHOTOS");val mirror=job("MIRROR",DriveMirrorPlan.SCOPE);val other=job("OTHER",key="SCAP/ANOTHER")
        assertEquals(setOf("CURRENT","PHOTOS","MIRROR","OTHER"),LocalCopyPresentation.latest(listOf(old,photos,current,mirror,other)).map{it.id}.toSet())
        assertTrue(LocalCopyPresentation.context(old,1,false).contains("intento anterior"))
    }
    @Test fun sameTimestampPrefersCurrentLayoutButNewerRecordRevisionAlwaysWins(){
        val old=job("A");val current=job("Z","FULL_LAYOUT_V4")
        assertEquals("Z",LocalCopyPresentation.latest(listOf(old,current)).single().id)
        val revision=job("NEW_REVISION",revision=2)
        assertEquals("NEW_REVISION",LocalCopyPresentation.latest(listOf(old,current,revision)).single().id)
    }
    @Test fun failedRegenerationRemainsVisibleAndHistoricalJobIsRetainedForExplicitHistory(){
        val old=job("OLD").copy(state="READY");val failed=job("FAILED","FULL_LAYOUT_V4",created=2).copy(state="FAILED",message="FICTITIOUS_FAILURE")
        val history=listOf(old,failed)
        assertEquals(failed,LocalCopyPresentation.latest(history).single());assertEquals("FICTITIOUS_FAILURE",LocalCopyPresentation.result(failed));assertEquals(old,history.first())
    }
    @Test fun quickDocumentsPreferModernCopiesAndKeepHistoryAndOnlyAvailableOriginalAccessible(){
        val old=job("OLD").copy(state="READY");val modern=job("MODERN","FULL_LAYOUT_V4",created=2)
        val oldFile=LocalSharedFile("OLD_FILE",old.id,old.recordKey,"Download/UUID_FICTITIOUS/","old.pdf","application/pdf",1,"a".repeat(64),"FICTITIOUS",state="READY")
        val fresh=oldFile.copy(id="MODERN_FILE",jobId=modern.id,relativePath="Download/Puente Ficticio/",name="modern.pdf")
        assertEquals(listOf(fresh),LocalCopyPresentation.currentFiles(listOf(old,modern),listOf(oldFile,fresh)))
        assertEquals(listOf(oldFile,fresh),LocalCopyPresentation.currentFiles(listOf(old,modern),listOf(oldFile,fresh),true))
        assertEquals(listOf(oldFile),LocalCopyPresentation.currentFiles(listOf(old,modern),listOf(oldFile)))
    }
}
