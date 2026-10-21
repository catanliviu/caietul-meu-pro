package ro.caietulmeu.pro;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.util.Random;

/**
 * Pergament realizat local, fără imagine din rețea și fără bitmap mare.
 * Fibre subtile, granulație discretă și margini puțin mai vechi.
 */
final class ParchmentDrawable extends Drawable {
 private final boolean night;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final BitmapShader grain;
 private int alpha=255;

 ParchmentDrawable(boolean nightMode){
  night=nightMode;
  Bitmap tile=Bitmap.createBitmap(144,144,Bitmap.Config.ARGB_8888);
  Canvas c=new Canvas(tile);
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  Random rng=new Random(82751975L);
  for(int i=0;i<190;i++){
   float x=rng.nextInt(144),y=rng.nextInt(144);
   float radius=(i%7==0)?0.9f:0.35f;
   p.setColor(night?0x163E2B20:0x21906B3C);
   c.drawCircle(x,y,radius,p);
  }
  for(int i=0;i<19;i++){
   float x=rng.nextInt(144),y=rng.nextInt(144);
   p.setColor(night?0x104E3929:0x168A653B);
   p.setStrokeWidth(0.45f);
   c.drawLine(x,y,Math.min(144,x+10+rng.nextInt(20)),y-1,p);
  }
  grain=new BitmapShader(tile,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
 }
 @Override public void draw(Canvas canvas){
  Rect b=getBounds();
  if(b.width()<=0||b.height()<=0)return;
  paint.setAlpha(alpha);
  paint.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,
   night?new int[]{0xff30251F,0xff463528,0xff2B211D}:
    new int[]{0xffF8EBD0,0xffEEDCB7,0xffFAEFD8},
   new float[]{0f,0.57f,1f},Shader.TileMode.CLAMP));
  canvas.drawRect(b,paint);
  paint.setShader(grain);
  canvas.drawRect(b,paint);
  // Margini ușor patinate, fără pete mari ce ar afecta citirea.
  paint.setShader(new LinearGradient(b.left,b.top,b.left+Math.max(30,b.width()*0.16f),b.top,
   night?0x3B130D0A:0x38946B37,Color.TRANSPARENT,Shader.TileMode.CLAMP));
  canvas.drawRect(b.left,b.top,b.left+Math.max(30,b.width()*0.16f),b.bottom,paint);
  paint.setShader(new LinearGradient(b.right-Math.max(30,b.width()*0.16f),b.top,b.right,b.top,
   Color.TRANSPARENT,night?0x38130D0A:0x31946B37,Shader.TileMode.CLAMP));
  canvas.drawRect(b.right-Math.max(30,b.width()*0.16f),b.top,b.right,b.bottom,paint);
  paint.setShader(new LinearGradient(b.left,b.top,b.left,b.top+Math.max(48,b.height()*0.13f),
   night?0x2D130D0A:0x26936A3F,Color.TRANSPARENT,Shader.TileMode.CLAMP));
  canvas.drawRect(b.left,b.top,b.right,b.top+Math.max(48,b.height()*0.13f),paint);
  paint.setShader(new LinearGradient(b.left,b.bottom-Math.max(48,b.height()*0.13f),b.left,b.bottom,
   Color.TRANSPARENT,night?0x29130D0A:0x24936A3F,Shader.TileMode.CLAMP));
  canvas.drawRect(b.left,b.bottom-Math.max(48,b.height()*0.13f),b.right,b.bottom,paint);
  paint.setShader(null);
 }
 @Override public void setAlpha(int a){alpha=a;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter cf){paint.setColorFilter(cf);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
