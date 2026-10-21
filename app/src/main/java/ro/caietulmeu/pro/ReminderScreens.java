package ro.caietulmeu.pro;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

final class ReminderScreens {
 final MainActivity a;
 EditText title,description,commandField;
 TextView dateLabel,timeLabel,soundLabel,repeatLabel,voiceFeedback;
 long at;
 String sound="",soundName="Sunetul implicit al telefonului",repeat="O singură dată";
 JSONObject editing;
 Calendar calendar=Calendar.getInstance();
 Ringtone preview;
 String lastSavedLabel="";
 static Ringtone sharedPreview;
 final SimpleDateFormat dateFormat=new SimpleDateFormat("EEEE, dd MMMM yyyy",new Locale("ro","RO"));
 final SimpleDateFormat timeFormat=new SimpleDateFormat("HH:mm",Locale.ROOT);
 ReminderScreens(MainActivity activity){a=activity;}
 void enter(){a.current=-1;a.remindersActive=true;a.root=a.vertical();}
 static void stopSharedPreview(){if(sharedPreview!=null){try{sharedPreview.stop();}catch(Exception ignored){}sharedPreview=null;}}
 void stopPreview(){stopSharedPreview();preview=null;}
 void navigation(String titleText){
  TextView h=new TextView(a);h.setText(titleText);h.setTextSize(23);h.setTextColor(a.fg());
  h.setPadding(15,12,10,12);a.root.addView(h);
 }
 String date(long millis){return dateFormat.format(new Date(millis));}
 String time(long millis){return timeFormat.format(new Date(millis));}
 void list(){
  stopPreview();enter();
  LinearLayout nav=a.line(a.root);
  a.button("‹ Notițe",nav,()->a.home());
  a.button("📅 Calendar",nav,()->showCalendar());
  a.button("➕ Memento nou",nav,()->form(null,"",""));
  a.button("🔊 PROBĂ ALARMĂ",nav,()->ReminderKit.testAlarm(a,""));
  a.button("🔇 OPREȘTE ALARMA",nav,()->ReminderKit.stopFromUi(a));
  a.button("🎙️ Memento vocal",nav,()->{form(null,"","");dictate();});
  navigation("🧠 Nu Uita — Mementourile mele");
  if(!lastSavedLabel.isEmpty())navigation(lastSavedLabel);
  ScrollView scroll=new ScrollView(a);LinearLayout items=new LinearLayout(a);items.setOrientation(1);scroll.addView(items);a.root.addView(scroll);
  JSONArray data=ReminderKit.all(a);
  ArrayList<JSONObject> sorted=new ArrayList<>();
  for(int i=0;i<data.length();i++)if(data.optJSONObject(i)!=null)sorted.add(data.optJSONObject(i));
  sorted.sort((x,y)->Long.compare(x.optLong("at"),y.optLong("at")));
  if(sorted.isEmpty())a.button("Nu ai încă mementouri. Apasă «Memento nou».",items,()->form(null,"",""));
  for(JSONObject o:sorted){
   int id=o.optInt("id");long when=o.optLong("at");
   String label="🔔 "+o.optString("title","Memento")+"\n"+date(when)+", "+time(when);
   if(when<System.currentTimeMillis() && "O singură dată".equals(o.optString("repeat")))label+=" • Trecut";
   a.button(label,items,()->form(ReminderKit.find(a,id),"",""));
  }
 }
 void showCalendar(){calendar.set(Calendar.DAY_OF_MONTH,1);month();}
 void month(){
  enter();
  LinearLayout nav=a.line(a.root);
  a.button("‹ Mementouri",nav,()->list());
  a.button("◀",nav,()->{calendar.add(Calendar.MONTH,-1);month();});
  a.button("▶",nav,()->{calendar.add(Calendar.MONTH,1);month();});
  a.button("➕ Nou",nav,()->form(null,"",""));
  SimpleDateFormat head=new SimpleDateFormat("MMMM yyyy",new Locale("ro","RO"));
  navigation("📅 "+head.format(calendar.getTime()));
  LinearLayout header=new LinearLayout(a);a.root.addView(header);
  for(String s:new String[]{"Lu","Ma","Mi","Jo","Vi","Sâ","Du"}){
   TextView v=cell(s,false);header.addView(v,new LinearLayout.LayoutParams(0,-2,1));
  }
  Calendar first=(Calendar)calendar.clone();first.set(Calendar.DAY_OF_MONTH,1);
  int offset=(first.get(Calendar.DAY_OF_WEEK)+5)%7;
  int max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
  JSONArray data=ReminderKit.all(a);
  for(int w=0;w<6;w++){
   LinearLayout r=new LinearLayout(a);a.root.addView(r);
   for(int col=0;col<7;col++){
    int day=w*7+col-offset+1;
    if(day<1||day>max){r.addView(cell("",false),new LinearLayout.LayoutParams(0,-2,1));continue;}
    Calendar c=(Calendar)first.clone();c.set(Calendar.DAY_OF_MONTH,day);
    boolean has=false;for(int j=0;j<data.length();j++){
     JSONObject o=data.optJSONObject(j);if(o==null)continue;
     Calendar d=Calendar.getInstance();d.setTimeInMillis(o.optLong("at"));
     if(d.get(Calendar.YEAR)==c.get(Calendar.YEAR)&&d.get(Calendar.DAY_OF_YEAR)==c.get(Calendar.DAY_OF_YEAR)){has=true;break;}
    }
    TextView v=cell(day+(has?" 🔔":""),has);
    r.addView(v,new LinearLayout.LayoutParams(0,-2,1));
    v.setOnClickListener(x->dayEvents(c));
   }
  }
  navigation("Atinge o zi ca să vezi programările.");
 }
 TextView cell(String text,boolean marked){
  TextView v=new TextView(a);v.setText(text);v.setTextSize(13);v.setTextColor(a.fg());v.setGravity(Gravity.CENTER);
  v.setPadding(1,14,1,14);v.setBackgroundColor(marked?(a.dark?0xff3a6488:0xffcce8ff):a.panel());return v;
 }
 void dayEvents(Calendar day){
  enter();
  LinearLayout nav=a.line(a.root);
  a.button("‹ Calendar",nav,()->month());
  a.button("➕ Memento",nav,()->{form(null,"","");setDay(day);});
  navigation("📅 "+date(day.getTimeInMillis()));
  JSONArray data=ReminderKit.all(a);int count=0;
  for(int i=0;i<data.length();i++){
   JSONObject o=data.optJSONObject(i);if(o==null)continue;
   Calendar d=Calendar.getInstance();d.setTimeInMillis(o.optLong("at"));
   if(d.get(Calendar.YEAR)==day.get(Calendar.YEAR)&&d.get(Calendar.DAY_OF_YEAR)==day.get(Calendar.DAY_OF_YEAR)){
    count++;int id=o.optInt("id");
    a.button(time(o.optLong("at"))+" 🔔 "+o.optString("title"),a.root,()->form(ReminderKit.find(a,id),"",""));
   }
  }
  if(count==0)navigation("Nicio programare în această zi.");
 }
 void setDay(Calendar day){
  Calendar c=Calendar.getInstance();c.setTimeInMillis(at);
  c.set(day.get(Calendar.YEAR),day.get(Calendar.MONTH),day.get(Calendar.DAY_OF_MONTH));
  at=c.getTimeInMillis();refresh();
 }
 void form(JSONObject existing,String initialTitle,String initialBody){
  stopPreview();enter();editing=existing;
  Calendar selected=Calendar.getInstance();
  if(existing!=null){at=existing.optLong("at");repeat=existing.optString("repeat","O singură dată");}
  else{selected.add(Calendar.HOUR_OF_DAY,1);selected.set(Calendar.MINUTE,0);selected.set(Calendar.SECOND,0);at=selected.getTimeInMillis();repeat="O singură dată";}
  // Globalul suprascrie melodia fiecărui memento, inclusiv cele vechi.
  sound=ReminderKit.globalSound(a,existing==null?"":existing.optString("sound",""));
  soundName=ReminderKit.globalSoundName(a,existing==null?"Sunetul implicit al telefonului":existing.optString("soundName","Sunetul implicit al telefonului"));
  LinearLayout nav=a.line(a.root);
  a.button("‹ Înapoi",nav,()->list());
  navigation(existing==null?"🔔 Memento nou":"🔔 Editează memento");
  ScrollView scroll=new ScrollView(a);LinearLayout content=new LinearLayout(a);content.setPadding(6,8,6,20);
  content.setOrientation(1);scroll.addView(content);a.root.addView(scroll);
  title=a.field("Ce trebuie să îți amintești?",false);title.setText(existing==null?initialTitle:existing.optString("title"));
  content.addView(title);
  description=a.field("Detalii (opțional)",true);description.setMinLines(2);description.setMaxLines(4);
  description.setText(existing==null?initialBody:existing.optString("details"));content.addView(description);
  commandField=a.field("Comandă: Amintește-mi pe 30 octombrie la ora 12:46 să...",true);
  commandField.setMinLines(2);commandField.setMaxLines(3);content.addView(commandField);
  voiceFeedback=new TextView(a);voiceFeedback.setTextColor(a.fg());voiceFeedback.setPadding(12,8,12,8);
  voiceFeedback.setText("Spune data și ora prin voce. Dacă le înțeleg, salvez automat mementoul.");content.addView(voiceFeedback);
  LinearLayout speak=a.line(content);
  a.button("🎙️ Spune comanda",speak,()->dictate());
  a.button("✅ Execută textul",speak,()->{
   if(!processCommand(commandField.getText().toString()))Toast.makeText(a,"Nu am înțeles data și ora. Corectează textul de mai sus.",Toast.LENGTH_LONG).show();
  });
  a.button("📅 Alege data",speak,()->datePicker());
  a.button("🕒 Alege ora",speak,()->timePicker());
  dateLabel=new TextView(a);dateLabel.setTextColor(a.fg());dateLabel.setTextSize(19);dateLabel.setPadding(14,12,14,9);content.addView(dateLabel);
  timeLabel=new TextView(a);timeLabel.setTextColor(a.fg());timeLabel.setTextSize(19);timeLabel.setPadding(14,8,14,15);content.addView(timeLabel);
  LinearLayout opts=a.line(content);
  a.button("🔁 Repetare",opts,()->pickRepeat());
  a.button("🎵 Alege sunet",opts,()->pickSound());
  a.button("🎶 Audio din telefon",opts,()->pickAudio());
  repeatLabel=new TextView(a);repeatLabel.setTextColor(a.fg());repeatLabel.setPadding(14,10,14,5);content.addView(repeatLabel);
  soundLabel=new TextView(a);soundLabel.setTextColor(a.fg());soundLabel.setPadding(14,8,14,10);content.addView(soundLabel);
  LinearLayout tests=a.line(content);
  a.button("▶ Ascultă",tests,()->testSound());
  a.button("■ Oprește test",tests,()->stopPreview());
  a.button("🔊 TEST ALARMĂ REALĂ",tests,()->ReminderKit.testAlarm(a,sound));
  a.button("🔇 OPREȘTE ORICE SUNET",tests,()->ReminderKit.stopFromUi(a));
  LinearLayout action=a.line(content);
  a.button("💾 SALVEAZĂ MEMENTO",action,()->save());
  if(existing!=null){
   a.button("🗑️ ȘTERGE",action,()->new AlertDialog.Builder(a)
    .setMessage("Ștergi definitiv acest memento?")
    .setNegativeButton("Nu",null)
    .setPositiveButton("Șterge",(d,w)->{ReminderKit.delete(a,existing.optInt("id"));list();}).show());
  }
  refresh();
 }
 void refresh(){
  if(dateLabel==null||timeLabel==null)return;
  dateLabel.setText("📅 Data: "+date(at));timeLabel.setText("🕒 Ora: "+time(at));
  repeatLabel.setText("🔁 Repetare: "+repeat);
  soundLabel.setText("🎵 Melodie permanentă, pentru TOATE alarmele: "+soundName);
 }
 void datePicker(){
  Calendar c=Calendar.getInstance();c.setTimeInMillis(at);
  new DatePickerDialog(a,(picker,y,m,d)->{
   Calendar n=Calendar.getInstance();n.setTimeInMillis(at);n.set(y,m,d);at=n.getTimeInMillis();refresh();
  },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
 }
 void timePicker(){
  Calendar c=Calendar.getInstance();c.setTimeInMillis(at);
  new TimePickerDialog(a,(picker,h,m)->{
   Calendar n=Calendar.getInstance();n.setTimeInMillis(at);n.set(Calendar.HOUR_OF_DAY,h);n.set(Calendar.MINUTE,m);n.set(Calendar.SECOND,0);
   at=n.getTimeInMillis();refresh();
  },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();
 }
 void pickRepeat(){
  String[] options={"O singură dată","Zilnic","Săptămânal","Lunar"};
  new AlertDialog.Builder(a).setTitle("Repetarea alarmei").setSingleChoiceItems(options,Arrays.asList(options).indexOf(repeat),(d,index)->{repeat=options[index];d.dismiss();refresh();}).setNegativeButton("Renunță",null).show();
 }
 void pickSound(){
  stopPreview();
  ArrayList<String> names=new ArrayList<>();ArrayList<String> uris=new ArrayList<>();
  names.add("Sunetul implicit al telefonului");uris.add("");
  try{
   RingtoneManager rm=new RingtoneManager(a);rm.setType(RingtoneManager.TYPE_ALARM);
   try(Cursor c=rm.getCursor()){
    for(int i=0;i<c.getCount()&&i<60;i++) {
     c.moveToPosition(i);
     Uri u=rm.getRingtoneUri(i);
     if(u!=null){names.add(c.getString(RingtoneManager.TITLE_COLUMN_INDEX));uris.add(u.toString());}
    }
   }
  }catch(Exception e){Toast.makeText(a,"Nu pot citi lista de alarme.",Toast.LENGTH_LONG).show();}
  new AlertDialog.Builder(a).setTitle("Alege un sunet de alarmă").setItems(names.toArray(new String[0]),(d,i)->{
   sound=uris.get(i);soundName=names.get(i);
   ReminderKit.rememberGlobalSound(a,sound,soundName);
   refresh();testSound();
   Toast.makeText(a,"✓ Melodia este memorată pentru toate mementourile",Toast.LENGTH_LONG).show();
  }).setNegativeButton("Renunță",null).show();
 }
 void pickAudio(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");
  i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  try{a.startActivityForResult(i,79);}catch(Exception e){Toast.makeText(a,"Selectorul audio nu este disponibil",Toast.LENGTH_LONG).show();}
 }
 void onAudio(Intent data){
  if(data==null||data.getData()==null)return;
  Uri u=data.getData();
  try{a.getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
  sound=u.toString();soundName="Melodie personală";
  try(Cursor c=a.getContentResolver().query(u,null,null,null,null)){
   if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)soundName=c.getString(i);}
  }catch(Exception ignored){}
  ReminderKit.rememberGlobalSound(a,sound,soundName);
  refresh();testSound();
  Toast.makeText(a,"✓ Melodia aleasă rămâne activă până o schimbi",Toast.LENGTH_LONG).show();
 }
 void testSound(){
  stopPreview();
  try{
   Uri uri=sound.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(sound);
   preview=RingtoneManager.getRingtone(a,uri);sharedPreview=preview;
   if(preview!=null){if(Build.VERSION.SDK_INT>=28)preview.setLooping(false);preview.play();}
   else Toast.makeText(a,"Sunet indisponibil",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(a,"Nu pot reda acest sunet",Toast.LENGTH_SHORT).show();}
 }
 boolean save(){
  String name=title.getText().toString().trim();
  if(name.isEmpty()){title.setError("Spune ce trebuie să îți amintești");return false;}
  if(at<=System.currentTimeMillis()){Toast.makeText(a,"Alege o dată și o oră din viitor.",Toast.LENGTH_LONG).show();return false;}
  try{
   JSONObject o=editing==null?new JSONObject():new JSONObject(editing.toString());
   if(editing==null)o.put("id",(int)(System.currentTimeMillis()&0x7fffffff));
   o.put("title",name);o.put("details",description.getText().toString());
   o.put("at",at);o.put("repeat",repeat);o.put("sound",sound);o.put("soundName",soundName);
   int id=o.optInt("id");
   // Persistăm sincron, verificăm abia apoi programarea exactă a alarmei.
   if(!ReminderKit.save(a,o)||ReminderKit.find(a,id)==null){
    Toast.makeText(a,"Nu am putut salva mementoul în memoria telefonului.",Toast.LENGTH_LONG).show();
    return false;
   }
   ReminderKit.cancel(a,id);
   boolean scheduled=ReminderKit.schedule(a,o);
   stopPreview();
   lastSavedLabel=(scheduled?"✅ SALVAT ȘI PROGRAMAT: ":"✅ SALVAT (alarma necesită permisiune): ")
    +name+" — "+date(at)+", "+time(at);
   list();
   Toast.makeText(a,lastSavedLabel,Toast.LENGTH_LONG).show();
   if(!scheduled)ReminderKit.askPermission(a);
   return true;
  }catch(Exception e){
   android.util.Log.e("Memento","Eroare la salvare",e);
   Toast.makeText(a,"Eroare la salvare: "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();
   return false;
  }
 }
 void dictate(){
  Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ro-RO");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ro-RO");
  i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
  i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Exemplu: Memento pe 30 octombrie la ora 12:46 să sun medicul. Salvează.");
  try{a.startActivityForResult(i,78);}catch(Exception e){Toast.makeText(a,"Dictarea vocală nu este disponibilă",Toast.LENGTH_LONG).show();}
 }

 // Romanian spoken numbers are often transcribed as words, including "patruzeci și șase".
 static String normalized(String t){
  return java.text.Normalizer.normalize(t.toLowerCase(new Locale("ro","RO")),java.text.Normalizer.Form.NFD)
   .replaceAll("\\p{M}+","").replace('ş','s').replace('ţ','t');
 }
 static int spokenNumber(String term){
  switch(term.trim()){
   case "zero":return 0;
   case "unu":case "una":case "un":return 1;
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
   case "paisprezece":case "patrusprezece":return 14;
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
 static String numericSpeech(String speech){
  String words="cincizeci|patruzeci|treizeci|douazeci|nouasprezece|optsprezece|saptesprezece|saisprezece|cincisprezece|patrusprezece|paisprezece|treisprezece|douasprezece|doisprezece|unsprezece|zece|noua|opt|sapte|sase|cinci|patru|trei|doua|doi|una|unu|zero";
  Pattern p=Pattern.compile("\\b("+words+")(?:\\s+si\\s+(unu|una|doi|doua|trei|patru|cinci|sase|sapte|opt|noua))?\\b");
  Matcher m=p.matcher(speech);StringBuffer out=new StringBuffer();
  while(m.find()){
   int base=spokenNumber(m.group(1)),unit=m.group(2)==null?0:spokenNumber(m.group(2));
   m.appendReplacement(out,Matcher.quoteReplacement(Integer.toString(base+unit)));
  }
  m.appendTail(out);return out.toString();
 }
 static Calendar exactDate(int year,int month,int day,int hour,int minute){
  if(year<2000||month<1||month>12||day<1||day>31||hour<0||hour>23||minute<0||minute>59)return null;
  Calendar result=Calendar.getInstance();result.clear();result.setLenient(false);
  result.set(year,month-1,day,hour,minute,0);
  try{result.getTimeInMillis();return result;}catch(Exception e){return null;}
 }
 static Calendar dayNext(int day,int hour,int minute){
  Calendar now=Calendar.getInstance();
  for(int i=0;i<24;i++){
   Calendar first=(Calendar)now.clone();first.set(Calendar.DAY_OF_MONTH,1);first.add(Calendar.MONTH,i);
   Calendar candidate=exactDate(first.get(Calendar.YEAR),first.get(Calendar.MONTH)+1,day,hour,minute);
   if(candidate!=null&&candidate.after(now))return candidate;
  }
  return null;
 }

 // Keep the whole dictated description; a short excerpt is used as the title.
 static String dictatedDetails(String spoken){
  Matcher detail=Pattern.compile("(?iu)\\b(?:cu\\s+)?(?:detaliile|detalii|descrierea|descriere)\\s*[:,-]?\\s*(.+)$").matcher(spoken);
  if(!detail.find())return "";
  String value=detail.group(1).trim()
   .replaceAll("(?iu)(?:\\s*[,;.]?\\s*(?:și|si)\\s*)?salveaz(?:ă|a)?(?:-?o)?[\\s.!]*$","")
   .replaceAll("^[\\s:,-]+|[\\s]+$","");
  return value;
 }
 static String shortTitle(String details){
  String s=details.trim();
  if(s.isEmpty())return "Notificare";
  int line=s.indexOf('\n');if(line>0)s=s.substring(0,line);
  if(s.length()>62){
   int last=s.lastIndexOf(' ',62);
   s=s.substring(0,last>25?last:62).trim()+"…";
  }
  return s.substring(0,1).toUpperCase(new Locale("ro","RO"))+s.substring(1);
 }
 static String makeTitle(String utterance){
  String s=numericSpeech(normalized(utterance));
  String months="ianuarie|februarie|martie|aprilie|mai|iunie|iulie|august|septembrie|octombrie|noiembrie|decembrie";
  s=s.replaceAll("\\b(?:anul\\s+(?:viitor|urmator|acesta|asta|curent)|la\\s+anul|(?:in\\s+)?anul\\s+\\d{4})\\b"," ");
  s=s.replaceAll("\\b(?:in\\s+)?luna\\s+(?:de\\s+)?(?:"+months+")\\b"," ");
  s=s.replaceAll("\\b(?:in\\s+)?(?:anul\\s+)?(?:20|21)\\d{2}\\b"," ");
  s=s.replaceAll("\\b(?:la\\s+ora|ora|orele|la)\\s+\\d{1,2}(?:\\s*(?:[:.,]|si)\\s*\\d{1,2}|\\s+\\d{2})?\\b"," ");
  s=s.replaceAll("\\b\\d{1,2}[./-]\\d{1,2}(?:[./-]\\d{4})?\\b"," ");
  s=s.replaceAll("\\b(?:(?:in\\s+)?(?:data|ziua)\\s+(?:de\\s+)?|pe\\s+)\\d{1,2}\\b"," ");
  s=s.replaceAll("\\b\\d{1,2}\\s+(?:"+months+")(?:\\s+\\d{4})?\\b"," ");
  s=s.replaceAll("\\b(?:"+months+")\\b"," ");
  s=s.replaceAll("\\b(?:astazi|azi|maine|poimaine|luni|marti|miercuri|joi|vineri|sambata|duminica)\\b"," ");
  s=s.replaceAll("\\b(?:si\\s+)?salveaz(?:a)?(?:-?o)?\\b"," ");
  s=s.replaceAll("\\b(?:vreau|as|vrea|te|rog|sa|imi|mi|faci|fa|creezi|creeaza|creaza|pui|pune|adauga|o|un|notificare|memento|alarma|aminte(?:ste|sti)|aminteste|pentru|in|pe|la|data|de|ziua|ora|orele|care|cu|detaliile|detalii|anul)\\b"," ");
  s=s.replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
  if(s.isEmpty())return "Notificare";
  return shortTitle(s);
 }
 void onVoice(Intent data){
  if(data==null||title==null){Toast.makeText(a,"Recunoașterea vocală nu a întors rezultate.",Toast.LENGTH_LONG).show();return;}
  ArrayList<String> matches=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
  if(matches==null||matches.isEmpty()){Toast.makeText(a,"Nu am primit text de la microfon.",Toast.LENGTH_LONG).show();return;}
  // Android poate propune mai multe transcrieri: folosim prima cu dată+oră înțelese.
  String best=matches.get(0).trim();
  for(String candidate:matches){
   RomanianReminderParser.Result p=RomanianReminderParser.parse(candidate,System.currentTimeMillis(),TimeZone.getDefault());
   if(p.valid){processCommand(candidate);return;}
  }
  processCommand(best); // Afișează clar motivul, NU salvează o dată inventată.
 }
 boolean processCommand(String spoken){
  if(spoken==null||spoken.trim().isEmpty())return false;
  if(commandField!=null)commandField.setText(spoken);
  RomanianReminderParser.Result p=RomanianReminderParser.parse(spoken,System.currentTimeMillis(),TimeZone.getDefault());
  if(!p.valid){
   if(voiceFeedback!=null)voiceFeedback.setText("🎤 Am auzit: "+spoken+"\n❌ "+p.error);
   Toast.makeText(a,p.error,Toast.LENGTH_LONG).show();
   return false;
  }
  at=p.at;
  if(!p.details.isEmpty()){
   title.setText(p.title);
   description.setText(p.details);
  }else if(title.getText().toString().trim().isEmpty())title.setText("Memento");
  refresh();
  if(voiceFeedback!=null)voiceFeedback.setText("✅ Am înțeles: "+date(at)+", "+time(at)+". Salvez automat.");
  // Nu este necesar cuvântul «salvează» și nu există un al doilea buton obligatoriu.
  return save();
 }

}
