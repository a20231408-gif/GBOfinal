package com.tuempresa.inventariovial.pavement.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.tuempresa.inventariovial.pavement.*
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.Color
import kotlin.math.*

internal object PavementDrawing {
    fun draw(c: NativeCanvas, s: PavementSampleEntity, damages: List<PavementDamageEntity>, scale: Float, selected: Set<Int> = emptySet(), grid: Double = 1.0, highlightedDamageId: String? = null, labels: Boolean = true) {
        val w = s.anchoM.toFloat()*scale; val h = s.largoM.toFloat()*scale
        val transform = PavementScale(s.largoM, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth=1f; textSize=13f }
        paint.color=Color.WHITE; c.drawRect(0f,0f,w,h,paint)
        val rigid=s.surface=="RIGID" && s.slabRows>0 && s.slabColumns>0
        val cols=if(rigid)s.slabColumns else ceil(s.anchoM/grid).toInt().coerceAtMost(1000)
        val rows=if(rigid)s.slabRows else ceil(s.largoM/grid).toInt().coerceAtMost(1000)
        val dx=if(rigid)w/cols else (grid*scale).toFloat(); val dy=if(rigid)h/rows else (grid*scale).toFloat()
        paint.color=Color.LTGRAY
        for(i in 0..cols) c.drawLine(i*dx,0f,i*dx,h,paint)
        for(i in 0..rows) c.drawLine(0f,transform.canvasY(if(rigid) i*s.largoM/rows else i*grid),w,transform.canvasY(if(rigid) i*s.largoM/rows else i*grid),paint)
        fun slab(n:Int,label:String,color:Int) {
            if(!rigid || n !in 1..(rows*cols)) return
            val col=(n-1)%cols; val row=(n-1)/cols
            paint.color=color; paint.style=Paint.Style.STROKE; c.drawRect(col*dx,transform.canvasY((row+1)*s.largoM/rows),(col+1)*dx,transform.canvasY(row*s.largoM/rows),paint); paint.style=Paint.Style.FILL
            c.drawText(label,col*dx+2,transform.canvasY(row*s.largoM/rows)-4,paint)
        }
        if(rigid) for(n in 1..rows*cols) slab(n,n.toString(),Color.DKGRAY)
        fun tag(text:String,left:Float,baseline:Float,color:Int) {
            paint.style=Paint.Style.FILL;paint.color=color
            c.drawRect(left-2,baseline-13,left+paint.measureText(text)+3,baseline+3,paint)
            paint.color=if(color==PavementSeverity.color("H"))Color.WHITE else Color.BLACK
            c.drawText(text,left,baseline,paint);paint.color=color;paint.style=Paint.Style.STROKE
        }
        paint.strokeWidth=2f
        for(d in damages) {
            val color=PavementSeverity.color(d.severity)
            paint.color=color
            paint.strokeWidth=if(d.id==highlightedDamageId) 5f else 2f
            val x=((d.x1?:0.0)*scale).toFloat();val y=transform.canvasY(d.y1?:0.0)
            val x2=((d.x2?:0.0)*scale).toFloat();val y2=transform.canvasY(d.y2?:0.0)
            paint.style=Paint.Style.STROKE
            fun recipe(left: Float, top: Float, right: Float, bottom: Float, kind: PavementSketchKind) {
                val ww=right-left;val hh=bottom-top
                fun path(points: List<Pair<Float,Float>>,close: Boolean=false) {
                    val p=android.graphics.Path();points.forEachIndexed { i,pt -> if(i==0)p.moveTo(pt.first,pt.second)else p.lineTo(pt.first,pt.second) };if(close)p.close();c.drawPath(p,paint)
                }
                fun irregular(a: SketchPoint,b: SketchPoint,key: String=d.id) {
                    path(PavementSketch.crack(key,a,b,s.anchoM,s.largoM).map { (it.x*scale).toFloat() to transform.canvasY(it.y) })
                }
                when(kind) {
                    PavementSketchKind.IRREGULAR_LINE -> irregular(SketchPoint(left/scale.toDouble(),transform.metresY(top)),SketchPoint(right/scale.toDouble(),transform.metresY(bottom)))
                    PavementSketchKind.LINE -> if(d.code==27)c.drawLine(right,top,right,bottom,paint)else c.drawLine(left,top,right,bottom,paint)
                    PavementSketchKind.CORNER -> irregular(SketchPoint(left/scale.toDouble(),transform.metresY(top+hh*.35f)),SketchPoint((left+ww*.35f)/scale.toDouble(),transform.metresY(top)),d.id+"/corner")
                    PavementSketchKind.JOINT -> { c.drawLine(left,top,right,top,paint);c.drawLine(left,top+min(hh*.12f,6f),right,top+min(hh*.12f,6f),paint) }
                    PavementSketchKind.POINT -> {
                        val p=android.graphics.Path();val cx=(left+right)/2;val cy=(top+bottom)/2
                        for(i in 0..16) { val angle=2*PI*i/16;val radius=if(i%2==0)1.0 else .88;val px=cx+ww/2*cos(angle).toFloat()*radius.toFloat();val py=cy+hh/2*sin(angle).toFloat()*radius.toFloat();if(i==0)p.moveTo(px,py)else p.lineTo(px,py) };p.close();c.drawPath(p,paint)
                    }
                    PavementSketchKind.PARABOLIC -> { val p=android.graphics.Path();p.moveTo(left,top);p.quadTo((left+right)/2,bottom+hh,right,top);c.drawPath(p,paint) }
                    PavementSketchKind.NETWORK -> {
                        c.drawRect(left,top,right,bottom,paint)
                        val cells=if(d.code==3)3 else 5
                        for(i in 1 until cells) {
                            val yy=top+hh*i/cells;val xx=left+ww*i/cells
                            irregular(SketchPoint(left/scale.toDouble(),transform.metresY(yy)),SketchPoint(right/scale.toDouble(),transform.metresY(yy)),"${d.id}/h$i")
                            irregular(SketchPoint(xx/scale.toDouble(),transform.metresY(top)),SketchPoint(xx/scale.toDouble(),transform.metresY(bottom)),"${d.id}/v$i")
                        }
                    }
                    PavementSketchKind.TRANSVERSE_PATTERN -> {
                        if(d.code==21) { val p=android.graphics.Path();p.moveTo(left,top+hh*.12f);p.quadTo((left+right)/2,top-hh*.12f,right,top+hh*.12f);c.drawPath(p,paint) }
                        else if(d.code in listOf(14,35)) { for(i in 1..2)c.drawLine(left,top+hh*i/3,right,top+hh*i/3,paint);for(i in 1..6)c.drawLine(left+ww*i/7,top+hh*.2f,left+ww*i/7,bottom-hh*.2f,paint) }
                        else if(hh==0f || ww==0f) { c.drawLine(left,top,right,bottom,paint);c.drawLine(left,top+3f,right,bottom+3f,paint) }
                        else { c.drawRect(left,top,right,bottom,paint);for(i in 1..5)c.drawLine(left,top+hh*i/6,right,top+hh*i/6,paint) }
                    }
                    PavementSketchKind.LONGITUDINAL_BANDS -> { c.drawRect(left,top,right,bottom,paint);for(i in 1..3)c.drawLine(left+ww*i/4,top,left+ww*i/4,bottom,paint) }
                    PavementSketchKind.PATCH -> { c.drawRect(left,top,right,bottom,paint);for(i in 1..4)c.drawLine(left,top+hh*i/5,left+ww*i/5,bottom,paint) }
                    PavementSketchKind.DEPRESSION, PavementSketchKind.SWELL -> { c.drawOval(left,top,right,bottom,paint);c.drawOval(left+ww*.2f,top+hh*.2f,right-ww*.2f,bottom-hh*.2f,paint);if(kind==PavementSketchKind.SWELL)c.drawLine((left+right)/2,top,(left+right)/2,bottom,paint) }
                    PavementSketchKind.SCATTER -> { c.drawRect(left,top,right,bottom,paint);for(i in 1..12)c.drawCircle(left+ww*((i*37)%97)/97,top+hh*((i*53)%89)/89,2f,paint) }
                    else -> c.drawRect(left,top,right,bottom,paint)
                }
            }
            val kind=PavementSketch.kind(d.code)
            when(d.geometry){
                "LINEAR" -> if(kind==PavementSketchKind.IRREGULAR_LINE) {
                    val p=android.graphics.Path();PavementSketch.crack(d.id,SketchPoint(d.x1!!,d.y1!!),SketchPoint(d.x2!!,d.y2!!),s.anchoM,s.largoM).forEachIndexed { i,pt -> if(i==0)p.moveTo((pt.x*scale).toFloat(),transform.canvasY(pt.y)) else p.lineTo((pt.x*scale).toFloat(),transform.canvasY(pt.y)) };c.drawPath(p,paint)
                } else recipe(x,y,x2,y2,kind)
                "AREA" -> recipe(min(x,x2),min(y,y2),max(x,x2),max(y,y2),kind)
                "POTHOLE" -> { val radius=((d.diameterM?:0.0)*scale/2).toFloat();recipe(x-radius,y-radius,x+radius,y+radius,kind) }
                "SLABS" -> PavementRules.slabIds(d).forEach { n ->
                    if(rigid && n in 1..rows*cols) {
                        val col=(n-1)%cols;val row=(n-1)/cols
                        recipe(col*dx,transform.canvasY((row+1)*s.largoM/rows),(col+1)*dx,transform.canvasY(row*s.largoM/rows),kind)
                        if(labels) tag("#${d.correlativo}",col*dx+3,transform.canvasY(row*s.largoM/rows)-14,color)
                    }
                }
                "GLOBAL" -> if(labels) { paint.style=Paint.Style.FILL;c.drawText("#${d.correlativo} ${d.name} · global",3f,16f+damages.filter{it.geometry=="GLOBAL"}.indexOf(d)*16f,paint);paint.style=Paint.Style.STROKE }
            }
            paint.style=Paint.Style.FILL
            if(labels && d.geometry !in listOf("GLOBAL","SLABS")) tag("#${d.correlativo}",x+3,y-3,color)
        }
        paint.color=Color.BLACK;paint.strokeWidth=2f;paint.style=Paint.Style.STROKE;c.drawRect(0f,0f,w,h,paint)
        // Draw selection last so it remains visible above damage symbols and the sample border.
        paint.strokeWidth=6f
        for(n in selected) slab(n,"✓$n",Color.RED)
    }
}

