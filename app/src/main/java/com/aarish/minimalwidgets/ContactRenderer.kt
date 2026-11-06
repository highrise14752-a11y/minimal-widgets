package com.aarish.minimalwidgets

import android.graphics.*
import kotlin.math.min

/** Original procedural avatars are placeholders; a configured private photo replaces them. */
data class ContactTile(val name:String="Contact",val number:String="",val photo:Bitmap?=null,val telegram:String="")
object ContactRenderer {
    fun preview(design:Design,a:Appearance):Bitmap {
        val wide=design.style==4 || design.style>=11
        val w=if(wide)400 else 200;val h=200
        val tiles=List(contactSlotCount(design.style)){i->ContactTile(when(design.style){7,8->"John Doe";9->"ELON MUSK";11,12,13->"Samira";else->if(i==0)"Arsh" else "Contact ${i+1}"})}
        val source=render(design.style,a,tiles,w,h)
        val out=Bitmap.createBitmap(280,220,Bitmap.Config.ARGB_8888)
        val scale=min(280f/w,220f/h);val bw=w*scale;val bh=h*scale
        Canvas(out).drawBitmap(source,null,RectF((280-bw)/2,(220-bh)/2,(280+bw)/2,(220+bh)/2),Paint(Paint.ANTI_ALIAS_FLAG))
        source.recycle();return out
    }
    fun render(style:Int,a:Appearance,tiles:List<ContactTile>,width:Int,height:Int):Bitmap {
        val bitmap=Bitmap.createBitmap(width.coerceAtLeast(40),height.coerceAtLeast(40),Bitmap.Config.ARGB_8888)
        val c=Canvas(bitmap);val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val w=bitmap.width.toFloat();val h=bitmap.height.toFloat();val unit=min(w,h)
        val fg=a.textColor();val tile=tiles.firstOrNull()?:ContactTile()
        fun rect(x:Float,y:Float,rw:Float,rh:Float,color:Int,r:Float=unit*.1f) {p.color=color;p.style=Paint.Style.FILL;c.drawRoundRect(x,y,x+rw,y+rh,r,r,p)}
        fun text(value:String,x:Float,y:Float,size:Float=unit*.15f,maxWidth:Float=w*.9f,color:Int=fg) {
            p.color=color;p.style=Paint.Style.FILL;p.textSize=size;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.textAlign=Paint.Align.CENTER
            if(p.measureText(value)>maxWidth)p.textSize*=maxWidth/p.measureText(value)
            c.drawText(value,x,y,p)
        }
        fun portrait(item:ContactTile,x:Float,y:Float,rw:Float,rh:Float,index:Int=0,circle:Boolean=false) {
            c.save();val bounds=RectF(x,y,x+rw,y+rh)
            c.clipPath(Path().apply{if(circle)addOval(bounds,Path.Direction.CW)else addRoundRect(bounds,unit*.1f,unit*.1f,Path.Direction.CW)})
            if(item.photo!=null) {
                val photo=item.photo;val scale=maxOf(rw/photo.width,rh/photo.height)
                c.drawBitmap(photo,null,RectF(x+(rw-photo.width*scale)/2,y+(rh-photo.height*scale)/2,x+(rw+photo.width*scale)/2,y+(rh+photo.height*scale)/2),p)
            } else {
                rect(x,y,rw,rh,a.secondary,0f)
                val u=min(rw,rh);val cx=x+rw*.5f;val cy=y+rh*.43f
                val shirts=intArrayOf(0xFF3D3D82.toInt(),0xFF6D60BD.toInt(),0xFF51AEE6.toInt(),0xFFD84743.toInt(),a.accent)
                p.color=shirts[index%shirts.size];c.drawOval(cx-rw*.5f,y+rh*.67f,cx+rw*.6f,y+rh*1.3f,p)
                p.color=0xFFF3AE8F.toInt();c.drawRoundRect(cx-u*.12f,cy+u*.2f,cx+u*.12f,cy+u*.47f,u*.04f,u*.04f,p)
                c.drawOval(cx-u*.23f,cy-u*.27f,cx+u*.24f,cy+u*.3f,p)
                p.color=if(index%3==1)0xFFD87959.toInt()else Color.BLACK
                c.drawOval(cx-u*.3f,cy-u*.4f,cx+u*.23f,cy-u*.03f,p)
                c.drawOval(cx-u*.12f,cy-u*.43f,cx+u*.31f,cy-u*.13f,p)
                p.color=Color.BLACK;p.strokeWidth=u*.016f;p.strokeCap=Paint.Cap.ROUND;p.style=Paint.Style.STROKE
                c.drawLine(cx-u*.1f,cy+u*.02f,cx-u*.08f,cy+u*.09f,p)
                c.drawLine(cx+u*.11f,cy+u*.01f,cx+u*.13f,cy+u*.08f,p)
                c.drawArc(cx-u*.06f,cy+u*.12f,cx+u*.11f,cy+u*.22f,5f,150f,false,p)
                if(index%3==0) {
                    p.color=Color.WHITE;p.strokeWidth=u*.014f
                    c.drawCircle(cx-u*.12f,cy-u*.01f,u*.09f,p);c.drawCircle(cx+u*.12f,cy-u*.01f,u*.09f,p)
                    c.drawLine(cx-u*.03f,cy-u*.01f,cx+u*.03f,cy-u*.01f,p)
                }
                p.style=Paint.Style.FILL
            };c.restore()
        }
        fun icon(kind:String,x:Float,y:Float,size:Float,color:Int) {
            c.save();c.translate(x,y);c.scale(size/24,size/24);p.color=color;p.style=Paint.Style.FILL
            fun phone() {c.drawPath(Path().apply{moveTo(7f,2f);lineTo(11f,7f);lineTo(8f,10f);cubicTo(10f,14f,12f,16f,16f,17f);lineTo(19f,14f);lineTo(24f,18f);lineTo(22f,23f);cubicTo(10f,26f,-2f,9f,2f,3f);close()},p)}
            fun bubble() {c.drawOval(1f,1f,23f,20f,p);c.drawPath(Path().apply{moveTo(16f,15f);lineTo(14f,24f);lineTo(22f,16f);close()},p)}
            when(kind){
                "call"->phone()
                "telegram"->{p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.strokeJoin=Paint.Join.ROUND;c.drawPath(Path().apply{moveTo(1f,9f);lineTo(23f,1f);lineTo(18f,23f);lineTo(10f,17f);lineTo(7f,21f);lineTo(7f,14f);lineTo(18f,6f);lineTo(7f,14f);close()},p)}
                "whatsapp"->{bubble();p.color=a.background;c.save();c.translate(6f,4f);c.scale(.55f,.55f);phone();c.restore()}
                else->bubble()
            };p.style=Paint.Style.FILL;c.restore()
        }
        val round=unit*.12f
        if(style!=8 && style!=9)rect(0f,0f,w,h,a.background,round)
        when(style){
            1,2->{
                val ph=if(style==1)h*.75f else h*.78f
                portrait(tile,0f,0f,w,ph,if(style==1)0 else 1)
                if(style==1)text(tile.name,w/2,h*.91f,unit*.16f)
                else {
                    text(tile.name,w/2,h*.74f,unit*.15f)
                    rect(w*.13f,h*.83f,w*.29f,h*.13f,a.accent,h*.065f)
                    rect(w*.58f,h*.83f,w*.29f,h*.13f,fg,h*.065f)
                    icon("call",w*.235f,h*.85f,h*.09f,a.background)
                    icon("message",w*.685f,h*.85f,h*.09f,a.background)
                }
            }
            3,4->{
                val cols=if(style==4 && w>h*1.35f)4 else 2
                val rows=if(style==4)8/cols else 2
                val cw=w/cols;val ch=h/rows;val diameter=min(cw,ch)*.76f
                repeat(contactSlotCount(style)){i->val cx=(i%cols+.5f)*cw;val cy=(i/cols+.5f)*ch
                    portrait(tiles.getOrElse(i){ContactTile()},cx-diameter/2,cy-diameter/2,diameter,diameter,i,true)}
            }
            5,6->{portrait(tile,0f,0f,w,h,if(style==5)2 else 3);icon(if(style==5)"call" else "message",if(style==5)w*.07f else w*.79f,h*.07f,unit*.14f,a.accent)}
            7,8->{
                val gap=unit*.035f;val pad=if(style==7)unit*.04f else 0f
                rect(pad,pad,w-2*pad,h*.49f-pad,if(style==7)a.secondary else a.background,unit*.08f)
                text(tile.name,w/2,h*.31f,unit*.16f,maxWidth=w*.85f)
                rect(pad,h*.51f,(w-gap)/2-pad,h*.49f-pad,if(style==7)a.accent else a.background,unit*.09f)
                rect((w+gap)/2,h*.51f,(w-gap)/2-pad,h*.49f-pad,if(style==7)a.secondary else a.accent,unit*.09f)
                icon("call",w*.15f,h*.66f,unit*.2f,if(style==7)a.background else fg)
                icon("message",w*.65f,h*.66f,unit*.2f,if(style==7)fg else a.background)
            }
            9->{
                rect(0f,0f,w*.48f,h,a.background,unit*.1f)
                rect(w*.52f,0f,w*.48f,h*.48f,a.background,unit*.1f)
                rect(w*.52f,h*.52f,w*.48f,h*.48f,a.accent,unit*.1f)
                val words=tile.name.uppercase().split(' ',limit=2)
                text(words[0],w*.24f,h*.48f,unit*.15f,w*.45f)
                if(words.size>1)text(words[1],w*.24f,h*.62f,unit*.15f,w*.45f)
                icon("message",w*.65f,h*.14f,unit*.23f,fg)
                icon("call",w*.65f,h*.66f,unit*.23f,a.background)
            }
            10->{
                text(tile.name.uppercase(),w/2,h*.44f,unit*.17f)
                rect(w*.05f,h*.72f,w*.43f,h*.22f,a.accent,unit*.06f)
                rect(w*.52f,h*.72f,w*.43f,h*.22f,a.accent,unit*.06f)
                icon("message",w*.205f,h*.765f,unit*.14f,a.background)
                icon("call",w*.675f,h*.765f,unit*.14f,a.background)
            }
            else->{
                val kind=when(style){11->"call";12->"message";13->"telegram";else->"whatsapp"}
                if(w>h*1.25f){icon(kind,w*.10f,h*.29f,h*.4f,a.accent);text(tile.name,w*.62f,h*.64f,h*.34f,maxWidth=w*.63f)}
                else {icon(kind,w*.35f,h*.12f,unit*.3f,a.accent);text(tile.name,w/2,h*.78f,unit*.19f)}
            }
        }
        return bitmap
    }
}
