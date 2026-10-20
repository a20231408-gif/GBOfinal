package com.tuempresa.inventariovial.pavement

import android.graphics.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import kotlin.math.*

/** Large upright identifiers; color always denotes the existing severity, never a new quantity. */
object PavementRigidPdfScheme {
    private fun paint()=Paint(Paint.ANTI_ALIAS_FLAG).apply{textSize=11f;color=Color.BLACK}
    fun label(d:PavementDamageEntity)="#${d.correlativo} ${d.code}/${PavementSeverity.canonical(d.severity) ?: "-"}"
    fun badgeWidth(d:PavementDamageEntity)=max(58.0,paint().measureText(label(d)).toDouble()+22)
    fun plans(s:PavementSampleEntity,damages:List<PavementDamageEntity>)=PavementRigidPdfLayout.panels(s,damages,::badgeWidth)
    /** Returns the baseline for the explanatory line/table below this chart. */
    fun draw(c:Canvas,s:PavementSampleEntity,p:PavementRigidPdfLayout.Panel,left:Float,bandTop:Float):Float {
        val r=p.region;val top=bandTop+30;val bottom=top+r.height.toFloat();val width=r.width.toFloat();val scale=r.scale.toFloat()
        val paint=paint()
        c.save();c.clipRect(left,top,left+width,bottom)
        c.translate(left+((s.largoM-r.y0)*r.scale).toFloat(),top-(r.x0*r.scale).toFloat());c.rotate(90f)
        PavementDrawing.draw(c,s,p.damages,scale,labels=false,slabLabels=false);c.restore()
        paint.style=Paint.Style.STROKE;paint.strokeWidth=1f;c.drawRect(left,top,left+width,bottom,paint);paint.style=Paint.Style.FILL
        fun text(value:String,x:Float,y:Float){paint.color=Color.BLACK;paint.style=Paint.Style.FILL;c.drawText(value,x,y,paint)}
        // Absolute axes retain the original frame even when a short specimen uses transverse strips.
        paint.textSize=9f
        val step=PavementRuler.labelStep(r.y1-r.y0,width,44f)
        val ticks=(PavementRuler.ticks(s.largoM).filter{it>r.y0 && it<r.y1 && it%step==0.0}+listOf(r.y0,r.y1)).distinct().sorted()
        for(y in ticks) {
            val x=left+((y-r.y0)*r.scale).toFloat()
            if(y!=r.y0 && y!=r.y1 && min(x-left,left+width-x)<30)continue
            c.drawLine(x,bottom,x,bottom+4,paint);text(fmt(y),x-paint.measureText(fmt(y))/2,bottom+15)
        }
        text("X=${fmt(r.x0)}",left-40,top+5);text(fmt(r.x1)+" m",left-40,bottom)
        val slabW=s.anchoM/s.slabColumns;val slabH=s.largoM/s.slabRows
        for(row in 0 until s.slabRows)for(col in 0 until s.slabColumns) {
            val a=max(r.y0,row*slabH);val b=min(r.y1,(row+1)*slabH)
            val x0=max(r.x0,col*slabW);val x1=min(r.x1,(col+1)*slabW)
            if(b-a>1e-8 && x1-x0>1e-8) {
                val pt=r.project(SketchPoint(x0+(x1-x0)*.9,a+(b-a)*.88))
                val label="L${row*s.slabColumns+col+1}";val x=(left+pt.x).toFloat()-paint.measureText(label)/2;val y=(top+pt.y).toFloat()
                paint.color=Color.WHITE;c.drawRect(x-2,y-9,x+paint.measureText(label)+2,y+2,paint);text(label,x,y)
            }
        }
        if(p.damages.isEmpty()) {
            paint.textSize=12f;paint.color=Color.DKGRAY;paint.textAlign=Paint.Align.CENTER
            c.drawText("Sin daños registrados en esta vista",left+width/2,top+r.height.toFloat()/2,paint)
            paint.textAlign=Paint.Align.LEFT
        }
        for(g in p.groups) {
            val badgeY=if(g.top)bandTop else bottom+26
            g.anchor?.let {a->
                val points=g.refs.mapNotNull{it.point};val x=left+a.x.toFloat()
                val y=top+a.y.toFloat();val edge=if(g.top)top else bottom
                paint.color=Color.rgb(145,155,165);paint.strokeWidth=.7f;paint.style=Paint.Style.STROKE
                // One vertical trunk per group inside its nearest half of the drawing.
                c.drawLine(x,y,x,edge,paint)
                c.drawLine(x,edge,(left+g.center).toFloat(),if(g.top)badgeY+22 else badgeY,paint)
                if(points.size==1)c.drawCircle(x,y,3.2f,paint)
                else {
                    paint.pathEffect=DashPathEffect(floatArrayOf(3f,2f),0f)
                    c.drawRect((left+points.minOf{it.x}-4).toFloat().coerceAtLeast(left),(top+points.minOf{it.y}-4).toFloat().coerceAtLeast(top),
                        (left+points.maxOf{it.x}+4).toFloat().coerceAtMost(left+width),(top+points.maxOf{it.y}+4).toFloat().coerceAtMost(bottom),paint)
                    paint.pathEffect=null
                }
            }
            var x=left+g.left.toFloat()
            for(ref in g.refs) {
                val color=PavementSeverity.color(ref.damage.severity);val w=ref.width.toFloat()
                paint.style=Paint.Style.FILL;paint.color=Color.WHITE;c.drawRoundRect(x,badgeY,x+w,badgeY+22,3f,3f,paint)
                paint.style=Paint.Style.STROKE;paint.color=color;paint.strokeWidth=1.5f;c.drawRoundRect(x,badgeY,x+w,badgeY+22,3f,3f,paint)
                paint.style=Paint.Style.FILL;c.drawRect(x+2,badgeY+3,x+5,badgeY+19,paint)
                paint.textSize=11f;text(label(ref.damage),x+9,badgeY+15);x+=w+4
            }
        }
        // The outer frame and callout leaders must not hide the shoulder tick.
        c.save();c.clipRect(left,top,left+width,bottom)
        c.translate(left+((s.largoM-r.y0)*r.scale).toFloat(),top-(r.x0*r.scale).toFloat());c.rotate(90f)
        PavementDrawing.drawShoulderMarkers(c,s,p.damages,scale);c.restore()
        return bottom+65
    }
    private fun fmt(n:Double)=String.format(java.util.Locale.US,"%.2f",n).trimEnd('0').trimEnd('.')
}
