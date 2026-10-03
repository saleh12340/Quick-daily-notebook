package com.saleh12340.quickdailynotebook
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*
class SettingsActivity:Activity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);window.decorView.layoutDirection=View.LAYOUT_DIRECTION_RTL;val p=getSharedPreferences("settings",0);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20);setBackgroundColor(Color.rgb(247,241,231))}
 root.addView(TextView(this).apply{text="الإعدادات";textSize=28f;setTextColor(Color.rgb(48,42,37))})
 root.addView(Switch(this).apply{text="الوضع الداكن";isChecked=p.getBoolean("dark",false);setOnCheckedChangeListener{_,v->p.edit().putBoolean("dark",v).apply();Toast.makeText(this@SettingsActivity,"سيطبق المظهر عند فتح التطبيق مجدداً",Toast.LENGTH_SHORT).show()}})
 root.addView(TextView(this).apply{text="حجم النص";textSize=17f});root.addView(SeekBar(this).apply{max=3;progress=p.getInt("font",1);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,v:Int,c:Boolean){if(c)p.edit().putInt("font",v).apply()};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){} })})
 root.addView(TextView(this).apply{text="الطباعة الحرارية: 58mm — اختر الطابعة المقترنة من شاشة الطباعة";textSize=16f;setPadding(0,25,0,25)});setContentView(root)}
}