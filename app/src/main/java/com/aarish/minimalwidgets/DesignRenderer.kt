package com.aarish.minimalwidgets

import android.graphics.*
import kotlin.math.*

/** Original, code-drawn previews. No artwork or fonts are extracted from the reference app. */
object DesignRenderer {
    fun render(d: Design, a: Appearance, level: Int = 60, charging: Boolean = true): Bitmap {
        if(d.category=="Contacts") return ContactRenderer.preview(d,a)
        if(d.category=="Battery") return BatteryRenderer.render(d,a,level,charging,140f,110f)
        val bitmap = Bitmap.createBitmap(280, 220, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val fg = a.textColor()
        fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 22f) {
            p.color = color; p.style = Paint.Style.FILL
            c.drawRoundRect(x, y, x+w, y+h, radius, radius, p)
        }
        fun dot(x: Float, y: Float, r: Float, color: Int) { p.color=color; p.style=Paint.Style.FILL; c.drawCircle(x,y,r,p) }
        fun text(t: String, x: Float, y: Float, size: Float, color: Int = fg, bold: Boolean = true, family: String = "sans-serif") {
            p.color=color; p.style=Paint.Style.FILL; p.textSize=size; p.typeface=Typeface.create(family, if(bold) Typeface.BOLD else Typeface.NORMAL)
            p.textAlign=Paint.Align.CENTER; c.drawText(t,x,y,p)
        }
        fun line(x: Float,y: Float,x2: Float,y2: Float,color: Int,width: Float=5f) {
            p.color=color; p.strokeWidth=width; p.strokeCap=Paint.Cap.ROUND; c.drawLine(x,y,x2,y2,p)
        }
        fun ring(x:Float,y:Float,r:Float,f:Float) {
            p.style=Paint.Style.STROKE; p.strokeWidth=12f; p.color=a.accentDim(); c.drawCircle(x,y,r,p)
            p.color=a.accent; c.drawArc(x-r,y-r,x+r,y+r,-90f,360*f,false,p); p.style=Paint.Style.FILL
        }
        fun bar(x:Float,y:Float,w:Float,h:Float,f:Float) { rect(x,y,w,h,a.accentDim(),h/2);rect(x,y,w*f.coerceIn(0f,1f),h,a.accent,h/2) }
        fun dots(cols:Int,rows:Int,x:Float=40f,y:Float=90f,w:Float=200f,h:Float=100f, fraction:Float=.6f, color:Int=a.accent) {
            val r=min(w/cols,h/rows)*.43f
            repeat(rows) { row -> repeat(cols) { col -> dot(x+(col+.5f)*w/cols,y+(row+.5f)*h/rows,r,if(row*cols+col<cols*rows*fraction)color else a.secondary) } }
        }
        fun avatar(x:Float,y:Float,r:Float,index:Int=0) {
            dot(x,y,r,a.secondary);dot(x,y-r*.12f,r*.34f,Color.rgb(239,170+index%3*12,135))
            p.color=a.accent; c.drawOval(x-r*.7f,y+r*.25f,x+r*.7f,y+r*.95f,p)
            p.color=Color.BLACK;c.drawArc(x-r*.39f,y-r*.6f,x+r*.4f,y+r*.1f,180f,180f,true,p)
        }
        fun controls(y:Float=153f) { text("◀",65f,y,20f);rect(108f,y-27,64f,38f,a.accent);text("▶",140f,y,22f,Color.BLACK);text("▶",215f,y,20f) }
        rect(20f,10f,240f,200f,a.background,28f)
        val n=d.style
        when(d.category) {
            "Digital Clock" -> {
                val family=when(n%5){0->"sans-serif-condensed";1->"monospace";2->"serif";3->"sans-serif-light";else->"sans-serif-black"}
                if(n==33) {text("12",140f,96f,69f);text("36",140f,169f,69f)}
                else if(n in listOf(20,31)) {c.save();c.scale(.65f,1.8f,140f,110f);text("0839",140f,135f,72f,fg,true,"sans-serif-condensed");c.restore()}
                else { val value=if(n==34)"12:36" else "0238";text(value,140f,139f,if(n==34)61f else 70f,fg,n%4!=3,family)
                    if(n in listOf(9,15)){for(y in 65..155 step 9)line(30f,y.toFloat(),250f,y.toFloat(),a.background,4f)}
                    if(n==32) {dot(90f,75f,13f,a.accent);dot(202f,157f,13f,a.accent)}
                }
            }
            "Hybrid Clock" -> when {
                n>=12 -> { val cities=if(n==14)listOf("NEW YORK","TOKYO","PARIS","DUBAI") else listOf("NEW YORK","TOKYO"); cities.forEachIndexed{i,city-> val x=if(n==12)140f else if(i%2==0)82f else 198f; val y=if(n==12)57f+i*95 else if(n==14)50f+i/2*90 else 87f; text(city,x,y,12f,a.secondary);text(if(i==0)"03:08 am" else "04:08 pm",x,y+28,23f);if(n==16)text(if(i==0)"Off hours" else "Available",x,y+50,12f,a.accent)} }
                n==3 || n==4 -> {rect(35f,29f,210f,74f,a.secondary);text("09:50",140f,84f,51f);rect(35f,116f,96f,75f,a.secondary);rect(144f,116f,101f,75f,a.secondary);text("23",83f,167f,37f,a.accent);text("MON",195f,163f,25f)}
                n==11 -> {rect(20f,10f,120f,200f,a.accent);text("12",80f,135f,61f,Color.BLACK);text("36",198f,135f,61f)}
                n==7 -> {text("12 36",140f,70f,44f);rect(39f,93f,115f,39f,fg);text("MONDAY",96f,119f,18f,a.background);dot(228f,195f,70f,a.accent);dot(210f,165f,12f,fg);dot(235f,165f,12f,fg);dot(212f,165f,6f,Color.BLACK);dot(237f,165f,6f,Color.BLACK)}
                n==10 -> {text("09",140f,64f,44f,a.accent);text("44",140f,112f,44f);text("06",140f,160f,44f,a.secondary);text("17 TUESDAY",140f,192f,16f)}
                else -> {text(if(n==5||n==9)"12:36:48" else "12:36",140f,113f,if(n==5||n==9)44f else 62f);text("23 MONDAY",140f,149f,21f,if(n==8)a.accent else a.secondary);if(n==2){ring(221f,178f,18f,1f);text(":)",221f,183f,18f)}}
            }
            "Folder Apps" -> {
                val cols=when(n){1->2;2->3;3->7;4->4;else->4};val rows=when(n){1->2;4->1;else->3}
                val h=if(n==4)54f else 165f; val y=if(n==4)82f else 28f
                repeat(rows){r->repeat(cols){col->rect(36f+col*208f/cols,y+r*h/rows,208f/cols-9,h/rows-9,a.secondary,12f)}}
            }
            "Games" -> when(n) {
                1 -> repeat(9){i->text(if(i%3==0)"O" else "×",72f+i%3*68,69f+i/3*62,57f,if(i%3==0)a.accent else fg)}
                2 -> {listOf(78f to 54f,202f to 54f,140f to 110f,78f to 166f,202f to 166f).forEach{dot(it.first,it.second,17f,fg)}}
                3 -> {repeat(21){i->rect(40f+i%7*29,28f+i/7*25,25f,21f,fg,5f)};rect(96f,181f,88f,17f,a.accent,6f);dot(156f,132f,10f,a.accent)}
                4 -> {repeat(5){i->rect(if(i<3)178f else 178f-(i-2)*30,if(i<3)80f+i*30 else 140f,27f,27f,fg,6f)};rect(178f,29f,27f,27f,a.accent,6f)}
                5 -> repeat(9){i->rect(43f+i%3*66,24f+i/3*61,57f,53f,a.secondary,9f);if(i!=7)text("${(i+3)%8+1}",72f+i%3*66,61f+i/3*61,30f)}
                else -> {text("PLAY…",100f,60f,22f,fg,true,"monospace");rect(25f,177f,230f,30f,fg,0f);rect(159f,115f,37f,62f,fg,0f);rect(173f,89f,57f,37f,fg,0f);dot(211f,98f,3f,a.background);rect(135f,137f,55f,15f,fg,0f)}
            }
            "Music" -> {
                when(n){1,6 -> {avatar(80f,67f,34f);repeat(3){i->rect(if(i==0)151f else 43f+(i-1)*108,if(i==0)30f else 122f,86f,67f,if(i==1)a.accent else a.secondary);text(if(i==2)"◀" else "▶",if(i==0)194f else 86f+(i-1)*108,if(i==0)74f else 166f,25f,if(i==1)Color.BLACK else fg)}}
                    9 -> {ring(140f,110f,83f,.55f);avatar(140f,110f,57f);text("◀",53f,118f,22f);text("▶",227f,118f,22f)}
                    10 -> text("▶",140f,135f,65f,a.accent)
                    else -> {if(n in listOf(2,3,4,7,8))avatar(140f,51f,36f);text("Your music",140f,if(n in listOf(11,12))55f else 108f,21f);text("Artist",140f,if(n in listOf(11,12))77f else 128f,16f,a.secondary);controls(if(n in listOf(11,12))132f else 172f);if(n==12)dots(15,3,40f,157f,200f,39f) else if(n==11)bar(45f,165f,190f,24f,.45f)}
                }
            }
            "Personalize" -> when(n) {
                in 1..4 -> {avatar(140f,64f,41f);text("Your Name",140f,132f,28f);text("Bio description",140f,155f,14f,a.secondary);text("◎   ♧   ⓟ   f",140f,190f,25f,a.accent)}
                5,6,7 -> {text(if(n==6)"Anniversary" else "Birthday",140f,53f,21f,a.secondary);text(if(n==6)"108" else "463",140f,132f,70f,a.accent);text("Days remaining",140f,169f,17f);if(n!=5)bar(45f,186f,190f,9f,.6f)}
                8,9 -> text("YOUR TEXT",140f,120f,30f)
                10 -> {text("SPREADING SMILES SINCE",140f,52f,14f);text("17541 DAYS",140f,120f,31f,a.accent);text("42.2 Years",140f,177f,23f)}
                else -> {avatar(if(n==12)85f else 140f,110f,if(n==12)53f else 85f);if(n==12){avatar(202f,110f,53f);text("♥",144f,120f,30f,a.accent)}}
            }
            "Photo" -> {
                c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR)
                val path=Path();val cx=140f;val cy=110f;val radius=83f
                when(n) {
                    2,3 -> {dots(if(n==2)6 else 3,if(n==2)6 else 3,55f,25f,170f,170f,1f,a.background);return bitmap}
                    1,33 -> {path.moveTo(140f,180f);path.cubicTo(20f,100f,60f,15f,140f,70f);path.cubicTo(220f,15f,260f,100f,140f,180f)}
                    30,31 -> path.addOval(55f,30f,225f,190f,Path.Direction.CW)
                    35 -> {path.moveTo(57f,190f);path.lineTo(57f,105f);path.cubicTo(57f,-5f,223f,-5f,223f,105f);path.lineTo(223f,190f);path.close()}
                    36 -> path.addRoundRect(52f,25f,228f,195f,35f,35f,Path.Direction.CW)
                    else -> {val polygon= n in 25..29; val sides=when(n){25,27->6;26->5;28->4;29->3;else->360};repeat(sides){i->val angle=2*PI*i/sides-PI/2;val r=if(polygon)radius.toDouble() else radius*(.85+.15*cos(angle*(if(n%3==0)8 else if(n%3==1)6 else 12)));val x=cx+(r*cos(angle)).toFloat();val y=cy+(r*sin(angle)).toFloat();if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};path.close()}
                };p.color=a.background;p.style=Paint.Style.FILL;c.drawPath(path,p)
            }
            "Productivity" -> when(n) {
                1 -> {text("DAILY WORKOUT",140f,49f,19f);text("1 DAY",140f,78f,18f,a.accent);repeat(7){i->text("SMTWTFS"[i].toString(),50f+i*30,125f,17f);dot(50f+i*30,158f,10f,if(i==6)a.accent else a.secondary)}}
                2 -> {text("Personal QR",140f,37f,18f);repeat(17){r->repeat(17){col->if((r*31+col*13+r*col)%7<3)rect(66f+col*9,49f+r*9,9f,9f,fg,0f)}}}
                3,5 -> {text("NEW YEAR",140f,54f,25f);text("50",140f,135f,67f);text("DAYS LEFT",140f,175f,22f,a.accent)}
                4 -> {text("YEAR PROGRESS",140f,60f,19f);text("75.1%",140f,121f,46f);bar(42f,159f,196f,15f,.751f)}
                6,8,9 -> {text(if(n==9)"Clipboard" else "Bookmarks",140f,47f,24f,a.accent);repeat(3){i->text(if(n==9)"Tap to copy text" else "Your saved link ${i+1}",135f,89f+i*38,17f,fg,false);line(44f,98f+i*38,235f,98f+i*38,a.secondary,1f)}}
                10 -> {text("PO",140f,66f,35f);text("MO",140f,106f,35f);rect(90f,115f,100f,80f,a.accent);text("DO",140f,146f,35f,Color.BLACK);text("RO",140f,182f,35f,Color.BLACK)}
                else -> {text("2hr 30mins",140f,100f,33f);text("screen time",140f,131f,24f,a.secondary);bar(44f,160f,192f,if(n==12)29f else 10f,.65f)}
            }
            "Quick Settings" -> {
                val icon=when {d.name.startsWith("Wi-Fi")->"◉";d.name.startsWith("Mobile")->"⇅";d.name.startsWith("Bluetooth")->"ᛒ";d.name.startsWith("Airplane")->"✈";d.name.startsWith("Alarm")->"◷";d.name.startsWith("Dark")->"☼";d.name.startsWith("Do Not")->"⊖";d.name.startsWith("Location")->"●";d.name.startsWith("Auto")->"↻";d.name.startsWith("Sound")->"◖))";d.name.startsWith("Torch")->"ϟ";else->"≋"}
                val family=if(n<=3)1 else (n-4)/13+1
                if(family==1){rect(90f,35f,100f,90f,a.accent);text(icon,140f,96f,45f,Color.BLACK);text(d.name.replace(Regex(" [1-4]$"),""),140f,165f,22f)}
                else {text(icon,140f,111f,65f,if(family==3)a.accent else fg);if(family==2)text(d.name.replace(Regex(" [1-4]$"),""),140f,166f,20f)}
            }
        }
        return bitmap
    }
}


