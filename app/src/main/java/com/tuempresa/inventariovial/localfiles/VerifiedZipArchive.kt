package com.tuempresa.inventariovial.localfiles

import java.io.*
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.*

/** Bounded buffers, Long sizes and the platform ZIP64 writer; never owns an original. */
object VerifiedZipArchive {
    data class Source(val path:String,val size:Long,val sha256:String,val open:()->InputStream)
    data class Progress(val phase:String,val bytes:Long,val total:Long)
    data class Result(val file:File,val bytes:Long,val sha256:String,val originalBytes:Long,val entries:Int)
    const val BUFFER=64*1024
    const val RESERVE=32L*1024*1024
    fun checkedSpace(available:Long,required:Long){
        require(required>=0)
        check(available>Math.addExact(required,RESERVE)){"Espacio insuficiente para preparar y publicar la copia. Originales conservados."}
    }
    private fun hex(bytes:ByteArray)=bytes.joinToString(""){"%02x".format(it)}
    private fun total(sources:List<Source>)=sources.fold(0L){n,s->Math.addExact(n,s.size)}
    /** Compression can expand data. Include conservative headers/DEFLATE overhead and a public copy. */
    fun requiredSpace(sources:List<Source>):Long {
        val n=total(sources)
        val bound=Math.addExact(n,Math.addExact(n/100,Math.addExact(sources.size*65536L,1024L*1024)))
        return Math.multiplyExact(bound,2)
    }
    private fun valid(s:Source){
        require(s.size>0&&s.sha256.matches(Regex("[a-f0-9]{64}")))
        require(s.path.isNotBlank()&&!s.path.startsWith('/')&&!s.path.contains('\\')&&
            s.path.split('/').all{it.isNotBlank()&&it!="."&&it!=".."}&&s.path.none{it<' '}){"Nombre ZIP inválido."}
    }
    fun verify(file:File,sources:List<Source>,cancel:()->Unit={},progress:(Progress)->Unit={}):Result {
        val expected=sources.associateBy{it.path};require(expected.size==sources.size)
        var done=0L;val sum=total(sources);val buffer=ByteArray(BUFFER)
        ZipFile(file).use{zip->
            val seen=mutableSetOf<String>();val entries=zip.entries()
            while(entries.hasMoreElements()){
                cancel();val entry=entries.nextElement();require(!entry.isDirectory&&seen.add(entry.name)){"Entrada ZIP duplicada."}
                val source=requireNotNull(expected[entry.name]){"Archivo ZIP no esperado."}
                val digest=MessageDigest.getInstance("SHA-256");val crc=CRC32();var count=0L
                zip.getInputStream(entry).use{input->while(true){cancel();val n=input.read(buffer);if(n<0)break
                    count=Math.addExact(count,n.toLong());require(count<=source.size){"Tamaño ZIP incorrecto."}
                    digest.update(buffer,0,n);crc.update(buffer,0,n);done+=n
                    if(done%(8L*1024*1024)<BUFFER)progress(Progress("VERIFYING",done,sum))
                }}
                require(count==source.size&&entry.size==count&&crc.value==entry.crc&&hex(digest.digest())==source.sha256){"No se verificó la integridad de ${entry.name}; originales conservados."}
            };require(seen==expected.keys){"Faltan archivos en el ZIP; originales conservados."}
        }
        progress(Progress("VERIFYING",sum,sum))
        val digest=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->while(true){cancel();val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)}}
        require(file.length()>0)
        return Result(file,file.length(),hex(digest.digest()),sum,sources.size)
    }
    @Synchronized fun write(target:File,sources:List<Source>,available:()->Long={target.parentFile!!.usableSpace},
        cancel:()->Unit={},progress:(Progress)->Unit={}):Result {
        require(sources.isNotEmpty()&&sources.size<=1000);sources.forEach(::valid)
        require(sources.map{it.path}.distinct().size==sources.size){"Nombres duplicados en el paquete."}
        check(target.parentFile!!.isDirectory||target.parentFile!!.mkdirs())
        if(target.exists())return verify(target,sources,cancel,progress)
        checkedSpace(available(),requiredSpace(sources))
        val partial=File.createTempFile("gbo-zip-",".partial",target.parentFile)
        var done=0L;val sum=total(sources);val buffer=ByteArray(BUFFER)
        try {
            FileOutputStream(partial).use{disk->
                val keepOpen=object:FilterOutputStream(disk){
                    override fun write(b:ByteArray,off:Int,len:Int){disk.write(b,off,len)}
                    override fun write(b:Int){disk.write(b)}
                    override fun close(){flush()}
                }
                ZipOutputStream(BufferedOutputStream(keepOpen,BUFFER)).use{zip->
                    zip.setLevel(Deflater.BEST_SPEED)
                    for(source in sources){
                        cancel();zip.putNextEntry(ZipEntry(source.path).apply{time=0})
                        val digest=MessageDigest.getInstance("SHA-256");var count=0L
                        source.open().use{input->while(true){cancel();val n=input.read(buffer);if(n<0)break
                            count=Math.addExact(count,n.toLong());require(count<=source.size){"Un original cambió de tamaño; se conserva."}
                            zip.write(buffer,0,n);digest.update(buffer,0,n);done+=n
                            if(done%(8L*1024*1024)<BUFFER){checkedSpace(available(),0);progress(Progress("WRITING",done,sum))}
                        }}
                        require(count==source.size&&hex(digest.digest())==source.sha256){"Un original cambió durante la copia; se conserva."}
                        zip.closeEntry()
                    }
                };disk.fd.sync()
            }
            val checked=verify(partial,sources,cancel,progress);cancel()
            check(!target.exists()){"Ya existe una copia; no se sobrescribió."}
            try{Files.move(partial.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE)}
            catch(_:AtomicMoveNotSupportedException){Files.move(partial.toPath(),target.toPath())}
            check(target.length()==checked.bytes)
            progress(Progress("READY",sum,sum))
            return checked.copy(file=target)
        } finally {
            // Only the temporary allocated by THIS attempt, never sources or prior deliveries.
            if(partial.exists())partial.delete()
        }
    }
}
