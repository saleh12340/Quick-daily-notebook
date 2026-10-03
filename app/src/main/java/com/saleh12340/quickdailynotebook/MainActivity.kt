package com.saleh12340.quickdailynotebook
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.view.*;import android.widget.*
class MainActivity:Activity(){
 lateinit var db:Db; lateinit var list:LinearLayout; lateinit var search:EditText
 override fun onCreate(b:Bundle?){super.onCreate(b);db=Db(this);build()}
 fun build(){window.decorView.layoutDirection=View.LAYOUT_DIRECTION_RTL;val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setPadding(18,18,18,18);root.setBackgroundColor(Color.rgb(247,241,231))
 val head=LinearLayout(this);head.gravity=Gravity.CENTER_VERTICAL;val title=TextView(this).apply{text="دفتر يومي";textSize=28f;setTextColor(Color.rgb(48,42,37));setTypeface(null,1)}
 head.addView(title,LinearLayout.LayoutParams(0,60,1f));val add=Button(this).apply{text="+ ملاحظة جديدة";setOnClickListener{val id=db.newNote();startActivity(Intent(this@MainActivity,NoteActivity::class.java).putExtra("id",id))}};head.addView(add);val settings=Button(this).apply{text="⚙";setOnClickListener{startActivity(Intent(this@MainActivity,SettingsActivity::class.java))}};head.addView(settings)
 root.addView(head);search=EditText(this).apply{hint="🔍 بحث في الملاحظات";textDirection=View.TEXT_DIRECTION_RTL;setSingleLine(true);setOnEditorActionListener{_,_,_->refresh();false}};root.addView(search)
 list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val sv=ScrollView(this);sv.addView(list);root.addView(sv,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);refresh()}
 override fun onResume(){super.onResume();if(::list.isInitialized)refresh()}
 fun refresh(){list.removeAllViews();db.notes(search.text.toString()).forEach{row->val id=row[0] as Long;val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,14,16,14);background=getDrawable(com.saleh12340.quickdailynotebook.R.drawable.paper_line)}
 val t=TextView(this).apply{text=row[1] as String;textSize=19f;setTextColor(Color.rgb(48,42,37));setTypeface(null,1)};val d=TextView(this).apply{text=java.text.SimpleDateFormat("EEEE، d MMMM yyyy • HH:mm",java.util.Locale("ar")).format(java.util.Date(row[2] as Long));textSize=13f}
 card.addView(t);card.addView(d);card.setOnClickListener{startActivity(Intent(this,NoteActivity::class.java).putExtra("id",id))};card.setOnLongClickListener{AlertDialog.Builder(this).setMessage("حذف الملاحظة؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف"){_,_->db.delete(id);refresh()}.show();true};val p=LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,10);list.addView(card,p)}}}