package com.saleh12340.quickdailynotebook

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var db: Db
    private lateinit var list: LinearLayout
    private lateinit var search: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        build()
    }

    private fun build() {
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(18))
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val heading = TextView(this).apply {
            text = "دفتر الملاحظات اليومية"
            textSize = 23f
            setTextColor(Color.rgb(38, 38, 38))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(heading, LinearLayout.LayoutParams(0, dp(54), 1f))
        val settings = TextView(this).apply {
            text = "⚙"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(70, 70, 70))
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        header.addView(settings, LinearLayout.LayoutParams(dp(50), dp(50)))
        root.addView(header)

        search = EditText(this).apply {
            hint = "بحث في الملاحظات"
            textSize = 16f
            setSingleLine(true)
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(Color.rgb(247, 247, 247), Color.rgb(225, 225, 225), 1, 12)
            setOnEditorActionListener { _, _, _ -> refresh(); false }
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply {
            setMargins(0, 0, 0, dp(12))
        })

        val newNote = TextView(this).apply {
            text = "+  ملاحظة جديدة"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(82, 67, 58), Color.TRANSPARENT, 0, 14)
            setOnClickListener { openNewNote() }
        }
        root.addView(newNote, LinearLayout.LayoutParams(-1, dp(50)).apply {
            setMargins(0, 0, 0, dp(14))
        })

        val scroll = ScrollView(this).apply { isFillViewport = true }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun openNewNote() {
        val id = db.newNote()
        startActivity(Intent(this, NoteActivity::class.java).putExtra("noteId", id))
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refresh()
    }

    private fun refresh() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val notes = db.notes(search.text.toString())

        if (notes.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لا توجد ملاحظات بعد\nأنشئ ملاحظتك الأولى من الزر أعلاه"
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(105, 105, 105))
                setPadding(dp(20), dp(60), dp(20), dp(60))
            }
            list.addView(empty)
            return
        }

        notes.forEachIndexed { index, row ->
            val id = row[0] as Long
            val noteTitle = (row[1] as String).ifBlank { "ملاحظة ${index + 1}" }
            val time = row[2] as Long
            val date = SimpleDateFormat("EEEE، d MMMM yyyy • HH:mm", Locale("ar")).format(Date(time))

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = rounded(Color.WHITE, Color.rgb(224, 224, 224), 1, 14)
                elevation = dp(2).toFloat()
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, NoteActivity::class.java).putExtra("noteId", id))
                }
                setOnLongClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("حذف الملاحظة؟")
                        .setNegativeButton("إلغاء", null)
                        .setPositiveButton("حذف") { _, _ -> db.delete(id); refresh() }
                        .show()
                    true
                }
            }

            val number = TextView(this).apply {
                text = "الملاحظة ${index + 1}"
                textSize = 12f
                setTextColor(Color.rgb(120, 120, 120))
                gravity = Gravity.RIGHT
            }
            val title = TextView(this).apply {
                text = noteTitle
                textSize = 19f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(35, 35, 35))
                maxLines = 2
                gravity = Gravity.RIGHT
            }
            val dateView = TextView(this).apply {
                text = date
                textSize = 13f
                setTextColor(Color.rgb(105, 105, 105))
                gravity = Gravity.RIGHT
            }
            card.addView(number)
            card.addView(title, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) })
            card.addView(dateView)
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) })
        }
    }

    private fun rounded(fill: Int, stroke: Int, width: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            if (width > 0) setStroke(dp(width), stroke)
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
