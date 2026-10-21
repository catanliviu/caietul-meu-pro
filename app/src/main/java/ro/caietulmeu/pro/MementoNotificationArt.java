package ro.caietulmeu.pro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

/** Desenăm textul cu exact stilul din notițe, fiindcă Android nu respectă
 * Typeface-urile personalizate pentru titlurile notificărilor standard.
 * Bitmap-ul este folosit EXCLUSIV la mementouri cu opțiunea mesaj scris.
 */
final class MementoNotificationArt {
 private MementoNotificationArt(){}
 static int dp(Context c,float size){
  return Math.round(size*c.getResources().getDisplayMetrics().density);
 }
 static StaticLayout lines(String str,TextPaint paint,int width,int count){
  return StaticLayout.Builder.obtain(str,0,str.length(),paint,width)
   .setAlignment(Layout.Alignment.ALIGN_NORMAL)
   .setIncludePad(true)
   .setEllipsize(TextUtils.TruncateAt.END)
   .setMaxLines(count).build();
 }
 static Bitmap render(Context c,String title,String details,boolean expanded){
  boolean dark=ReminderKit.mainEditorPrefs(c).getBoolean("dark",false);
  int width=Math.min(dp(c,390),Math.max(dp(c,230),
   c.getResources().getDisplayMetrics().widthPixels-dp(c,72)));
  int pad=dp(c,12),contentWidth=width-pad*2;
  int ink=ReminderKit.reminderColor(c,dark);
  float sp=ReminderKit.reminderFontSp(c);
  float typeSize=Math.min(sp,expanded?40f:21f);
  Typeface type=ReminderKit.reminderTypeface(c);
  TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
  text.setSubpixelText(true);text.setTypeface(type);text.setColor(ink);
  text.setTextSize(typeSize*c.getResources().getDisplayMetrics().scaledDensity);
  String safeTitle=title==null||title.trim().isEmpty()?"Memento":title.trim();
  String safeDetails=details==null?"":details.trim();
  StaticLayout titleLines=lines(safeTitle,text,contentWidth,expanded?2:1);
  // Maximum 2 rânduri de detalii în notificarea extinsă; mesajul integral
  // rămâne disponibil pe ecranul alarmei în fontul original.
  TextPaint body=new TextPaint(text);
  body.setTextSize(Math.min(sp,expanded?35f:18f)*
    c.getResources().getDisplayMetrics().scaledDensity);
  StaticLayout detailsLines=(expanded&&!safeDetails.isEmpty())
   ?lines(safeDetails,body,contentWidth,2):null;
  int headHeight=expanded?dp(c,16):0;
  int gap=detailsLines==null?0:dp(c,5);
  int h=pad*2+headHeight+titleLines.getHeight()+
   gap+(detailsLines==null?0:detailsLines.getHeight());
  h=Math.max(dp(c,45),Math.min(h,dp(c,220)));
  Bitmap bitmap=Bitmap.createBitmap(width,h,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(bitmap);
  // Aspect de pergament păstrat în notificare, fără a moșteni stilul sistemului.
  ParchmentDrawable parchment=new ParchmentDrawable(dark);
  parchment.setBounds(0,0,width,h);parchment.draw(canvas);
  Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
  stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(dp(c,1));
  stroke.setColor(dark?0xff8D755B:0xffC4A77C);
  canvas.drawRoundRect(1,1,width-1,h-1,dp(c,9),dp(c,9),stroke);
  int top=pad;
  if(expanded){
   Paint heading=new Paint(Paint.ANTI_ALIAS_FLAG);
   heading.setTypeface(Typeface.create("serif",Typeface.BOLD));
   heading.setTextSize(dp(c,12));heading.setColor(dark?0xffE7C795:0xff7A5330);
   canvas.drawText("NU UITA  ·  MEMENTO",pad,top+dp(c,12),heading);
   top+=headHeight;
  }
  canvas.save();canvas.translate(pad,top);titleLines.draw(canvas);canvas.restore();
  top+=titleLines.getHeight()+gap;
  if(detailsLines!=null && top<h){
   canvas.save();canvas.translate(pad,top);detailsLines.draw(canvas);canvas.restore();
  }
  return bitmap;
 }
}
