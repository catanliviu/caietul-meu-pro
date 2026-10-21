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
 static void message(String speech,String expected){
  RomanianReminderParser.Result result=RomanianReminderParser.parse(speech,NOW.getTimeInMillis(),TZ);
  if(!result.valid)throw new AssertionError(speech+" -> "+result.error);
  if(!expected.equals(result.details))
   throw new AssertionError("Mesaj greșit pentru «"+speech+"»: «"+result.details+"» (așteptat «"+expected+"»)");
  if(!expected.isEmpty()&&!expected.equals(RomanianReminderParser.speechText(result.title,result.details)))
   throw new AssertionError("Vocea ar repeta sau ar pierde mesajul: "+
    RomanianReminderParser.speechText(result.title,result.details));
  tested++;
 }
 static void spoken(String title,String details,String expected){
  String found=RomanianReminderParser.speechText(title,details);
  if(!expected.equals(found))throw new AssertionError("TTS pentru "+title+": «"+found+"», așteptat «"+expected+"»");
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
  message("Amintește-mi pe 30 octombrie la ora 12 și 46 să o sun pe Carmen","o sun pe Carmen");
  message("Vreau să îmi faci un memento mâine la ora 11 să beau apă","beau apă");
  message("Memento mâine la ora 11 să cumpăr pâine","cumpăr pâine");
  message("Vreau un memento anul viitor în februarie în data de 6 la ora 15:36 cu detaliile respective","");
  spoken("Memento","","");
  spoken("Memento","Nu uita să bei apă","Nu uita să bei apă");
  spoken("Nu uita să bei apă","Nu uita să bei apă","Nu uita să bei apă");
  spoken("Sună medicul","La ora programată","Sună medicul. La ora programată");
  spoken("Memento","să beau apă","să beau apă");
  message("Vreau să faci un memento pe 30 octombrie la ora 12:46 să cumpăr pâine și să iau lapte","cumpăr pâine și să iau lapte");
  message("Amintește-mi mâine la ora 11 să îi spun lui Carmen că ajung târziu și să iau apă","îi spun lui Carmen că ajung târziu și să iau apă");
  message("Memento peste două minute să închid aragazul și să verific ușa","închid aragazul și să verific ușa");
  message("Memento mâine la ora 11 să nu uit să cumpăr și să plătesc","nu uit să cumpăr și să plătesc");
  String longText="Trebuie să sun medicul pentru programarea la consultație și să întreb despre analize și să cumpăr medicamente după aceea";
  message("Memento pe 30 octombrie la ora 12:46 să "+longText,""+longText);
  spoken(longText.substring(0,62)+"…",longText,longText);
  String voiceMessage=RomanianReminderParser.cleanDictatedMessage("Nu uita să iei medicamentele, salvează");
  if(!"Nu uita să iei medicamentele".equals(voiceMessage))
   throw new AssertionError("Eroare mesaj dictat separat: "+voiceMessage);
  tested++;
  if(!RomanianReminderParser.cleanDictatedMessage("Memento").isEmpty())
   throw new AssertionError("Nu trebuie salvat titlul generic drept mesaj vocal");
  tested++;
  RomanianReminderParser.Result dateOnly=RomanianReminderParser.parse(
   "Vreau un memento pe data de 30 octombrie la ora 12:46",
   NOW.getTimeInMillis(),TZ);
  if(!dateOnly.valid||!RomanianReminderParser.speechText(dateOnly.title,dateOnly.details).isEmpty())
   throw new AssertionError("Comanda fără mesaj trebuie să ceară a doua dictare.");
  tested++;
  RomanianReminderParser.Result complete=RomanianReminderParser.parse(
   "Vreau un memento mâine la ora 11 să beau apă",
   NOW.getTimeInMillis(),TZ);
  if(!complete.valid||!"beau apă".equals(RomanianReminderParser.speechText(complete.title,complete.details)))
   throw new AssertionError("Comanda completă trebuie să fie salvată automat text+voce.");
  tested++;
  invalid("Vreau un memento cu detaliile respective");
  invalid("Notificare la ora 12:46");
  invalid("Memento pe 30 octombrie");
  invalid("Pe 6 februarie anul 2026 la ora 15:36");
  System.out.println("OK: "+tested+" teste memento vocal");
 }
}
