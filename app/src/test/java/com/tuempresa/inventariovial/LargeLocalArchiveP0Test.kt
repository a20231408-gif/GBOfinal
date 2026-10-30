package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.localfiles.VerifiedZipArchive
import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipFile

class LargeLocalArchiveP0Test {
    private val dir get()=File(System.getProperty("gbo.large.evidence","../tmp/large-archive"),java.util.UUID.randomUUID().toString()).apply{mkdirs()}
    private fun source(f:File):VerifiedZipArchive.Source {
        val hash=MessageDigest.getInstance("SHA-256");f.inputStream().use{input->val b=ByteArray(64*1024);while(true){val n=input.read(b);if(n<0)break;hash.update(b,0,n)}}
        return VerifiedZipArchive.Source(f.name,f.length(),hash.digest().joinToString(""){"%02x".format(it)}){f.inputStream()}
    }
    @Test(timeout=180000) fun actualZipAbove512MiBUsesBoundedHeapPreservesBytesAndReportsProgress(){
        val folder=dir;val original=File(folder,"EVIDENCIA_600MiB.mp4")
        val block=ByteArray(1024*1024);java.util.Random(101).nextBytes(block)
        original.outputStream().buffered().use{out->repeat(600){out.write(block)}}
        val s=source(original);val progress=mutableListOf<VerifiedZipArchive.Progress>()
        val result=VerifiedZipArchive.write(File(folder,"Puente Prueba3 - PE-28H - PR 10+123.zip"),listOf(s),progress={progress+=it})
        assertEquals(600L*1024*1024,result.originalBytes);assertTrue(result.bytes>512L*1024*1024)
        assertTrue(Runtime.getRuntime().maxMemory()<512L*1024*1024)
        assertEquals(s.sha256,source(original).sha256);assertTrue(progress.any{it.phase=="WRITING"});assertTrue(progress.any{it.phase=="VERIFYING"})
        ZipFile(result.file).use{assertEquals(1,it.size())}
        File(folder,"MEDICION.json").writeText("""{"originalBytes":${s.size},"zipBytes":${result.bytes},"jvmMaxHeapBytes":${Runtime.getRuntime().maxMemory()},"verified":true,"sha256":"${result.sha256}"}""")
    }
    @Test(timeout=240000) fun zip64EntryBeyond4GiBIsVerifiedWithoutLoadingItIntoMemory(){
        val folder=dir;val original=File(folder,"VIDEO_ZIP64_FICTICIO.bin")
        RandomAccessFile(original,"rw").use{it.setLength(4L*1024*1024*1024+65536)}
        val s=source(original);val result=VerifiedZipArchive.write(File(folder,"ZIP64.zip"),listOf(s))
        ZipFile(result.file).use{assertEquals(s.size,it.getEntry(original.name).size)}
        assertTrue(s.size>0xffffffffL);assertEquals(s.sha256,source(original).sha256)
        File(folder,"MEDICION_ZIP64.json").writeText("""{"originalBytes":${s.size},"zipBytes":${result.bytes},"zip64Verified":true,"jvmMaxHeapBytes":${Runtime.getRuntime().maxMemory()}}""")
    }
    @Test fun insufficientSpaceAndInterruptedWritesCleanOnlyOwnedTemporaryFiles(){
        val folder=dir;val original=File(folder,"original.bin").apply{writeBytes(ByteArray(16*1024*1024){(it%251).toByte()})};val s=source(original)
        val target=File(folder,"paquete.zip");val other=File(folder,"otro.partial").apply{writeText("conservar")}
        assertTrue(runCatching{VerifiedZipArchive.write(target,listOf(s),available={1})}.isFailure)
        assertFalse(target.exists());assertEquals(s.sha256,source(original).sha256)
        var reads=0
        val broken=s.copy(open={object:FilterInputStream(original.inputStream()){override fun read(b:ByteArray,o:Int,l:Int):Int{if(++reads>4)throw IOException("TEST_STOP");return super.read(b,o,l)}}})
        assertTrue(runCatching{VerifiedZipArchive.write(target,listOf(broken))}.isFailure)
        assertFalse(target.exists());assertTrue(other.exists());assertEquals(s.sha256,source(original).sha256)
        assertFalse(folder.listFiles()!!.any{it.name.startsWith("gbo-zip-")})
    }
    @Test fun smallArchiveDetectsChangedSourcesAndNeverOverwritesPriorArchive(){
        val folder=dir;val f=File(folder,"original.bin").apply{writeText("TEST_ONLY_ORIGINAL")};val s=source(f)
        val result=VerifiedZipArchive.write(File(folder,"small.zip"),listOf(s));val old=result.file.readBytes()
        f.writeText("TEST_CHANGED_SOURCE")
        assertTrue(runCatching{VerifiedZipArchive.write(result.file,listOf(source(f)))}.isFailure)
        assertArrayEquals(old,result.file.readBytes());assertEquals("TEST_CHANGED_SOURCE",f.readText())
    }
}
