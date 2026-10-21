package ro.caietulmeu.pro;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.RecognizerIntent;
import android.text.*;
import android.view.inputmethod.BaseInputConnection;
import android.text.style.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
 final int VOICE=77;
 LinearLayout root;
 EditText title,body,search;
 TextView status;
 boolean dark=false,loading=false;
 int current=-1;
 JSONArray notes=new JSONArray();
 ReminderScreens reminders;
 boolean remindersActive=false;
 ReminderScreens mementouri(){if(reminders==null)reminders=new ReminderScreens(this);return reminders;}
 String before="";
 Stack<String> undo=new Stack<>(),redo=new Stack<>();
 int bg(){return dark?0xff151b29:0xfff2f5ff;}
 int fg(){return dark?Color.WHITE:0xff182238;}
 int panel(){return dark?0xff283247:Color.WHITE;}
 @Override public void onCreate(Bundle b){super.onCreate(b);dark=getPreferences(0).getBoolean("dark",false);try{notes=new JSONArray(getPreferences(0).getString("notes","[]"));}catch(Exception e){}home();}
 TextView button(String text,LinearLayout parent,Runnable run){
  TextView v=new TextView(this);v.setText(text);v.setTextSize(16);v.setTextColor(fg());v.setGravity(17);v.setPadding(17,19,17,19);
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(panel());d.setCornerRadius(22);v.setBackground(d);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(4,5,4,5);parent.addView(v,p);v.setOnClickListener(w->run.run());return v;
 }
 LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(12,15,12,10);l.setBackgroundColor(bg());setContentView(l);return l;}
 LinearLayout line(LinearLayout parent){LinearLayout l=new LinearLayout(this);l.setOrientation(0);HorizontalScrollView sc=new HorizontalScrollView(this);sc.addView(l);parent.addView(sc);return l;}
 EditText field(String hint,boolean multiline){
  EditText e=new EditText(this);e.setHint(hint);e.setTextColor(fg());e.setHintTextColor(0xff888899);e.setTextSize(19);e.setPadding(15,14,15,14);e.setBackgroundColor(panel());
  if(multiline){e.setGravity(48);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);}else e.setSingleLine(true);
  e.setPrivateImeOptions("nm");
  return e;
 }
 void persist(){getPreferences(0).edit().putString("notes",notes.toString()).putBoolean("dark",dark).apply();}
 void home(){
  current=-1;remindersActive=false;if(reminders!=null)reminders.stopPreview();root=vertical();LinearLayout row=line(root);
  button("➕ NOTIȚĂ NOUĂ",row,()->editor(-1));
  button("🔔 MEMENTO",row,()->mementouri().list());
  button("🎙️ MEMENTO VOCAL",row,()->{mementouri().form(null,"","");mementouri().dictate();});
  button("📅 CALENDAR",row,()->mementouri().showCalendar());
  button("🌙 / ☀️",row,()->{dark=!dark;persist();home();});
  search=field("🔎 Caută notițe",false);root.addView(search);
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);ScrollView sc=new ScrollView(this);sc.addView(list);root.addView(sc);
  Runnable render=()->{list.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT);
   for(int i=notes.length()-1;i>=0;i--){JSONObject o=notes.optJSONObject(i);if(o==null)continue;
    String t=o.optString("title","Fără titlu"),b=o.optString("body","");
    if(!(t+" "+b).toLowerCase(Locale.ROOT).contains(q))continue;
    final int index=i;button("📝 "+t+"\n"+b.substring(0,Math.min(75,b.length())),list,()->editor(index));
   }
  };
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){render.run();}public void afterTextChanged(Editable e){}});
  render.run();
 }
 void editor(int index){
  current=index;remindersActive=false;root=vertical();LinearLayout top=line(root);
  button("‹ Înapoi",top,()->{save();home();});
  button("💾 SALVEAZĂ",top,()->save());
  button("🔔 MEMENTO",top,()->{save();mementouri().form(null,title.getText().toString(),body.getText().toString());});
  button("🗑️ ȘTERGE",top,()->new AlertDialog.Builder(this).setMessage("Ștergi notița?").setNegativeButton("Nu",null).setPositiveButton("Da",(d,w)->{if(current>=0){JSONArray a=new JSONArray();for(int i=0;i<notes.length();i++)if(i!=current)a.put(notes.opt(i));notes=a;persist();}home();}).show());
  status=new TextView(this);status.setTextColor(fg());status.setText("Modificări în curs…");root.addView(status);
  title=field("Titlul notiței",false);root.addView(title);
  LinearLayout controls=line(root);
  button("🎤 MICROFON",controls,()->voice());
  button("↶ ANULEAZĂ",controls,()->{if(!undo.empty()){redo.push(body.getText().toString());replace(undo.pop());}});
  button("↷ REFĂ",controls,()->{if(!redo.empty()){undo.push(body.getText().toString());replace(redo.pop());}});
  LinearLayout controls2=line(root);
  button("📋 COPIAZĂ",controls2,()->clipboard(false));
  button("📌 LIPEȘTE",controls2,()->paste());
  button("✂️ DECUPEAZĂ",controls2,()->clipboard(true));
  button("B",controls2,()->format(new StyleSpan(Typeface.BOLD)));
  button("I",controls2,()->format(new StyleSpan(Typeface.ITALIC)));
  button("U",controls2,()->format(new UnderlineSpan()));
  button("🔤 FONT +",controls2,()->body.setTextSize(body.getTextSize()/getResources().getDisplayMetrics().scaledDensity+2));
  button("🔡 FONT −",controls2,()->body.setTextSize(Math.max(12,body.getTextSize()/getResources().getDisplayMetrics().scaledDensity-2)));
  body=field("Scrie sau dictează aici…",true);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  if(index>=0){JSONObject o=notes.optJSONObject(index);if(o!=null){title.setText(o.optString("title"));body.setText(o.optString("body"));}}
  before=body.getText().toString();
  body.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(Editable e){if(!loading&&!before.equals(e.toString())){undo.push(before);if(undo.size()>50)undo.remove(0);before=e.toString();redo.clear();status.setText("Modificări în curs…");}}});
 }
 void replace(String s){loading=true;body.setText(s);BaseInputConnection.removeComposingSpans(body.getText());body.setSelection(s.length());before=s;loading=false;}
 void save(){if(body==null||title==null)return;try{JSONObject o=new JSONObject();o.put("title",title.getText().toString().trim().isEmpty()?"Fără titlu":title.getText().toString());o.put("body",body.getText().toString());o.put("time",System.currentTimeMillis());if(current<0){notes.put(o);current=notes.length()-1;}else notes.put(current,o);persist();status.setText("✓ Salvat pe telefon");}catch(Exception e){Toast.makeText(this,"Eroare la salvare",Toast.LENGTH_SHORT).show();}}
 void voice(){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ro-RO");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ro-RO");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Dictează în română");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);try{startActivityForResult(i,VOICE);}catch(Exception e){Toast.makeText(this,"Activează recunoașterea vocală Google",Toast.LENGTH_LONG).show();}}
 @Override protected void onActivityResult(int req,int res,Intent data){
  super.onActivityResult(req,res,data);
  if(res==RESULT_OK && req==78 && reminders!=null){reminders.onVoice(data);return;}
  if(res==RESULT_OK && req==79 && reminders!=null){reminders.onAudio(data);return;}
  if(req==VOICE&&res==RESULT_OK&&data!=null&&body!=null){
   ArrayList<String> matches=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
   if(matches!=null&&!matches.isEmpty()){
    String words=matches.get(0);
    String normalized=ReminderScreens.normalized(words);
    if(normalized.matches("(?s).*\\b(?:memento|notificare|alarma|aminteste|programeaza)\\b.*")
     && normalized.matches("(?s).*\\b(?:ora|orele)\\b.*")){
     String initialTitle=title.getText().toString(),initialBody=body.getText().toString();
     save();mementouri().form(null,initialTitle,initialBody);mementouri().processCommand(words);return;
    }int a=Math.max(0,body.getSelectionStart()),b=Math.max(0,body.getSelectionEnd());
    Editable e=body.getText();int start=Math.min(a,b),end=Math.max(a,b);
    String prefix=start>0&&!Character.isWhitespace(e.charAt(start-1))?" ":"";
    e.replace(start,end,prefix+words);BaseInputConnection.removeComposingSpans(e);body.setSelection(start+prefix.length()+words.length());
   }
  }
 }
 void format(Object span){int a=body.getSelectionStart(),b=body.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Selectează mai întâi textul",Toast.LENGTH_SHORT).show();return;}body.getText().setSpan(span,a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
 void clipboard(boolean cut){int a=body.getSelectionStart(),b=body.getSelectionEnd();if(a<0||a==b){Toast.makeText(this,"Selectează textul",Toast.LENGTH_SHORT).show();return;}int x=Math.min(a,b),y=Math.max(a,b);((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("text",body.getText().subSequence(x,y).toString()));if(cut)body.getText().delete(x,y);}
 void paste(){android.content.ClipboardManager m=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(!m.hasPrimaryClip())return;CharSequence c=m.getPrimaryClip().getItemAt(0).coerceToText(this);int a=Math.max(0,body.getSelectionStart());body.getText().insert(a,c.toString());}
 @Override protected void onResume(){super.onResume();ReminderKit.scheduleAll(this);}
 @Override public void onBackPressed(){if(remindersActive){home();}else if(current>=0){save();home();}else super.onBackPressed();}
}