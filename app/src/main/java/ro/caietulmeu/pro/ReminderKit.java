package ro.caietulmeu.pro;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public final class ReminderKit {
 private ReminderKit() {}
 static final String STORE="caiet_mementouri_v1";
 static final String KEY="items";
 static final String CHANNEL="memento_alarm_silent_v2";
 static final String ALARM="ro.caietulmeu.pro.MEMENTO_ALARM";
 static final String STOP="ro.caietulmeu.pro.MEMENTO_STOP";
 static final int NOTIFICATION_ID=1107;

 // Mark a dismissed occurrence so pressing STOP before the alarm service starts
 // cannot result in playback starting a moment later.
 static boolean recentlyDismissed(Context c,int id){
  if(id==0)return false;
  android.content.SharedPreferences p=c.getSharedPreferences(STORE,0);
  return Math.abs(System.currentTimeMillis()-p.getLong("dismissed_"+id,0))<30000;
 }
 static void silence(Context c,int requestedId){
  SoundService active=SoundService.active;
  int playingId=active==null?0:active.id;
  long now=System.currentTimeMillis();
  android.content.SharedPreferences.Editor prefs=c.getSharedPreferences(STORE,0).edit();
  if(requestedId>0)prefs.putLong("dismissed_"+requestedId,now);
  if(playingId>0)prefs.putLong("dismissed_"+playingId,now);
  prefs.commit(); // Nu reporni dintr-un START sosit imediat după oprire.
  ReminderScreens.stopSharedPreview();
  if(active!=null){active.stopAudio();active.stopForeground(true);active.stopSelf();}
  c.stopService(new Intent(c,SoundService.class));
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(nm!=null)nm.cancel(NOTIFICATION_ID);
 }
 public static class StopReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c,Intent i){
   if(STOP.equals(i.getAction()))silence(c,i.getIntExtra("id",0));
  }
 }

 static JSONArray all(Context c) {
  try { return new JSONArray(c.getSharedPreferences(STORE,0).getString(KEY,"[]")); }
  catch(Exception e) { return new JSONArray(); }
 }
 static JSONObject find(Context c,int id) {
  JSONArray a=all(c);
  for(int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if(o!=null && o.optInt("id")==id) return o; }
  return null;
 }
 static void write(Context c,JSONArray a) { c.getSharedPreferences(STORE,0).edit().putString(KEY,a.toString()).apply(); }
 static void save(Context c,JSONObject o) {
  JSONArray a=all(c),b=new JSONArray();
  int id=o.optInt("id");
  for(int i=0;i<a.length();i++){ JSONObject e=a.optJSONObject(i); if(e!=null && e.optInt("id")!=id)b.put(e); }
  b.put(o);write(c,b);
 }
 static void delete(Context c,int id) {
  cancel(c,id);JSONArray a=all(c),b=new JSONArray();
  for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(e!=null&&e.optInt("id")!=id)b.put(e);}
  write(c,b);
 }
 static PendingIntent alarmIntent(Context c,int id,int flags) {
  Intent i=new Intent(c,Trigger.class).setAction(ALARM).setData(Uri.parse("memento://trigger/"+id));
  i.putExtra("id",id);
  return PendingIntent.getBroadcast(c,id,i,flags|PendingIntent.FLAG_IMMUTABLE);
 }
 static void cancel(Context c,int id) {
  AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  PendingIntent p=alarmIntent(c,id,PendingIntent.FLAG_NO_CREATE);
  if(m!=null&&p!=null)m.cancel(p);
 }
 static boolean exactAllowed(Context c) {
  AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  return m!=null && (Build.VERSION.SDK_INT<31 || m.canScheduleExactAlarms());
 }
 static boolean schedule(Context c,JSONObject o) {
  long at=o.optLong("at");int id=o.optInt("id");
  if(at<=System.currentTimeMillis()||!exactAllowed(c))return false;
  AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  PendingIntent alarm=alarmIntent(c,id,PendingIntent.FLAG_UPDATE_CURRENT);
  Intent show=new Intent(c,AlarmScreen.class).putExtra("id",id);
  PendingIntent showPi=PendingIntent.getActivity(c,id,show,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try {
   m.setAlarmClock(new AlarmManager.AlarmClockInfo(at,showPi),alarm);
   return true;
  }catch(SecurityException ex){return false;}
 }
 static long next(long at,String repeat,long now) {
  Calendar c=Calendar.getInstance();c.setTimeInMillis(at);
  int attempts=0;
  while(c.getTimeInMillis()<=now && attempts++<20000) {
   if("Zilnic".equals(repeat))c.add(Calendar.DAY_OF_YEAR,1);
   else if("Săptămânal".equals(repeat))c.add(Calendar.WEEK_OF_YEAR,1);
   else if("Lunar".equals(repeat))c.add(Calendar.MONTH,1);
   else return -1;
  }
  return c.getTimeInMillis()>now?c.getTimeInMillis():-1;
 }
 static void scheduleAll(Context c) {
  JSONArray a=all(c);boolean changed=false;
  for(int i=0;i<a.length();i++) {
   JSONObject o=a.optJSONObject(i);if(o==null)continue;
   long when=o.optLong("at");
   if(when<=System.currentTimeMillis()){
    long n=next(when,o.optString("repeat"),System.currentTimeMillis());
    if(n>0){try{o.put("at",n);changed=true;}catch(Exception ignored){}}
   }
   schedule(c,o);
  }
  if(changed)write(c,a);
 }
 static void askPermission(Activity a) {
  if(Build.VERSION.SDK_INT>=31&&!exactAllowed(a)){
   new AlertDialog.Builder(a).setTitle("Permisiune pentru alarme")
    .setMessage("Pentru ca Memento să sune la ora exactă, permite «Alarme și mementouri» în setările Android. Mementoul rămâne salvat, dar nu va suna până primește permisiunea.")
    .setPositiveButton("Deschide setările",(d,w)->{
     try {a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+a.getPackageName())));}
     catch(Exception e){Toast.makeText(a,"Activează accesul «Alarme și mementouri» din setări.",Toast.LENGTH_LONG).show();}
    }).setNegativeButton("Mai târziu",null).show();
  }
  if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
   a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},201);
 }
 public static class Reboot extends BroadcastReceiver {
  @Override public void onReceive(Context c,Intent i) {
   String action=i.getAction();
   if(Intent.ACTION_BOOT_COMPLETED.equals(action)||AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action))
    scheduleAll(c);
  }
 }
 public static class Trigger extends BroadcastReceiver {
  @Override public void onReceive(Context c,Intent i) {
   if(!ALARM.equals(i.getAction()))return;
   int id=i.getIntExtra("id",0);if(recentlyDismissed(c,id))return;
   JSONObject o=find(c,id);if(o==null)return;
   long newTime=next(o.optLong("at"),o.optString("repeat","O singură dată"),System.currentTimeMillis());
   if(newTime>0){try{o.put("at",newTime);save(c,o);schedule(c,o);}catch(Exception ignored){}}
   Intent s=new Intent(c,SoundService.class).setAction("START");
   s.putExtra("id",id);s.putExtra("title",o.optString("title","Memento"));
   s.putExtra("sound",o.optString("sound",""));
   try{if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);}
   catch(Exception e){android.util.Log.e("Memento","Alarm service failed",e);}
  }
 }
 public static class SoundService extends Service {
  static volatile SoundService active;
  MediaPlayer player;
  int id;
  @Override public void onCreate(){super.onCreate();active=this;}
  synchronized void stopAudio(){
   if(player!=null){
    try{if(player.isPlaying())player.stop();}catch(Exception ignored){}
    try{player.release();}catch(Exception ignored){}
    player=null;
   }
  }
  @Override public IBinder onBind(Intent i){return null;}
  @Override public int onStartCommand(Intent i,int flags,int startId) {
   if(i==null){stopSelf();return START_NOT_STICKY;}
   if(STOP.equals(i.getAction())){silence(this,i.getIntExtra("id",id));return START_NOT_STICKY;}
   id=i.getIntExtra("id",0);
   if(recentlyDismissed(this,id)){stopAudio();stopSelf();return START_NOT_STICKY;}
   String title=i.getStringExtra("title");if(title==null||title.isEmpty())title="Memento";
   String sound=i.getStringExtra("sound");
   ReminderScreens.stopSharedPreview();
   NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   NotificationChannel channel=new NotificationChannel(CHANNEL,"Alarme Memento",NotificationManager.IMPORTANCE_HIGH);
   channel.setDescription("Alarmă persistentă pentru programări");
   channel.setSound(null,null);channel.enableVibration(true);nm.createNotificationChannel(channel);
   Intent open=new Intent(this,AlarmScreen.class).putExtra("id",id).putExtra("title",title);
   open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   PendingIntent openPi=PendingIntent.getActivity(this,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Intent stop=new Intent(this,StopReceiver.class).setAction(STOP)
    .setData(Uri.parse("memento://stop/"+id)).putExtra("id",id);
   PendingIntent stopPi=PendingIntent.getBroadcast(this,100000+id,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification n=new Notification.Builder(this,CHANNEL)
     .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
     .setContentTitle("🔔 "+title).setContentText("Alarma sună până apeși OPREȘTE.")
     .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC)
     .setOngoing(true).setAutoCancel(false).setPriority(Notification.PRIORITY_HIGH)
     .setContentIntent(openPi).setFullScreenIntent(openPi,true)
     .addAction(android.R.drawable.ic_media_pause,"OPREȘTE",stopPi).build();
   startForeground(NOTIFICATION_ID,n);
   stopAudio();
   if(recentlyDismissed(this,id)){stopAudio();stopForeground(true);stopSelf();return START_NOT_STICKY;}
   try {
    Uri uri=sound!=null&&!sound.isEmpty()?Uri.parse(sound):RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    AudioAttributes attr=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    player=MediaPlayer.create(this,uri,null,attr,0);
    if(player==null)throw new IllegalStateException("Nu există sunetul ales");
    player.setLooping(true);player.start();
   }catch(Exception e){
    android.util.Log.w("Memento","Fallback alarm sound",e);
    try{
     Uri u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
     AudioAttributes backupAttr=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
     player=MediaPlayer.create(this,u,null,backupAttr,0);if(player!=null){player.setLooping(true);player.start();}
    }catch(Exception ignored){}
   }
   return START_STICKY;
  }
  @Override public void onDestroy(){
   stopAudio();
   if(active==this)active=null;
   super.onDestroy();
  }
 }
 public static class AlarmScreen extends Activity {
  @Override public void onCreate(Bundle b){
   super.onCreate(b);
   getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
   int id=getIntent().getIntExtra("id",0);
   JSONObject o=find(this,id);
   String name=getIntent().getStringExtra("title");
   if(name==null)name=o==null?"Memento":o.optString("title","Memento");
   LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);
   root.setPadding(28,30,28,30);root.setBackgroundColor(0xff13223b);
   TextView title=new TextView(this);title.setText("🔔\n"+name);title.setTextSize(28);title.setTextColor(Color.WHITE);title.setGravity(Gravity.CENTER);
   root.addView(title);
   TextView stop=new TextView(this);stop.setText("OPREȘTE ALARMA");stop.setTextSize(22);stop.setTextColor(Color.WHITE);
   stop.setBackgroundColor(0xffb62d42);stop.setGravity(Gravity.CENTER);stop.setPadding(28,30,28,30);
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,50,0,0);root.addView(stop,p);
   stop.setOnClickListener(v->{ReminderKit.silence(this,id);finish();});
   setContentView(root);
  }
 }
}
