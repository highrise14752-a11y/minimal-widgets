package com.aarish.minimalwidgets

import android.graphics.*
import kotlin.math.*

/** One vector renderer for gallery previews and real widgets. Coordinates never stretch circles. */
object BatteryRenderer {
    fun timeLabel(level: Int, charging: Boolean, remainingMillis: Long?): String {
        if(level<0) return "Battery unavailable"
        if(level>=100) return "Fully charged"
        if(!charging) return "Discharging"
        if(remainingMillis==null || remainingMillis<=0) return "Charging"
        val minutes=ceil(remainingMillis/60000.0).toLong()
        val duration=if(minutes>=60) "${minutes/60} h ${minutes%60} m" else "$minutes m"
        return "$duration until full"
    }

    fun render(d: Design, a: Appearance, level: Int, charging: Boolean, width: Float, height: Float,
               remainingMillis: Long? = null): Bitmap {
        val bitmap=Bitmap.createBitmap((width*2).roundToInt().coerceIn(40,1600),
            (height*2).roundToInt().coerceIn(40,1600),Bitmap.Config.ARGB_8888)
        val c=Canvas(bitmap)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val n=d.style
        val f=if(level<0)0f else level.coerceIn(0,100)/100f
        val percent=if(level<0) "\u2014" else "${level.coerceIn(0,100)}%"
        val status=if(level<0) "Unavailable" else if(level>=100) "Fully charged" else if(charging) "Charging" else "Discharging"
        val fg=a.textColor()
        val track=Color.rgb((Color.red(a.accent)*.24f).toInt(),(Color.green(a.accent)*.24f).toInt(),(Color.blue(a.accent)*.24f).toInt())
        val muted=Color.rgb((Color.red(fg)*.36f+Color.red(a.background)*.64f).toInt(),
            (Color.green(fg)*.36f+Color.green(a.background)*.64f).toInt(),
            (Color.blue(fg)*.36f+Color.blue(a.background)*.64f).toInt())
        val cw=if(n in listOf(4,9,11,17))400f else 200f
        val ch=200f
        // Battery 8 stays a silhouette at every host aspect ratio.
        val scale=min(bitmap.width/cw,bitmap.height/ch)
        if(n!=8) {
            c.translate((bitmap.width-cw*scale)/2,(bitmap.height-ch*scale)/2)
            c.scale(scale,scale)
        }
        fun rect(x:Float,y:Float,w:Float,h:Float,color:Int,r:Float=20f) {
            p.color=color;p.style=Paint.Style.FILL
            c.drawRoundRect(x,y,x+w,y+h,r,r,p)
        }
        fun text(value:String,x:Float,y:Float,size:Float,color:Int=fg,bold:Boolean=true,
                 align:Paint.Align=Paint.Align.CENTER,italic:Boolean=false,maxWidth:Float=cw-24f) {
            p.color=color;p.style=Paint.Style.FILL;p.textAlign=align
            p.typeface=Typeface.create("sans-serif",if(italic)Typeface.BOLD_ITALIC else if(bold)Typeface.BOLD else Typeface.NORMAL)
            p.textSize=size
            val measured=p.measureText(value)
            if(measured>maxWidth)p.textSize=size*maxWidth/measured
            c.drawText(value,x,y,p)
        }
        fun ring(x:Float,y:Float,r:Float,stroke:Float,fraction:Float=f) {
            p.style=Paint.Style.STROKE;p.strokeWidth=stroke;p.strokeCap=Paint.Cap.BUTT;p.color=a.accentDim()
            c.drawCircle(x,y,r,p)
            p.color=a.accent
            if(fraction>0)c.drawArc(x-r,y-r,x+r,y+r,-90f,360f*fraction,false,p)
            p.style=Paint.Style.FILL
        }
        fun pill(x:Float,y:Float,w:Float,h:Float,fraction:Float=f,color:Int=a.accent,track:Int=a.accentDim(),inset:Float=0f,radius:Float=h/2) {
            rect(x,y,w,h,track,radius)
            if(fraction<=0)return
            c.save();c.clipPath(Path().apply{addRoundRect(RectF(x+inset,y+inset,x+w-inset,y+h-inset),(radius-inset).coerceAtLeast(0f),(radius-inset).coerceAtLeast(0f),Path.Direction.CW)})
            p.color=color
            if(inset>0) c.drawRoundRect(x+inset,y+inset,x+inset+(w-2*inset)*fraction,y+h-inset,(h-2*inset)/2,(h-2*inset)/2,p)
            else c.drawRect(x,y,x+w*fraction,y+h,p)
            c.restore()
        }
        fun vertical(x:Float,y:Float,w:Float,h:Float) {
            rect(x,y,w,h,a.accentDim(),w/2)
            c.save();c.clipRect(x,y+h*(1-f),x+w,y+h)
            rect(x,y,w,h,a.accent,w/2);c.restore()
        }
        fun bolt(x:Float,y:Float,size:Float,color:Int=a.accent) {
            val path=Path().apply {
                moveTo(x+size*.57f,y);lineTo(x+size*.12f,y+size*.58f)
                lineTo(x+size*.46f,y+size*.58f);lineTo(x+size*.38f,y+size)
                lineTo(x+size*.88f,y+size*.4f);lineTo(x+size*.55f,y+size*.4f);close()
            };p.color=color;p.style=Paint.Style.FILL;c.drawPath(path,p)
        }
        fun batteryIcon(x:Float,y:Float,w:Float,h:Float,color:Int=fg) {
            rect(x+w*.33f,y,w*.34f,h*.1f,color,2f)
            rect(x,y+h*.07f,w,h*.93f,color,w*.16f)
        }
        fun dots(cols:Int,rows:Int,x:Float,y:Float,w:Float,h:Float,color:Int=a.accent,radiusRatio:Float=.47f) {
            val dx=w/cols;val dy=h/rows;val r=min(dx,dy)*radiusRatio
            val active=floor(f*cols*rows+.0001f).toInt()
            repeat(rows){row->repeat(cols){col->p.color=if(row*cols+col<active)color else a.secondary
                c.drawCircle(x+(col+.5f)*dx,y+(row+.5f)*dy,r,p)}}
        }
        if(n!=8 && n!=9)rect(0f,0f,cw,ch,a.background,24f)
        when(n) {
            1 -> {
                ring(100f,88f,55f,13f)
                text(percent,100f,100f,34f)
                text(timeLabel(level,charging,remainingMillis),100f,185f,11f,bold=false,maxWidth=174f)
            }
            2 -> {
                text("Power",100f,37f,16f,bold=false)
                text(percent,100f,99f,58f,a.accent)
                text(timeLabel(level,charging,remainingMillis),100f,130f,13f,bold=false,maxWidth=174f)
                pill(18f,151f,164f,37f,inset=5f)
            }
            3 -> {
                text(status.lowercase(),100f,33f,18f,bold=false)
                text(percent,100f,59f,24f,a.accent)
                val cx=100f;val cy=160f;val radius=64f
                fun face(color:Int) {
                    p.color=color;p.style=Paint.Style.STROKE;p.strokeWidth=11f;p.strokeCap=Paint.Cap.ROUND
                    c.drawCircle(cx,cy,radius,p)
                    c.drawArc(cx-30,cy-5,cx+30,cy+33,12f,156f,false,p)
                    p.style=Paint.Style.FILL;c.drawCircle(cx-23,cy-10,7f,p);c.drawCircle(cx+23,cy-10,7f,p)
                }
                c.save();c.clipRect(0f,0f,200f,200f);face(a.accentDim())
                c.clipRect(0f,cy+radius-2*radius*f,200f,cy+radius);face(a.accent);c.restore()
            }
            4 -> {
                ring(71f,100f,59f,17f)
                batteryIcon(55f,76f,31f,48f)
                text(percent,262f,110f,68f,maxWidth=220f)
                text("BATTERY",262f,145f,26f,muted,bold=false)
            }
            5 -> {
                text(percent,100f,49f,42f)
                text("BATTERY",100f,68f,12f,muted)
                pill(15f,91f,170f,93f,radius=26f)
            }
            6 -> dots(5,5,20f,18f,160f,160f)
            7 -> {
                text(percent,20f,48f,35f,a.accent,align=Paint.Align.LEFT,maxWidth=160f)
                text(status.lowercase(),20f,70f,19f,bold=false,align=Paint.Align.LEFT,maxWidth=160f)
                val heart=Path().apply {
                    moveTo(137f,180f);cubicTo(115f,162f,89f,141f,91f,117f)
                    cubicTo(93f,90f,123f,92f,137f,108f)
                    cubicTo(151f,92f,181f,90f,183f,117f)
                    cubicTo(185f,141f,159f,162f,137f,180f);close()
                }
                p.color=a.accentDim();c.drawPath(heart,p)
                c.save();c.clipRect(88f,180f-84f*f,187f,184f);p.color=a.accent;c.drawPath(heart,p);c.restore()
            }
            8 -> {
                // No panel behind this vertical battery, including after resizing.
                val fw=bitmap.width.toFloat();val fh=bitmap.height.toFloat()
                val bh=min(fh*.84f,fw*.78f*2.1f);val bw=bh/2.1f;val cap=bh*.06f
                val x=(fw-bw)/2;val y=(fh-bh-cap)/2+cap
                rect(fw/2-bw*.2f,y-cap,bw*.4f,cap*2,a.accent,cap)
                rect(x,y,bw,bh,a.background,bw*.24f)
                c.save();c.clipRect(x,y+bh*(1-f),x+bw,y+bh)
                rect(x,y,bw,bh,a.accent,bw*.24f);c.restore()
            }
            9 -> {
                rect(354f,72f,25f,56f,a.accent,14f)
                rect(18f,16f,344f,168f,a.background,36f)
                c.save();c.clipRect(18f,16f,18f+344f*f,184f)
                rect(18f,16f,344f,168f,a.accent,36f);c.restore()
            }
            10 -> {
                batteryIcon(20f,14f,23f,33f,a.accent)
                text(percent,18f,166f,32f,a.accent,align=Paint.Align.LEFT,maxWidth=84f)
                text(status.lowercase(),18f,185f,17f,muted,bold=false,align=Paint.Align.LEFT,maxWidth=84f)
                rect(109f,10f,80f,180f,track,16f)
                repeat(5){i->
                    val x=118f;val y=21f+i*32f;val fraction=(f*5-(4-i)).coerceIn(0f,1f)
                    rect(x,y,62f,24f,a.accentDim(),6f)
                    c.save();c.clipRect(x,y,x+62f*fraction,y+24f)
                    rect(x,y,62f,24f,a.accent,6f);c.restore()
                }
            }
            11 -> {
                text(percent,200f,68f,40f)
                text(status,200f,112f,34f,muted,bold=false)
                pill(40f,150f,320f,10f)
            }
            12 -> {
                bolt(82f,14f,38f)
                text("$percent $status",100f,80f,16f,maxWidth=176f)
                rect(18f,99f,164f,88f,track,17f)
                repeat(5){i->val x=29f+i*29f;val fraction=(f*5-i).coerceIn(0f,1f)
                    rect(x,110f,22f,66f,a.accentDim(),6f)
                    c.save();c.clipRect(x,176f-66f*fraction,x+22f,176f)
                    rect(x,110f,22f,66f,a.accent,6f);c.restore()
                }
            }
            13 -> {
                bolt(19f,19f,25f)
                text(percent,19f,144f,49f,align=Paint.Align.LEFT,italic=true,maxWidth=139f)
                text(status,20f,164f,14f,bold=false,align=Paint.Align.LEFT,maxWidth=137f)
                text(timeLabel(level,charging,remainingMillis),20f,184f,10f,bold=false,align=Paint.Align.LEFT,maxWidth=138f)
                vertical(169f,18f,10f,166f)
            }
            14,15 -> {
                vertical(18f,18f,25f,166f)
                text("BATTERY",117f,39f,24f,maxWidth=140f)
                if(n==14)text(status.lowercase(),117f,60f,16f,muted,bold=false,maxWidth=138f)
                text(percent,117f,177f,51f,italic=true,maxWidth=140f)
            }
            16 -> dots(10,10,16f,16f,168f,168f)
            17 -> {
                text(percent,29f,49f,27f,align=Paint.Align.LEFT)
                text(status,29f,68f,15f,bold=false,align=Paint.Align.LEFT)
                dots(20,5,27f,91f,346f,85f)
            }
            18 -> {
                bolt(83f,16f,40f)
                text(percent,100f,119f,53f)
                dots(6,1,18f,143f,164f,28f)
            }
            19 -> {
                text("Battery",18f,36f,19f,align=Paint.Align.LEFT,maxWidth=99f)
                text(percent,182f,36f,24f,align=Paint.Align.RIGHT,italic=true,maxWidth=70f)
                dots(7,5,16f,55f,168f,125f,fg,.25f)
            }
        }
        return bitmap
    }
}
