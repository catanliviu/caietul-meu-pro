package ro.caietulmeu.pro;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interpretează offline comenzile românești, fără cerința de a rosti "salvează". */
final class RomanianReminderParser {
 private static final String MONTHS="ianuarie|februarie|martie|aprilie|mai|iunie|iulie|august|septembrie|octombrie|noiembrie|decembrie";
 private static final String[] MONTH_NAMES=MONTHS.split("\\|");
 private static final String UNITS="unu|una|un|o|doi|doua|trei|patru|cinci|sase|sapte|opt|noua";
 private static final String NUMBER_WORDS="cincizeci|patruzeci|treizeci|douazeci|nouasprezece|optsprezece|saptesprezece|saisprezece|cincisprezece|paisprezece|treisprezece|douasprezece|doisprezece|unsprezece|zece|noua|opt|sapte|sase|cinci|patru|trei|doua|doi|una|unu|zero|un|o";
 static final class Result {
  final boolean valid;
  final long at;
  final String title, details, error;
  private Result(long when,String t,String d,String e) {
   valid=e==null;at=when;title=t;details=d;error=e;
  }
  static Result fail(String reason){return new Result(0,"","",reason);}
 }
 private RomanianReminderParser(){}

 static String normalize(String text) {
  return Normalizer.normalize(text.toLowerCase(new Locale("ro","RO")),Normalizer.Form.NFD)
   .replaceAll("\\p{M}+","").replace('ş','s').replace('ţ','t')
   .replaceAll("\\s+"," ").trim();
 }
 static int wordValue(String s){
  switch(s){
   case "zero":return 0;
   case "un":case "unu":case "una":case "o":return 1;
   case "doi":case "doua":return 2;
   case "trei":return 3;
   case "patru":return 4;
   case "cinci":return 5;
   case "sase":return 6;
   case "sapte":return 7;
   case "opt":return 8;
   case "noua":return 9;
   case "zece":return 10;
   case "unsprezece":return 11;
   case "doisprezece":case "douasprezece":return 12;
   case "treisprezece":return 13;
   case "paisprezece":return 14;
   case "cincisprezece":return 15;
   case "saisprezece":return 16;
   case "saptesprezece":return 17;
   case "optsprezece":return 18;
   case "nouasprezece":return 19;
   case "douazeci":return 20;
   case "treizeci":return 30;
   case "patruzeci":return 40;
   case "cincizeci":return 50;
   default:return -1;
  }
 }
 static String digits(String input){
  Matcher m=Pattern.compile("\\b("+NUMBER_WORDS+")(?:\\s+(?:si\\s+)?("+UNITS+"))?\\b").matcher(input);
  StringBuffer out=new StringBuffer();
  while(m.find()){
   int a=wordValue(m.group(1)), b=m.group(2)==null?0:wordValue(m.group(2));
   // "trei si doi" nu reprezintă un număr compus; combinăm doar zecile.
   if(b>0&&a%10!=0){m.appendReplacement(out,Matcher.quoteReplacement(Integer.toString(a)+" si "+b));}
   else m.appendReplacement(out,Integer.toString(a+b));
  }
  m.appendTail(out);
  return out.toString().replaceAll("\\b(20|30|40|50)\\s+si\\s+([1-9])\\b",mrPlaceholder());
 }
 // Markerul de mai sus se înlocuiește printr-un al doilea pas cu calcul real.
 private static String mrPlaceholder(){return "$1 si $2";}
 static String spokenDigits(String input){
  String s=digits(input);
  Matcher m=Pattern.compile("\\b(20|30|40|50)\\s+si\\s+([1-9])\\b").matcher(s);
  StringBuffer result=new StringBuffer();
  while(m.find())m.appendReplacement(result,Integer.toString(Integer.parseInt(m.group(1))+Integer.parseInt(m.group(2))));
  m.appendTail(result);
  return result.toString();
 }
 static Calendar date(int y,int month,int day,int h,int min,TimeZone tz){
  if(y<2000||y>2200||month<1||month>12||day<1||day>31||h<0||h>23||min<0||min>59)return null;
  Calendar cal=Calendar.getInstance(tz);cal.clear();cal.setLenient(false);
  cal.set(y,month-1,day,h,min,0);
  try{cal.getTimeInMillis();return cal;}catch(Exception e){return null;}
 }
 private static int monthIndex(String month){
  for(int i=0;i<MONTH_NAMES.length;i++)if(MONTH_NAMES[i].equals(month))return i+1;
  return -1;
 }
 // Curățăm sufixele vocale de salvare fără să ștergem mesajul utilizatorului.
 private static String tidyMessage(String raw){
  if(raw==null)return "";
  String s=raw.trim().replaceAll("(?iu)\\s*[,;.]?\\s*(?:și|si)?\\s*salveaz(?:ă|a)?(?:-?o)?\\s*[.!]*$","").trim();
  s=s.replaceAll("^[\\s:;,.-]+|[\\s:;,.-]+$","").trim();
  if(s.matches("(?iu)^(respective|următoarele|urmatoarele|menționate|mentionate|de mai sus|acestea|asta)$"))return "";
  return s;
 }
 private static boolean commandOnly(String s){
  String n=normalize(s);
  return n.matches("(?s)^(?:imi |mi |te rog |va rog )?(?:faci|creezi|creaza|creeaza|setezi|programezi|pui|adaugi|trimiti)\\b.*") &&
    !n.matches("(?s).*\\b(?:cumpar|beau|iau|sun|platesc|merg|verific|ridic|fac sport)\\b.*");
 }
 private static String details(String spoken){
  // Prioritate mesajului explicit după „detaliile: ...”.
  Matcher explicit=Pattern.compile("(?iu)\\b(?:cu\\s+)?(?:detaliile|detalii|descrierea|descriere)\\s*[:,-]?\\s*(.+)$").matcher(spoken);
  if(explicit.find()){
   String value=tidyMessage(explicit.group(1));
   if(!value.isEmpty())return value;
  }
  // La comenzi precum «Vreau să faci un memento ... să cumpăr pâine»
  // ultimul «să» introduce ACȚIUNEA de citit, nu instrucțiunea de creare.
  Matcher actions=Pattern.compile("(?iu)\\b(?:să|sa)\\s+").matcher(spoken);
  String chosen="";
  while(actions.find()){
   String candidate=tidyMessage(spoken.substring(actions.end()));
   if(!candidate.isEmpty()&&!commandOnly(candidate))chosen=candidate;
  }
  if(!chosen.isEmpty())return chosen;
  Matcher subject=Pattern.compile("(?iu)\\b(?:despre|pentru)\\s+(.+)$").matcher(spoken);
  if(subject.find()){
   String s=tidyMessage(subject.group(1));
   if(!s.matches("(?iu)^(?:anul|luna|data|ziua|ora)\\b.*"))return s;
  }
  return "";
 }
 // Când recunoaștem al doilea enunț, acesta ESTE mesajul, nu o altă comandă.
 static String cleanDictatedMessage(String spoken){
  if(spoken==null)return "";
  String text=spoken.trim();
  text=text.replaceAll("(?iu)^\\s*(?:mesajul\\s+este\\s+|amintește-mi\\s+(?:să\\s+)?|aminteste-mi\\s+(?:sa\\s+)?)","");
  text=tidyMessage(text);
  return speechText(text,"").isEmpty()?"":text;
 }
 // Exact mesajul transmis către Text-to-Speech (fără «Memento. Memento»).
 static String speechText(String title,String details){
  String t=title==null?"":title.trim();
  String d=details==null?"":details.trim();
  boolean generic=t.isEmpty()||t.equalsIgnoreCase("Memento")||t.equalsIgnoreCase("Notificare");
  if(!d.isEmpty()){
   if(generic||t.equalsIgnoreCase(d)||d.toLowerCase(new Locale("ro","RO")).startsWith(t.toLowerCase(new Locale("ro","RO"))))
    return d;
   return t+". "+d;
  }
  return generic?"":t;
 }
 static Result parse(String speech,long currentTime,TimeZone tz){
  if(speech==null||speech.trim().isEmpty())return Result.fail("Nu am primit cuvintele de la microfon.");
  String s=spokenDigits(normalize(speech));
  Calendar now=Calendar.getInstance(tz);now.setTimeInMillis(currentTime);
  int year=now.get(Calendar.YEAR);
  boolean yearExplicit=false;
  Matcher yearMatch=Pattern.compile("\\b(?:anul|in anul|pentru anul)\\s+(20\\d{2}|21\\d{2})\\b").matcher(s);
  if(yearMatch.find()){year=Integer.parseInt(yearMatch.group(1));yearExplicit=true;}
  if(Pattern.compile("\\banul\\s+(?:viitor|urmator)\\b").matcher(s).find()){year=now.get(Calendar.YEAR)+1;yearExplicit=true;}
  if(Pattern.compile("\\banul\\s+(?:acesta|asta|curent)\\b").matcher(s).find()){year=now.get(Calendar.YEAR);yearExplicit=true;}

  Matcher relative=Pattern.compile("\\b(?:peste|in)\\s+(\\d{1,3})\\s+(minute|minut|ore|ora)\\b").matcher(s);
  long when=0;
  if(relative.find()){
   long n=Long.parseLong(relative.group(1));
   when=currentTime+n*(relative.group(2).startsWith("min")?60000L:3600000L);
   // La mementouri relative nu e nevoie de ora explicită.
  }else{
   Matcher clock=Pattern.compile("\\b(?:la\\s+)?(?:ora|orele)\\s*(\\d{1,2})(?:\\s*[:.,]\\s*(\\d{1,2})|\\s+(?:si\\s+)?(\\d{1,2}))?\\b").matcher(s);
   Matcher colon=Pattern.compile("\\bla\\s+(\\d{1,2})\\s*:\\s*(\\d{1,2})\\b").matcher(s);
   int hour=-1, minute=0;
   if(clock.find()){
    hour=Integer.parseInt(clock.group(1));
    if(clock.group(2)!=null)minute=Integer.parseInt(clock.group(2));
    else if(clock.group(3)!=null)minute=Integer.parseInt(clock.group(3));
   }else if(colon.find()){
    hour=Integer.parseInt(colon.group(1));minute=Integer.parseInt(colon.group(2));
   }
   if(hour<0||hour>23||minute<0||minute>59)return Result.fail("Nu am înțeles ora. Spune «la ora 12 și 46».");
   Matcher numeric=Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{4}))?\\b").matcher(s);
   Matcher named=Pattern.compile("\\b(\\d{1,2})\\s+(?:de\\s+)?("+MONTHS+")(?:\\s+(\\d{4}))?\\b").matcher(s);
   Matcher mon=Pattern.compile("\\b(?:luna\\s+(?:de\\s+)?|in\\s+)("+MONTHS+")\\b").matcher(s);
   Matcher day=Pattern.compile("\\b(?:data|ziua)\\s+(?:de\\s+)?(\\d{1,2})\\b|\\bpe\\s+(\\d{1,2})\\b").matcher(s);
   int d=-1, m=-1;
   if(s.matches("(?s).*\\bpoimaine\\b.*")||s.matches("(?s).*\\bmaine\\b.*")||s.matches("(?s).*\\b(?:azi|astazi)\\b.*")){
    Calendar c=(Calendar)now.clone();
    c.add(Calendar.DAY_OF_YEAR,s.matches("(?s).*\\bpoimaine\\b.*")?2:s.matches("(?s).*\\bmaine\\b.*")?1:0);
    d=c.get(Calendar.DAY_OF_MONTH);m=c.get(Calendar.MONTH)+1;year=c.get(Calendar.YEAR);yearExplicit=true;
   }else if(numeric.find()){
    d=Integer.parseInt(numeric.group(1));m=Integer.parseInt(numeric.group(2));
    if(numeric.group(3)!=null){year=Integer.parseInt(numeric.group(3));yearExplicit=true;}
   }else if(named.find()){
    d=Integer.parseInt(named.group(1));m=monthIndex(named.group(2));
    if(named.group(3)!=null){year=Integer.parseInt(named.group(3));yearExplicit=true;}
   }else{
    if(mon.find())m=monthIndex(mon.group(1));
    if(day.find())d=Integer.parseInt(day.group(1)!=null?day.group(1):day.group(2));
    if(d<0){
     // O comandă poate spune "în februarie pe șase"; numărul este deja convertit.
     return Result.fail("Nu am înțeles ziua. Spune «pe data de 30».");
    }
   }
   if(d<1||d>31||m>12)return Result.fail("Data rostită nu este validă.");
   Calendar chosen=null;
   if(m>0){
    chosen=date(year,m,d,hour,minute,tz);
    if(chosen!=null&&chosen.getTimeInMillis()<=currentTime&&!yearExplicit)chosen=date(year+1,m,d,hour,minute,tz);
   }else if(!yearExplicit){
    // «pe 30, la ora 12:46» = prima zi 30 care urmează.
    for(int i=0;i<36;i++){
     Calendar c=(Calendar)now.clone();c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,i);
     Calendar v=date(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,d,hour,minute,tz);
     if(v!=null&&v.getTimeInMillis()>currentTime){chosen=v;break;}
    }
   }else return Result.fail("Spune și luna pentru anul indicat.");
   if(chosen==null)return Result.fail("Data indicată nu există.");
   when=chosen.getTimeInMillis();
  }
  if(when<=currentTime)return Result.fail("Ora sau data indicată este deja în trecut.");
  String note=details(speech);
  String t=note.trim().isEmpty()?"Memento":note.trim();
  if(t.length()>65)t=t.substring(0,62)+"…";
  return new Result(when,t,note,null);
 }
}
