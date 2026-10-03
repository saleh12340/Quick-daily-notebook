package com.saleh12340.quickdailynotebook

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteActivity : Activity() {
    private lateinit var db: Db
    private lateinit var body: LinearLayout
    private lateinit var title: EditText
    private var noteId = 0L
    private val blocks = mutableListOf<Block>()
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = intent.getLongExtra("noteId", intent.getLongExtra("id", 0L))
        db = Db(this)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(55, 55, 55))
            setOnClickListener { save(); finish() }
        }
        top.addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
        title = EditText(this).apply {
            setText(db.title(noteId))
            hint = "عنوان الملاحظة"
            textSize = 20f
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setSingleLine(true)
            setPadding(dp(4), 0, dp(8), 0)
            background = null
        }
        top.addView(title, LinearLayout.LayoutParams(0, dp(52), 1f))
        val more = TextView(this).apply {
            text = "⋮"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(55, 55, 55))
            setOnClickListener { menu(this) }
        }
        top.addView(more, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(top)

        val date = TextView(this).apply {
            text = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date())
            textSize = 14f
            setTextColor(Color.rgb(90, 90, 90))
            gravity = Gravity.RIGHT
            setPadding(dp(16), 0, dp(16), dp(7))
        }
        root.addView(date)

        val tools = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(7))
        }
        tools.addView(tool("🎙", "تسجيل") { record() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        tools.addView(tool("🖼", "صورة") { pickImage() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        tools.addView(tool("مشاركة", "↗") { shareNote() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        tools.addView(tool("طباعة", "▣") { printNote() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(tools)

        val paperScroll = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            isFillViewport = true
        }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(2), dp(14), dp(20))
            setBackgroundColor(Color.WHITE)
        }
        paperScroll.addView(body)
        root.addView(paperScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        loadBlocks()
        if (blocks.isEmpty()) addText()
    }

    private fun tool(symbol: String, label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = "$symbol  $label"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(55, 55, 55))
            setOnClickListener { action() }
        }

    private fun lineEdit(text: String = ""): EditText {
        val e = LinedEditText(this).apply {
            setText(text)
            textSize = 18f
            setTextColor(Color.rgb(30, 30, 30))
            gravity = Gravity.TOP or Gravity.RIGHT
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(4), dp(5), dp(4), dp(3))
            setSingleLine(false)
            includeFontPadding = false
            setLineSpacing(0f, 1.0f)
            minHeight = dp(34)
        }
        body.addView(e, LinearLayout.LayoutParams(-1, -2))
        return e
    }

    private fun addText(text: String = "") { lineEdit(text).requestFocus() }

    private fun loadBlocks() {
        blocks.clear()
        body.removeAllViews()
        db.blocks(noteId).forEach { b ->
            blocks.add(b)
            when (b.type) {
                "TEXT" -> lineEdit(b.text)
                "AUDIO" -> audioView(b)
                "IMAGE" -> imageView(b)
            }
        }
    }

    private fun save() {
        if (noteId == 0L) return
        val out = mutableListOf<Block>()
        for (i in 0 until body.childCount) {
            val v = body.getChildAt(i)
            when (v) {
                is EditText -> out.add(Block(noteId = noteId, type = "TEXT", position = i, text = v.text.toString()))
                else -> {
                    val b = v.tag as? Block
                    if (b != null) out.add(b.copy(position = i))
                }
            }
        }
        blocks.clear()
        blocks.addAll(out)
        db.saveTitle(noteId, title.text.toString())
        db.replaceBlocks(noteId, blocks)
    }

    private fun menu(v: View) {
        PopupMenu(this, v).apply {
            menu.add("سطر نص جديد")
            menu.add("حذف الملاحظة")
            menu.add("نسخ الملاحظة")
            setOnMenuItemClickListener {
                when (it.title.toString()) {
                    "سطر نص جديد" -> addText()
                    "حذف الملاحظة" -> db.delete(noteId).also { finish() }
                    "نسخ الملاحظة" -> copyNote()
                }
                true
            }
        }.show()
    }

    private fun copyNote() {
        val s = buildText()
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("دفتر يومي", s))
        Toast.makeText(this, "تم نسخ الملاحظة", Toast.LENGTH_SHORT).show()
    }

    private fun buildText(): String {
        save()
        return buildString {
            append(title.text).append("\n")
            blocks.forEach {
                when (it.type) {
                    "TEXT" -> append(it.text).append("\n")
                    "AUDIO" -> append("[تسجيل صوتي: ").append(it.name).append("]\n")
                    "IMAGE" -> append("[صورة: ").append(it.name).append("]\n")
                }
            }
        }
    }

    private fun shareNote() {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, buildText())
        startActivity(Intent.createChooser(i, "مشاركة الملاحظة"))
    }

    private fun record() {
        if (recorder != null) {
            try { recorder?.stop() } catch (_: Exception) {}
            recorder?.release()
            recorder = null
            recordingFile?.let { f ->
                blocks.add(Block(noteId = noteId, type = "AUDIO", position = blocks.size, uri = f.absolutePath,
                    name = "تسجيل " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())))
            }
            save()
            loadBlocks()
            Toast.makeText(this, "تم حفظ التسجيل", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
            return
        }
        recordingFile = File(filesDir, "audio_" + System.currentTimeMillis() + ".m4a")
        recorder = MediaRecorder(this).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(recordingFile!!.absolutePath)
            prepare()
            start()
        }
        Toast.makeText(this, "🔴 جاري التسجيل — اضغط لإيقافه", Toast.LENGTH_LONG).show()
    }

    private fun audioView(b: Block) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = lineBackground()
            tag = b
        }
        val play = TextView(this).apply {
            text = "▶"
            textSize = 20f
            gravity = Gravity.CENTER
            setOnClickListener {
                try {
                    MediaPlayer().apply {
                        setDataSource(b.uri)
                        prepare()
                        start()
                        setOnCompletionListener { release() }
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@NoteActivity, "تعذر تشغيل التسجيل", Toast.LENGTH_SHORT).show()
                }
            }
        }
        row.addView(play, LinearLayout.LayoutParams(dp(50), dp(46)))
        row.addView(TextView(this).apply {
            text = "🎙  ${b.name}"
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }, LinearLayout.LayoutParams(0, dp(46), 1f))
        body.addView(row, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun imageView(b: Block) {
        val row = TextView(this).apply {
            text = "📎  ${b.name}"
            textSize = 17f
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setPadding(dp(12), 0, dp(12), 0)
            background = lineBackground()
            tag = b
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        }
        body.addView(row, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun pickImage() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), 8)
    }

    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        super.onActivityResult(r, c, d)
        if (r == 8 && c == RESULT_OK && d?.data != null) {
            val u = d.data!!
            try { contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = u.lastPathSegment?.substringAfterLast('/') ?: "صورة"
            blocks.add(Block(noteId = noteId, type = "IMAGE", position = blocks.size, uri = u.toString(), name = name))
            save()
            loadBlocks()
        }
    }

    override fun onPause() {
        save()
        super.onPause()
    }

    private fun printNote() {
        AlertDialog.Builder(this).setTitle("طريقة الطباعة")
            .setItems(arrayOf("طابعة Bluetooth 58mm", "طباعة النظام / PDF")) { _, w ->
                if (w == 0) ThermalPrinter.choose(this, buildText()) else printSystem()
            }.show()
    }

    private fun printSystem() {
        val p = getSystemService(PRINT_SERVICE) as android.print.PrintManager
        p.print("دفتر يومي", object : android.print.PrintDocumentAdapter() {
            override fun onLayout(a: android.print.PrintAttributes?, o: android.print.PrintAttributes?, c: android.os.CancellationSignal?,
                cb: android.print.PrintDocumentAdapter.LayoutResultCallback?, x: android.os.Bundle?) {
                cb?.onLayoutFinished(android.print.PrintDocumentInfo.Builder("daily_note.pdf")
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(), true)
            }
            override fun onWrite(p: Array<android.print.PageRange>?, f: android.os.ParcelFileDescriptor?,
                c: android.os.CancellationSignal?, cb: android.print.PrintDocumentAdapter.WriteResultCallback?) {
                try {
                    val doc = android.graphics.pdf.PdfDocument()
                    val page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(226, 900, 1).create())
                    val cv = page.canvas
                    cv.drawColor(Color.WHITE)
                    val paint = android.graphics.Paint().apply {
                        textSize = 18f; color = Color.BLACK; textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    var y = 35f
                    buildText().split("\n").forEach {
                        if (y < 870) { cv.drawText(it, 215f, y, paint); y += 25 }
                    }
                    doc.finishPage(page)
                    doc.writeTo(java.io.FileOutputStream(f!!.fileDescriptor))
                    doc.close()
                    cb?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) { cb?.onWriteFailed(e.message) }
            }
        }, null)
    }

    private fun lineBackground() = android.graphics.drawable.ColorDrawable(Color.WHITE)
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private inner class LinedEditText(context: Context) : androidx.appcompat.widget.AppCompatEditText(context) {
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(218, 218, 218)
            strokeWidth = dp(1).toFloat()
        }
        override fun onDraw(canvas: Canvas) {
            val lineHeight = dp(34).toFloat()
            var y = paddingTop + lineHeight - dp(5)
            while (y < height - paddingBottom) {
                canvas.drawLine(paddingLeft.toFloat(), y, (width - paddingRight).toFloat(), y, linePaint)
                y += lineHeight
            }
            super.onDraw(canvas)
        }
    }
}