@Composable internal fun PavementScheme(s:PavementSampleEntity, damages:List<PavementDamageEntity>, selected:Set<Int>, first:Pair<Double,Double>?, enabled:Boolean, moving:PavementDamageEntity?=null, onMove:(PavementDamageEntity)->Unit={}, highlightedDamageId:String?=null, onTap:(Double,Double)->Unit) {
    var zoom by remember(s.id) { mutableFloatStateOf(1f) }; var pan by remember(s.id) { mutableStateOf(Offset.Zero) }; var grid by remember { mutableStateOf(1.0) }
    val tap by rememberUpdatedState(onTap)
    val move by rememberUpdatedState(onMove)
    var dragged by remember(moving) { mutableStateOf<PavementDamageEntity?>(null) }
    val gutter = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
    val labelSize = with(androidx.compose.ui.platform.LocalDensity.current) { 14.sp.toPx() }
    Column {
        Row { TextButton(onClick={zoom=1f;pan=Offset.Zero}){Text("Ajustar")}; TextButton(onClick={grid=if(grid==1.0)0.5 else 1.0}){Text("Malla $grid m")} }
        Canvas(Modifier.fillMaxWidth().weight(1f).heightIn(min=180.dp).testTag("pavement-scheme")
            .pointerInput(s.anchoM,s.largoM,zoom,pan,enabled,moving) { detectTapGestures { p ->
                if(enabled && moving==null && s.anchoM>0 && s.largoM>0) {
                    val viewport=PavementViewport.fit(s.anchoM,s.largoM,size.width.toFloat(),size.height.toFloat(),gutter,zoom,pan.x,pan.y)
                    val scale=viewport.scale
                    val x=viewport.metresX(p.x);val y=viewport.metresY(p.y)
                    if(x.toDouble() in 0.0..s.anchoM && y in 0.0..s.largoM) tap(x.toDouble(),y)
                }
            } }
            .pointerInput(moving,enabled) {
                if(moving!=null && enabled) detectDragGestures(
                    onDragStart={dragged=moving},
                    onDragCancel={dragged=null},
                    onDragEnd={dragged?.let{move(it)};dragged=null},
                    onDrag={change,delta ->
                        change.consume()
                        val scale=PavementViewport.fit(s.anchoM,s.largoM,size.width.toFloat(),size.height.toFloat(),gutter,zoom,pan.x,pan.y).scale.toDouble()
                        dragged=PavementMovement.translate(s,dragged?:moving,delta.x/scale,-delta.y/scale)
                    }
                ) else detectTransformGestures { center, delta, factor, _ ->
                val next=(zoom*factor).coerceIn(1f,8f); val ratio=next/zoom
                val view=PavementViewport.fit(s.anchoM,s.largoM,size.width.toFloat(),size.height.toFloat(),gutter,zoom,pan.x,pan.y)
                val transformed=view.zoomAt(center.x,center.y,ratio,delta.x,delta.y)
                pan=Offset(transformed.originX-gutter,transformed.originY-15f);zoom=next
            } }) {
            if(s.anchoM>0 && s.largoM>0) {
                val viewport=PavementViewport.fit(s.anchoM,s.largoM,size.width.toFloat(),size.height.toFloat(),gutter,zoom,pan.x,pan.y)
                    val scale=viewport.scale
                val c=drawContext.canvas.nativeCanvas;c.save();c.clipRect(0f,0f,size.width,size.height);c.translate(viewport.originX,viewport.originY)
                PavementDrawing.draw(c,s,damages.map{if(it.id==dragged?.id)dragged!! else it},scale,selected,grid,highlightedDamageId)
                first?.let { c.drawCircle((it.first*scale).toFloat(),PavementScale(s.largoM,scale).canvasY(it.second),5f,Paint().apply{color=Color.BLUE}) }
                // Ruler shares the grid's translated frame, including horizontal pan.
                val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.DKGRAY; textSize=labelSize; strokeWidth=2f; textAlign=Paint.Align.RIGHT }
                val transform=PavementScale(s.largoM,scale)
                val step=PavementRuler.labelStep(s.largoM,s.largoM.toFloat()*scale,labelSize*1.8f)
                c.drawLine(0f,0f,0f,transform.canvasY(0.0),paint)
                val low=viewport.metresY(size.height).coerceIn(0.0,s.largoM)
                val high=viewport.metresY(0f).coerceIn(0.0,s.largoM)
                for(d in PavementRuler.ticks(s.largoM).filter { it>=low && it<=high }) {
                    val y=transform.canvasY(d)
                    val endpoint=d==0.0 || d==s.largoM
                    val principal=d%step==0.0 || endpoint
                    c.drawLine(if(principal) -10f else -5f,y,0f,y,paint)
                    val nearEnd= !endpoint && (d*scale<labelSize*1.8f || (s.largoM-d)*scale<labelSize*1.8f)
                    if(endpoint || (principal && !nearEnd)) {
                        val label=if(d%1.0==0.0) "${d.toInt()} m" else "${String.format(java.util.Locale.US,"%.1f",d)} m"
                        c.drawText(label,-14f,y+labelSize/3,paint)
                    }
                }
                c.restore()
            }
        }
    }
}
