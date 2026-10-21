package ro.caietulmeu.pro;

import java.util.Calendar;
import java.util.TimeZone;

/** Rulabil cu javac, verifică exemple reale înainte de construirea APK. */
public final class ParserSmokeTest {
 static final TimeZone TZ=TimeZone.getTimeZone("Europe/Berlin");
 static final Calendar NOW=Calendar.getInstance(TZ);
 static int tested=0;
 static {
  NOW.clear();NOW.set(2026,Calendar.OCTOBER,21,19,55,0);
 }
 static void check(String speech,int y,int month,int day,int h,int minute){
  RomanianReminderParser.Result result=RomanianReminderParser.parse(speech,NOW.getTimeInMillis(),TZ);
  if(!result.valid)throw new AssertionError(speech+" -> "+result.error);
  Calendar value=Calendar.getInstance(TZ);value.setTimeInMillis(result.at);
  if(value.get(Calendar.YEAR)!=y||value.get(Calendar.MONTH)+1!=month
      ||value.get(Calendar.DAY_OF_MONTH)!=day||value.get(Calendar.HOUR_OF_DAY)!=h||value.get(Calendar.MINUTE)!=minute)
   throw new AssertionError(speech+" -> "+value.getTime()+" expected "+y+"-"+month+"-"+day+" "+h+":"+minute);
  tested++;
 }
 static void invalid(String speech){
  RomanianReminderParser.Result result=RomanianReminderParser.parse(speech,NOW.getTimeInMillis(),TZ);
  if(result.valid)throw new AssertionError("Trebuia să respingă: "+speech);
  tested++;
 }
 public static void main(String[] args){
  check("Amintește-mi pe 30 octombrie la ora 12 și 46 să o sun pe Carmen",2026,10,30,12,46);
  check("Vreau în data de 30 să faci o notificare la ora 12:46",2026,10,30,12,46);
  check("În data de treizeci la ora doisprezece și patruzeci și șase să iau lapte",2026,10,30,12,46);
  check("Vreau un memento anul viitor în luna februarie în data de 6 la ora 15:36 cu detaliile respective",2027,2,6,15,36);
  check("Amintește-mi pe 6 februarie 2027 la ora cincisprezece și treizeci și șase să verific programarea",2027,2,6,15,36);
  check("Memento mâine la ora 11 să cumpăr pâine",2026,10,22,11,0);
  check("Pune notificare poimâine la ora 9 și 15",2026,10,23,9,15);
  check("Notificare pe 30.10.2026 la 12:46",2026,10,30,12,46);
  check("Memento peste două minute să beau apă",2026,10,21,19,57);
  check("La ora 15 36 pe 6 februarie anul viitor",2027,2,6,15,36);
  invalid("Vreau un memento cu detaliile respective");
  invalid("Notificare la ora 12:46");
  invalid("Memento pe 30 octombrie");
  invalid("Pe 6 februarie anul 2026 la ora 15:36");
  System.out.println("OK: "+tested+" teste memento vocal");
 }
}
