package com.tuempresa.inventariovial.core

/** Locate EOI structurally, skipping APP/EXIF payloads and escaped entropy bytes. */
object JpegEnvelope {
    fun end(bytes:ByteArray):Int? {
        fun u(n:Int)=bytes[n].toInt() and 255
        if(bytes.size<4 || u(0)!=255 || u(1)!=216)return null
        var at=2;var scan=false;var frame=false;var sawScan=false
        while(at<bytes.size) {
            if(scan) {
                while(at<bytes.size && u(at)!=255)at++
            }
            if(at>=bytes.size || u(at++)!=255)return null
            while(at<bytes.size && u(at)==255)at++
            if(at>=bytes.size)return null
            val marker=u(at++)
            if(scan && (marker==0 || marker in 208..215))continue
            scan=false
            if(marker==217)return at.takeIf {frame && sawScan}
            if(marker==216 || marker==0)return null
            if(marker==1)continue
            if(at+1>=bytes.size)return null
            val size=u(at)*256+u(at+1)
            if(size<2 || size>bytes.size-at)return null
            if(marker in 192..207 && marker !in setOf(196,200,204))frame=true
            at+=size
            if(marker==218){scan=true;sawScan=true}
        }
        return null
    }
    fun valid(bytes:ByteArray)=end(bytes)==bytes.size
}
