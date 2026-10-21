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
 EditText title,description;
 TextView dateLabel,timeLabel,soundLabel,repeatLabel;
 long at;
 String sound="",soundName="Sunetul implicit al telefonului",repeat="O singură dată";
 JSONObject editing;
 Calendar calendar=Calendar.getInstance();
 Ringtone preview;
 final SimpleDateFormat dateFormat=new SimpleDateFormat("EEEE, dd MMMM yyyy",new Locale("ro","RO"));
 final SimpleDateFormat timeFormat=new SimpleDateFormat("HH:mm",Locale.ROOT);
 ReminderScreens(MainActivity activity){a=activity;}
 void enter(){a.current=-1;a.remindersActive=true;a.root=a.vertical();}
 void stopPreview(){if(preview!=null){preview.stop();preview=null;}}
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
  navigation("🔔 Mementourile mele");
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
  if(existing!=null){at=existing.optLong("at");sound=existing.optString("sound","");soundName=existing.optString("soundName","Sunetul implicit");repeat=existing.optString("repeat","O singură dată");}
  else{selected.add(Calendar.HOUR_OF_DAY,1);selected.set(Calendar.MINUTE,0);selected.set(Calendar.SECOND,0);at=selected.getTimeInMillis();sound="";soundName="Sunetul implicit al telefonului";repeat="O singură dată";}
  LinearLayout nav=a.line(a.root);
  a.button("‹ Înapoi",nav,()->list());
  navigation(existing==null?"🔔 Memento nou":"🔔 Editează memento");
  ScrollView scroll=new ScrollView(a);LinearLayout content=new LinearLayout(a);content.setPadding(6,8,6,20);
  content.setOrientation(1);scroll.addView(content);a.root.addView(scroll);
  title=a.field("Ce trebuie să îți amintești?",false);title.setText(existing==null?initialTitle:existing.optString("title"));
  content.addView(title);
  description=a.field("Detalii (opțional)",true);description.setMinLines(2);description.setMaxLines(4);
  description.setText(existing==null?initialBody:existing.optString("details"));content.addView(description);
  LinearLayout speak=a.line(content);
  a.button("🎙️ Spune prin voce",speak,()->dictate());
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
  repeatLabel.setText("🔁 Repetare: "+repeat);soundLabel.setText("🎵 Sunet: "+soundName);
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
   sound=uris.get(i);soundName=names.get(i);refresh();testSound();
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
  refresh();testSound();
 }
 void testSound(){
  stopPreview();
  try{
   Uri uri=sound.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(sound);
   preview=RingtoneManager.getRingtone(a,uri);
   if(preview!=null){if(Build.VERSION.SDK_INT>=28)preview.setLooping(false);preview.play();}
   else Toast.makeText(a,"Sunet indisponibil",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(a,"Nu pot reda acest sunet",Toast.LENGTH_SHORT).show();}
 }
 void save(){
  String name=title.getText().toString().trim();
  if(name.isEmpty()){title.setError("Scrie motivul mementoului");return;}
  if(at<=System.currentTimeMillis()){Toast.makeText(a,"Alege o dată și o oră din viitor.",Toast.LENGTH_LONG).show();return;}
  try{
   JSONObject o=editing==null?new JSONObject():editing;
   if(editing==null)o.put("id",(int)(System.currentTimeMillis()&0x7fffffff));
   o.put("title",name);o.put("details",description.getText().toString());
   o.put("at",at);o.put("repeat",repeat);o.put("sound",sound);o.put("soundName",soundName);
   int id=o.optInt("id");ReminderKit.cancel(a,id);ReminderKit.save(a,o);
   boolean scheduled=ReminderKit.schedule(a,o);
   stopPreview();list();
   Toast.makeText(a,scheduled?"✓ Memento și alarmă programate":"Memento salvat. Acordă permisiunea de alarmă pentru a suna!",Toast.LENGTH_LONG).show();
   ReminderKit.askPermission(a);
  }catch(Exception e){Toast.makeText(a,"Eroare la salvarea mementoului",Toast.LENGTH_LONG).show();}
 }
 void dictate(){
  Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ro-RO");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Spune data, ora și ce dorești să-ți amintești");
  try{a.startActivityForResult(i,78);}catch(Exception e){Toast.makeText(a,"Dictarea vocală nu este disponibilă",Toast.LENGTH_LONG).show();}
 }
 void onVoice(Intent data){
  if(data==null||title==null)return;
  ArrayList<String> matches=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
  if(matches==null||matches.isEmpty())return;
  String speech=matches.get(0).trim(),s=speech.toLowerCase(new Locale("ro","RO"));
  String clean=speech.replaceFirst("(?iu)^amintește-mi\\s*(?:să\\s*)?","");
  Matcher event=Pattern.compile("(?iu)\\bsă\\s+(.+)$").matcher(clean);
  title.setText(event.find()?event.group(1).trim():clean);
  Calendar c=Calendar.getInstance();c.setTimeInMillis(at);
  Matcher hour=Pattern.compile("(?iu)(?:la\\s+ora|ora|la)\\s*(\\d{1,2})(?:[:.](\\d{2}))?").matcher(s);
  boolean timeFound=false;
  if(hour.find()){
   int h=Integer.parseInt(hour.group(1));int m=hour.group(2)==null?0:Integer.parseInt(hour.group(2));
   if(h<=23&&m<=59){c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);timeFound=true;}
  }
  if(s.contains("poimâine")||s.contains("poimaine")){Calendar today=Calendar.getInstance();c.set(today.get(Calendar.YEAR),today.get(Calendar.MONTH),today.get(Calendar.DAY_OF_MONTH));c.add(Calendar.DAY_OF_YEAR,2);}
  else if(s.contains("mâine")||s.contains("maine")){Calendar today=Calendar.getInstance();c.set(today.get(Calendar.YEAR),today.get(Calendar.MONTH),today.get(Calendar.DAY_OF_MONTH));c.add(Calendar.DAY_OF_YEAR,1);}
  else{
   String[] days={"luni","marți","miercuri","joi","vineri","sâmbătă","duminică"};
   int[] values={Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY,Calendar.SUNDAY};
   boolean found=false;
   for(int i=0;i<days.length;i++)if(s.contains(days[i])||(i==1&&s.contains("marti"))||(i==5&&s.contains("sambata"))){
    Calendar today=Calendar.getInstance();c.set(today.get(Calendar.YEAR),today.get(Calendar.MONTH),today.get(Calendar.DAY_OF_MONTH));
    int delta=(values[i]-c.get(Calendar.DAY_OF_WEEK)+7)%7;
    if(delta==0&&c.getTimeInMillis()<=System.currentTimeMillis())delta=7;
    c.add(Calendar.DAY_OF_YEAR,delta);found=true;break;
   }
   Matcher numeric=Pattern.compile("(?iu)(?:pe\\s+)?(?:data\\s+de\\s+)?(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{4}))?").matcher(s);
   if(numeric.find()){
    int d=Integer.parseInt(numeric.group(1)),mon=Integer.parseInt(numeric.group(2)),year=numeric.group(3)==null?Calendar.getInstance().get(Calendar.YEAR):Integer.parseInt(numeric.group(3));
    if(d>=1&&d<=31&&mon>=1&&mon<=12){c.set(year,mon-1,d);if(numeric.group(3)==null&&c.getTimeInMillis()<System.currentTimeMillis())c.add(Calendar.YEAR,1);found=true;}
   }
   String[] months={"ianuarie","februarie","martie","aprilie","mai","iunie","iulie","august","septembrie","octombrie","noiembrie","decembrie"};
   for(int i=0;i<months.length;i++){
    Matcher m=Pattern.compile("(?iu)(\\d{1,2})\\s+"+months[i]).matcher(s);
    if(m.find()){int d=Integer.parseInt(m.group(1));if(d>=1&&d<=31){c.set(Calendar.MONTH,i);c.set(Calendar.DAY_OF_MONTH,d);
     if(c.getTimeInMillis()<System.currentTimeMillis())c.add(Calendar.YEAR,1);found=true;}break;}
   }
   if(!found && s.matches("(?iu).*\\b(?:pe\\s+)?(?:data\\s+de\\s+)(\\d{1,2}).*")){
    Matcher m=Pattern.compile("(?iu)data\\s+de\\s+(\\d{1,2})").matcher(s);
    if(m.find()){int d=Integer.parseInt(m.group(1));if(d>0&&d<=31){c.set(Calendar.DAY_OF_MONTH,Math.min(d,c.getActualMaximum(Calendar.DAY_OF_MONTH)));if(c.getTimeInMillis()<System.currentTimeMillis())c.add(Calendar.MONTH,1);}}
   }
  }
  at=c.getTimeInMillis();refresh();
  Toast.makeText(a,"Verifică data și ora înainte să salvezi.",Toast.LENGTH_LONG).show();
 }
}
