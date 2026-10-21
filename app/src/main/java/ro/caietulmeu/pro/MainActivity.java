package ro.caietulmeu.pro;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
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
 int fg(){return dark?0xffF8E4C4:0xff493522;}
 int panel(){return dark?0xC04A382D:0xC6FFF6E3;}
 int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 static final String[] NOTE_COLORS={"Negru / natural","Albastru","Verde","Vișiniu","Mov"};
 static final int[] NOTE_LIGHT={0xff493522,0xff174D9A,0xff14653B,0xff912B49,0xff623B97};
 static final int[] NOTE_DARK={0xffF8E4C4,0xffA2C8FF,0xffA1E0BB,0xffFFADC2,0xffCEB3FF};
 static final String[] FONT_NAMES={"Clasic","Îngroșat","Elegant","Artistic","Scris de mână","Modern","Tip carte"};
 static final String[] FONT_FAMILIES={"sans-serif","sans-serif","serif","serif","casual","sans-serif-light","serif"};
 static final int[] FONT_STYLES={Typeface.NORMAL,Typeface.BOLD,Typeface.ITALIC,
  Typeface.BOLD_ITALIC,Typeface.NORMAL,Typeface.NORMAL,Typeface.NORMAL};
 int fontStyleIndex(){
  return Math.max(0,Math.min(FONT_NAMES.length-1,getPreferences(0).getInt("global_font_style",0)));
 }
 Typeface preferredTypeface(int index){
  return Typeface.create(FONT_FAMILIES[index],FONT_STYLES[index]);
 }
 Typeface preferredTypeface(){return preferredTypeface(fontStyleIndex());}
 void selectFontStyle(){
  LinearLayout chooser=new LinearLayout(this);chooser.setOrientation(LinearLayout.VERTICAL);
  chooser.setPadding(dp(12),dp(7),dp(12),dp(7));
  chooser.setBackground(new ParchmentDrawable(dark));
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(chooser);
  AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle("✒ Alege stilul de scris")
   .setView(scroll).setNegativeButton("Renunță",null).create();
  for(int i=0;i<FONT_NAMES.length;i++){
   final int index=i;
   TextView preview=new TextView(this);
   preview.setText((fontStyleIndex()==i?"✓  ":"")+FONT_NAMES[i]+"   •   Jurnalul meu");
   preview.setTypeface(preferredTypeface(i));
   preview.setTextSize(20);
   preview.setTextColor(noteColor());
   preview.setGravity(Gravity.CENTER_VERTICAL);
   preview.setMinHeight(dp(56));
   preview.setPadding(dp(15),dp(8),dp(12),dp(8));
   preview.setBackground(glassSurface(false,true,false));
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
   p.setMargins(dp(3),dp(4),dp(3),dp(4));chooser.addView(preview,p);
   preview.setOnClickListener(v->{
    boolean saved=getPreferences(0).edit().putInt("global_font_style",index).commit();
    if(saved){
     applyNoteAppearance();
     Toast.makeText(this,"✓ Stilul «"+FONT_NAMES[index]+"» rămâne memorat",Toast.LENGTH_LONG).show();
    }else Toast.makeText(this,"Nu am putut salva stilul",Toast.LENGTH_LONG).show();
    dialog.dismiss();
   });
  }
  dialog.show();
 }
 int noteColorIndex(){return Math.max(0,Math.min(NOTE_COLORS.length-1,getPreferences(0).getInt("global_note_color",0)));}
 int noteColor(){int i=noteColorIndex();return dark?NOTE_DARK[i]:NOTE_LIGHT[i];}
 void applyNoteAppearance(){
  if(body!=null){
   body.setTextColor(noteColor());body.setTextSize(fontSizeSp());
   body.setTypeface(preferredTypeface());
  }
  if(title!=null){
   title.setTextColor(noteColor());title.setTextSize(fontSizeSp());
   title.setTypeface(preferredTypeface());
  }
 }
 void selectNoteColor(){
  LinearLayout chooser=new LinearLayout(this);chooser.setOrientation(LinearLayout.VERTICAL);
  chooser.setPadding(dp(16),dp(8),dp(16),dp(8));chooser.setBackground(new ParchmentDrawable(dark));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("🎨 Culoarea permanentă a textului")
   .setView(chooser).setNegativeButton("Anulează",null).create();
  for(int i=0;i<NOTE_COLORS.length;i++){
   final int index=i;
   TextView swatch=new TextView(this);
   swatch.setText((noteColorIndex()==i?"✓  ":"●  ")+NOTE_COLORS[i]+"   Aa");
   swatch.setTextSize(19);swatch.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   swatch.setGravity(Gravity.CENTER_VERTICAL);swatch.setMinHeight(dp(52));
   swatch.setPadding(dp(18),dp(8),dp(8),dp(8));
   swatch.setTextColor(dark?NOTE_DARK[i]:NOTE_LIGHT[i]);
   swatch.setBackground(glassSurface(false,true,false));
   chooser.addView(swatch,new LinearLayout.LayoutParams(-1,-2));
   swatch.setOnClickListener(v->{
    boolean ok=getPreferences(0).edit().putInt("global_note_color",index).commit();
    if(ok){applyNoteAppearance();Toast.makeText(this,"✓ Culoarea "+NOTE_COLORS[index]+" a fost memorată",Toast.LENGTH_SHORT).show();}
    else Toast.makeText(this,"Nu s-a putut salva culoarea",Toast.LENGTH_LONG).show();
    dialog.dismiss();
   });
  }
  dialog.show();
 }
 void section(LinearLayout parent,String heading){
  TextView h=new TextView(this);h.setText(heading);h.setTextColor(fg());h.setTextSize(17);
  h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(dp(12),dp(15),dp(9),dp(5));
  parent.addView(h,new LinearLayout.LayoutParams(-1,-2));
 }
 TextView tile(String name,LinearLayout row,Runnable action){
  TextView v=button(name,row,action);
  LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();
  p.width=0;p.height=-2;p.weight=1;v.setLayoutParams(p);
  v.setMinHeight(dp(68));
  v.setGravity(Gravity.CENTER);
  v.setMaxLines(3);
  return v;
 }
 // Grilă de 2 coloane: butoane identice ca lățime și înălțime.
 TextView homeTile(String text,LinearLayout row,Runnable action){
  TextView v=tile(text,row,action);
  LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();
  p.width=0;p.height=dp(84);p.weight=1;
  p.setMargins(dp(5),dp(5),dp(5),dp(5));v.setLayoutParams(p);
  v.setMaxLines(2);v.setGravity(Gravity.CENTER);
  if(Build.VERSION.SDK_INT>=26)
   v.setAutoSizeTextTypeUniformWithConfiguration(12,16,1,
    android.util.TypedValue.COMPLEX_UNIT_SP);
  return v;
 }
 // FONT GLOBAL: același pentru notițele vechi, noi și după repornire.
 float fontSizeSp(){return Math.max(12f,Math.min(40f,getPreferences(0).getFloat("global_font_sp",19f)));}
 void changeFontSize(int delta){
  float newSize=Math.max(12f,Math.min(40f,fontSizeSp()+delta));
  boolean saved=getPreferences(0).edit().putFloat("global_font_sp",newSize).commit();
  applyNoteAppearance();
  Toast.makeText(this,(saved?"✓ Font permanent: ":"Eroare la salvarea fontului: ")+(int)newSize+" sp",Toast.LENGTH_SHORT).show();
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);dark=getPreferences(0).getBoolean("dark",false);try{notes=new JSONArray(getPreferences(0).getString("notes","[]"));}catch(Exception e){}home();}
 // Sticlă 3D bombată pentru TOATE butoanele aplicației.
 Drawable glassSurface(boolean danger,boolean primary,boolean positive){
  return glassSurface(danger,primary,positive,false);
 }
 Drawable glassSurface(boolean danger,boolean primary,boolean positive,boolean pressed){
  int radius=dp(primary?23:17);
  { // Toate butoanele au acum volum convex de sticlă, nu doar cele principale.
   int[] body=dark
    ?(primary?new int[]{0xE8D9E7F5,0xD68FAED4,0xCF527AAE,0xE144608D,0xE7233E67}
      :new int[]{0xF9F9F2E7,0xDBC8C3B6,0xD1A99E8D,0xE48C7B65,0xE45D554B})
    :(primary?new int[]{0xF9FFFFFF,0xE5DEEBFF,0xD5A2C4ED,0xE87E9EC8,0xF34A689A}
      :new int[]{0xFCFFFFFF,0xF3FFF9E9,0xDDFAE8C8,0xEED8BC8C,0xF2A58E69});
   if(danger)body=dark?new int[]{0xEFFFD9E1,0xEFAF7C8D,0xE67F415A,0xE54A263E,0xE2372335}
    :new int[]{0xFAFFFFFF,0xEAFDE1E7,0xF0DDA1B1,0xE8C97086,0xE99F455C};
   if(positive)body=dark?new int[]{0xF7F1FFED,0xE5A7DAC2,0xD55A9A85,0xDD31725B,0xF01D4E43}
    :new int[]{0xFAFFFFFF,0xF3E4FFEF,0xE0A8D9C3,0xED67A48D,0xEE377A63};
   GradientDrawable under=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
    dark?new int[]{0x57241B15,0x8E10100F}:new int[]{0x50643A27,0xAA5A392C});
   under.setCornerRadius(radius);
   GradientDrawable convex=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,body);
   convex.setCornerRadius(radius);convex.setStroke(dp(2),
    dark?0xF9E1ECFF:0xFFEFF8FF);
   GradientDrawable specular=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
    new int[]{0xBFFFFFFF,0x84FFFFFF,0x21FFFFFF,0x00FFFFFF});
   specular.setCornerRadius(dp(22));
   GradientDrawable lowerGlint=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
    new int[]{0x87FFFFFF,0x25FFFFFF,0x00FFFFFF});
   lowerGlint.setCornerRadius(radius);
   GradientDrawable edge=new GradientDrawable();
   edge.setColor(Color.TRANSPARENT);edge.setCornerRadius(radius);
   edge.setStroke(dp(1),dark?0xB9D2EEFF:0xCBA7D4FF);
   LayerDrawable glass=new LayerDrawable(new Drawable[]{under,convex,specular,lowerGlint,edge});
   glass.setLayerInset(1,dp(1),dp(1),dp(1),pressed?dp(1):dp(4));
   glass.setLayerInset(2,dp(9),dp(4),dp(9),pressed?dp(9):dp(13));
   glass.setLayerInset(3,dp(5),dp(1),dp(5),pressed?dp(3):dp(6));
   glass.setLayerInset(4,dp(1),dp(1),dp(1),pressed?dp(1):dp(4));
   return glass;
  }
 }
 TextView button(String text,LinearLayout parent,Runnable run){
  String lower=text.toLowerCase(new Locale("ro","RO"));
  boolean danger=lower.contains("oprește")||lower.contains("opreste")||lower.contains("șterge")||lower.contains("sterge")||lower.contains("🗑");
  boolean primary=lower.contains("salvează")||lower.contains("salveaza")||
   lower.contains("notiță nouă")||lower.contains("memento vocal")||
   lower.contains("nu uita")||lower.contains("memento nou")||
   lower.contains("spune comanda")||lower.contains("execută textul");
  boolean positive=!danger&&!primary&&(lower.contains("test alarm")||
   lower.contains("probă alarm")||lower.contains("ascultă"));
  int ink=danger?(dark?0xffFFE0E9:0xff81223C)
   :primary?(dark?0xffF9FCFF:0xff173F7B)
   :positive?(dark?0xffDDFCF0:0xff12593D):fg();
  TextView v=new TextView(this);v.setText(text);v.setTextSize(15.5f);
  v.setTextColor(ink);v.setGravity(Gravity.CENTER);
  v.setTypeface(Typeface.DEFAULT,(primary||danger)?Typeface.BOLD:Typeface.NORMAL);
  v.setPadding(dp(13),dp(13),dp(13),dp(13));v.setMinHeight(dp(52));
  boolean raised=true;
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{android.R.attr.state_pressed},
   glassSurface(danger,primary,positive,true));
  states.addState(new int[]{},glassSurface(danger,primary,positive,false));
  v.setBackground(new RippleDrawable(
   ColorStateList.valueOf(dark?0x57FFFFFF:0x3FFFFFFF),states,null));
  if(raised){
   v.setElevation(dp(5));
   StateListAnimator pressAnimation=new StateListAnimator();
   pressAnimation.addState(new int[]{android.R.attr.state_pressed},
    ObjectAnimator.ofFloat(v,"translationZ",0f).setDuration(85));
   pressAnimation.addState(new int[]{},
    ObjectAnimator.ofFloat(v,"translationZ",dp(3)).setDuration(140));
   v.setStateListAnimator(pressAnimation);
  }else{v.setElevation(0f);v.setStateListAnimator(null);}
  v.setClickable(true);v.setFocusable(true);v.setContentDescription(text);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(parent.getOrientation()==LinearLayout.VERTICAL?-1:-2,-2);
  p.setMargins(dp(4),dp(5),dp(4),dp(5));parent.addView(v,p);
  v.setOnClickListener(w->run.run());
  return v;
 }
 LinearLayout vertical(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(12),dp(15),dp(12),dp(10));
  l.setBackground(new ParchmentDrawable(dark));setContentView(l);return l;
 }
 LinearLayout line(LinearLayout parent){LinearLayout l=new LinearLayout(this);l.setOrientation(0);HorizontalScrollView sc=new HorizontalScrollView(this);sc.addView(l);parent.addView(sc);return l;}
 EditText field(String hint,boolean multiline){
  EditText e=new EditText(this);e.setHint(hint);e.setTextColor(fg());e.setHintTextColor(0xff888899);e.setTextSize(19);e.setPadding(15,14,15,14);e.setBackgroundColor(panel());
  if(multiline){e.setGravity(48);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);}else e.setSingleLine(true);
  GradientDrawable inputGlass=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   dark?new int[]{0xD0504137,0xC0382C25}:new int[]{0xF9FFF9EB,0xEAF9E6C9});
  inputGlass.setCornerRadius(dp(15));
  inputGlass.setStroke(dp(1),dark?0xA5B39C7F:0xB3B89A69);
  e.setBackground(inputGlass);
  e.setPrivateImeOptions("nm");
  return e;
 }
 void persist(){getPreferences(0).edit().putString("notes",notes.toString()).putBoolean("dark",dark).apply();}
 void home(){
  current=-1;remindersActive=false;
  if(reminders!=null)reminders.stopPreview();root=vertical();
  TextView brand=new TextView(this);brand.setText("🧠 Nu Uita");
  brand.setTextSize(28);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  brand.setTextColor(fg());brand.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC));
  brand.setPadding(dp(10),dp(5),dp(8),dp(0));root.addView(brand);
  TextView subtitle=new TextView(this);subtitle.setText("Notițe, calendar și mementouri într-un singur loc");
  subtitle.setTextSize(13);subtitle.setTextColor(dark?0xffDECEB1:0xff6F573B);
  subtitle.setPadding(dp(11),0,dp(9),dp(6));root.addView(subtitle);
  LinearLayout first=new LinearLayout(this);first.setOrientation(LinearLayout.HORIZONTAL);root.addView(first);
  homeTile("➕ Notiță nouă",first,()->editor(-1));
  homeTile("🎙️ Memento vocal",first,()->{mementouri().form(null,"","");mementouri().dictate();});
  LinearLayout second=new LinearLayout(this);second.setOrientation(LinearLayout.HORIZONTAL);root.addView(second);
  homeTile("🧠 Mementouri",second,()->mementouri().list());
  homeTile("📅 Calendar",second,()->mementouri().showCalendar());
  LinearLayout third=new LinearLayout(this);third.setOrientation(LinearLayout.HORIZONTAL);root.addView(third);
  homeTile("🔇 Oprește sunetul",third,()->ReminderKit.stopFromUi(this));
  homeTile("🌙 / ☀️ Temă",third,()->{dark=!dark;persist();home();});
  section(root,"📚 Notițele mele");
  search=field("🔎 Caută în notițe",false);root.addView(search);
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.addView(list);
  root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  Runnable render=()->{
   list.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT);
   for(int i=notes.length()-1;i>=0;i--){
    JSONObject o=notes.optJSONObject(i);if(o==null)continue;
    String t=o.optString("title","Fără titlu"),b=o.optString("body","");
    if(!(t+" "+b).toLowerCase(Locale.ROOT).contains(q))continue;
    final int index=i;
    TextView noteRow=button("📝 "+t+"\n"+b.substring(0,Math.min(75,b.length())),list,()->editor(index));
    noteRow.setTextSize(fontSizeSp());noteRow.setTextColor(noteColor());
    noteRow.setTypeface(preferredTypeface());noteRow.setGravity(Gravity.CENTER_VERTICAL);
   }
  };
  search.addTextChangedListener(new TextWatcher(){
   public void beforeTextChanged(CharSequence s,int st,int c,int a){}
   public void onTextChanged(CharSequence s,int st,int before,int count){render.run();}
   public void afterTextChanged(Editable e){}
  });
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
  section(root,"✏️ Aspect text • salvat automat pentru toate notițele");
  LinearLayout fontTools=new LinearLayout(this);fontTools.setOrientation(LinearLayout.HORIZONTAL);root.addView(fontTools);
  tile("A+ Mărește",fontTools,()->changeFontSize(2));
  tile("A− Micșorează",fontTools,()->changeFontSize(-2));
  LinearLayout styleTools=new LinearLayout(this);styleTools.setOrientation(LinearLayout.HORIZONTAL);root.addView(styleTools);
  tile("✒ Stil de scris",styleTools,()->selectFontStyle());
  tile("🎨 Culoare",styleTools,()->selectNoteColor());
  body=field("Scrie sau dictează aici…",true);
  applyNoteAppearance();
  root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  if(index>=0){JSONObject o=notes.optJSONObject(index);if(o!=null){title.setText(o.optString("title"));body.setText(o.optString("body"));}}
  before=body.getText().toString();
  body.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(Editable e){if(!loading&&!before.equals(e.toString())){undo.push(before);if(undo.size()>50)undo.remove(0);before=e.toString();redo.clear();status.setText("Modificări în curs…");}}});
 }
 void replace(String s){loading=true;body.setText(s);BaseInputConnection.removeComposingSpans(body.getText());body.setSelection(s.length());before=s;loading=false;}
 void save(){if(body==null||title==null)return;try{JSONObject o=new JSONObject();o.put("title",title.getText().toString().trim().isEmpty()?"Fără titlu":title.getText().toString());o.put("body",body.getText().toString());o.put("time",System.currentTimeMillis());if(current<0){notes.put(o);current=notes.length()-1;}else notes.put(current,o);persist();status.setText("✓ Salvat pe telefon");}catch(Exception e){Toast.makeText(this,"Eroare la salvare",Toast.LENGTH_SHORT).show();}}
 void voice(){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ro-RO");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ro-RO");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Dictează în română");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);try{startActivityForResult(i,VOICE);}catch(Exception e){Toast.makeText(this,"Activează recunoașterea vocală Google",Toast.LENGTH_LONG).show();}}
 @Override protected void onActivityResult(int req,int res,Intent data){
  super.onActivityResult(req,res,data);
  if(req==78){
   if(res==RESULT_OK&&data!=null&&reminders!=null)reminders.onVoice(data);
   else Toast.makeText(this,"Dictarea a fost anulată sau nu a recunoscut vorbele. Încearcă din nou.",Toast.LENGTH_LONG).show();
   return;
  }
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