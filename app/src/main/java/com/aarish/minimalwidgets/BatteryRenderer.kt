package com.aarish.minimalwidgets

import android.graphics.*
import kotlin.math.min
import kotlin.math.roundToInt

/** Draw in the host's actual aspect ratio; never squeeze a square preview into a strip. */
object BatteryRenderer {
    fun render(d: Design, a: Appearance, level: Int, charging: Boolean, width: Float, height: Float): Bitmap {
        val w=(width*2).roundToInt().coerceIn(40,1600)
        val h=(height*2).roundToInt().coerceIn(40,1600)
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val c=Canvas(output)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val fw=w.toFloat(); val fh=h.toFloat(); val unit=min(fw,fh)
        if(d.style==8) {
            // Paint only the battery so wallpaper is visible around its silhouette.
            val bodyHeight=min(fh*.84f,fw*.78f*2.1f)
            val bodyWidth=bodyHeight/2.1f
            val capHeight=bodyHeight*.06f
            val left=(fw-bodyWidth)/2f
            val top=(fh-bodyHeight-capHeight)/2f+capHeight
            val body=RectF(left,top,left+bodyWidth,top+bodyHeight)
            val radius=bodyWidth*.24f
            p.color=a.accent
            c.drawRoundRect(fw/2-bodyWidth*.2f,top-capHeight,fw/2+bodyWidth*.2f,top+capHeight,capHeight,capHeight,p)
            p.color=a.background
            c.drawRoundRect(body,radius,radius,p)
            val fraction=(level/100f).coerceIn(0f,1f)
            if(fraction>0f) {
                c.save()
                c.clipRect(body.left,body.bottom-body.height()*fraction,body.right,body.bottom)
                p.color=a.accent
                c.drawRoundRect(body,radius,radius,p)
                c.restore()
            }
            return output
        }
        p.color=a.background
        c.drawRoundRect(0f,0f,fw,fh,unit*.12f,unit*.12f,p)
        val f=(level/100f).coerceIn(0f,1f)
        val label=if(level<0) "—" else "$level%"
        fun text(value:String,x:Float,y:Float,size:Float,color:Int=a.textColor()) {
            p.color=color;p.style=Paint.Style.FILL;p.textSize=size
            p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.textAlign=Paint.Align.CENTER
            c.drawText(value,x,y,p)
        }
        fun bar(x:Float,y:Float,bw:Float,bh:Float) {
            p.style=Paint.Style.FILL;p.color=a.accentDim();c.drawRoundRect(x,y,x+bw,y+bh,bh/2,bh/2,p)
            if(f>0){p.color=a.accent;c.drawRoundRect(x,y,x+bw*f,y+bh,bh/2,bh/2,p)}
        }
        if(fw>fh*1.35f) {
            when(d.style) {
                4 -> {
                    val r=unit*.3f;val cx=fw*.23f;val cy=fh*.5f
                    p.style=Paint.Style.STROKE;p.strokeWidth=unit*.075f;p.color=a.accentDim();c.drawCircle(cx,cy,r,p)
                    p.color=a.accent;c.drawArc(cx-r,cy-r,cx+r,cy+r,-90f,360*f,false,p)
                    text(label,fw*.67f,fh*.57f,unit*.38f)
                }
                9 -> bar(fw*.1f,fh*.22f,fw*.8f,fh*.56f)
                17 -> {
                    text(label,fw*.18f,fh*.28f,unit*.23f)
                    val cols=20; val rows=5;val spacing=min(fw*.84f/cols,fh*.5f/rows)
                    repeat(rows){row->repeat(cols){col->p.color=if(row*cols+col<rows*cols*f)a.accent else a.secondary
                        c.drawCircle(fw*.08f+(col+.5f)*fw*.84f/cols,fh*.38f+(row+.5f)*spacing,spacing*.42f,p)}}
                }
                else -> {
                    text(label,fw*.5f,fh*.42f,unit*.32f)
                    text(if(charging) "Charging" else "Battery",fw*.5f,fh*.64f,unit*.16f)
                    bar(fw*.1f,fh*.79f,fw*.8f,fh*.055f)
                }
            }
        } else {
            val preview=DesignRenderer.render(d,a,level,charging)
            val scale=min(fw/240f,fh/200f)
            val bw=240*scale;val bh=200*scale
            c.drawBitmap(preview,Rect(20,10,260,210),RectF((fw-bw)/2,(fh-bh)/2,(fw+bw)/2,(fh+bh)/2),p)
            preview.recycle()
        }
        return output
    }
}
