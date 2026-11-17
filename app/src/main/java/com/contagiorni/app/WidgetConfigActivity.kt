package com.contagiorni.app

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Schermata per creare o modificare un widget: titolo, data, frase, stile, foto, musica. */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var data = WidgetData.default()
    private var loading = false
    private val swatches = mutableListOf<View>()

    private lateinit var ivPreview: ImageView
    private lateinit var etTitle: EditText
    private lateinit var etPhrase: EditText
    private lateinit var etAudioName: EditText
    private lateinit var btnDate: Button
    private lateinit var rgTheme: RadioGroup
    private lateinit var sbVolume: SeekBar
    private lateinit var cbLoop: CheckBox
    private lateinit var tvImageState: TextView
    private lateinit var tvAudioState: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImagePicked(uri)
    }
    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onAudioPicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        setContentView(R.layout.activity_config)
        title = "Personalizza widget"

        data = WidgetStore.get(this, widgetId) ?: WidgetData.default()

        ivPreview = findViewById(R.id.ivPreview)
        etTitle = findViewById(R.id.etTitle)
        etPhrase = findViewById(R.id.etPhrase)
        etAudioName = findViewById(R.id.etAudioName)
        btnDate = findViewById(R.id.btnDate)
        rgTheme = findViewById(R.id.rgTheme)
        sbVolume = findViewById(R.id.sbVolume)
        cbLoop = findViewById(R.id.cbLoop)
        tvImageState = findViewById(R.id.tvImageState)
        tvAudioState = findViewById(R.id.tvAudioState)

        val colorRow = findViewById<LinearLayout>(R.id.colorRow)
        WidgetData.COLORS.forEachIndexed { i, color ->
            val v = View(this)
            val lp = LinearLayout.LayoutParams(dp(40), dp(40))
            lp.setMargins(0, dp(4), dp(10), dp(4))
            v.layoutParams = lp
            v.contentDescription = "Colore ${i + 1}"
            v.setOnClickListener {
                data = data.copy(color = color)
                refreshSwatches()
                preview()
            }
            colorRow.addView(v)
            swatches.add(v)
        }

        watch(etTitle)
        watch(etPhrase)
        watch(etAudioName)
        rgTheme.setOnCheckedChangeListener { _, _ ->
            if (!loading) {
                readForm()
                preview()
            }
        }
        cbLoop.setOnCheckedChangeListener { _, _ ->
            if (!loading) readForm()
        }
        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!loading) readForm()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnDate.setOnClickListener { openDatePicker() }
        findViewById<Button>(R.id.btnImage).setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnImageClear).setOnClickListener {
            data.imagePath?.let { path -> File(path).delete() }
            data = data.copy(imagePath = null)
            fillForm()
            preview()
        }
        findViewById<Button>(R.id.btnAudio).setOnClickListener { pickAudio.launch("audio/*") }
        findViewById<Button>(R.id.btnAudioClear).setOnClickListener {
            if (WidgetStore.playingId(this) == widgetId) MusicService.stop(this)
            data.audioPath?.let { path -> File(path).delete() }
            data = data.copy(audioPath = null, audioName = "")
            fillForm()
            preview()
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        fillForm()
        preview()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun watch(e: EditText) {
        e.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!loading) {
                    readForm()
                    preview()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun readForm() {
        val theme = when (rgTheme.checkedRadioButtonId) {
            R.id.rbFoto -> "foto"
            R.id.rbChiaro -> "chiaro"
            else -> "colore"
        }
        data = data.copy(
            title = etTitle.text.toString(),
            phrase = etPhrase.text.toString(),
            audioName = etAudioName.text.toString(),
            theme = theme,
            volume = sbVolume.progress / 100f,
            loop = cbLoop.isChecked
        )
    }

    private fun fillForm() {
        loading = true
        etTitle.setText(data.title)
        etPhrase.setText(data.phrase)
        etAudioName.setText(data.audioName)
        sbVolume.progress = (data.volume * 100).toInt()
        cbLoop.isChecked = data.loop
        rgTheme.check(
            when (data.theme) {
                "foto" -> R.id.rbFoto
                "chiaro" -> R.id.rbChiaro
                else -> R.id.rbColore
            }
        )
        btnDate.text = formatDate(data.date)
        tvImageState.text = if (data.imagePath != null) "Immagine caricata (si vede nello stile Foto)" else "Nessuna immagine"
        tvAudioState.text = if (data.audioPath != null) "Brano caricato" else "Nessun brano"
        refreshSwatches()
        loading = false
    }

    private fun formatDate(s: String): String {
        return try {
            LocalDate.parse(s).format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.ITALIAN)
            )
        } catch (e: Exception) {
            "Scegli la data"
        }
    }

    private fun refreshSwatches() {
        swatches.forEachIndexed { i, v ->
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(WidgetData.COLORS[i])
            if (WidgetData.COLORS[i] == data.color) g.setStroke(dp(4), 0xFF888888.toInt())
            v.background = g
        }
    }

    private fun preview() {
        val bmp: Bitmap = WidgetRenderer.render(this, data, 300f, 170f)
        ivPreview.setImageBitmap(bmp)
    }

    private fun openDatePicker() {
        val d = try { LocalDate.parse(data.date) } catch (e: Exception) { LocalDate.now() }
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, day ->
            data = data.copy(date = LocalDate.of(y, m + 1, day).toString())
            btnDate.text = formatDate(data.date)
            preview()
        }
        DatePickerDialog(this, listener, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }

    private fun onImagePicked(uri: Uri) {
        try {
            val bmp = ImageUtil.loadScaled(this, uri, 900)
            val f = File(filesDir, "img_$widgetId.jpg")
            FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            data = data.copy(imagePath = f.absolutePath, theme = "foto")
            fillForm()
            preview()
        } catch (e: Exception) {
            Toast.makeText(this, "Immagine non valida. Prova con un JPG o PNG.", Toast.LENGTH_LONG).show()
        }
    }

    private fun onAudioPicked(uri: Uri) {
        try {
            var name = "Brano"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx) ?: "Brano"
            }
            name = name.substringBeforeLast('.').take(40)
            if (WidgetStore.playingId(this) == widgetId) MusicService.stop(this)
            val f = File(filesDir, "aud_$widgetId")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(f).use { out -> input.copyTo(out) }
            } ?: throw IllegalStateException("file non leggibile")
            if (f.length() > 40L * 1024 * 1024) {
                f.delete()
                Toast.makeText(this, "Il file supera 40 MB. Scegli un brano più leggero.", Toast.LENGTH_LONG).show()
                return
            }
            data = data.copy(audioPath = f.absolutePath, audioName = name)
            fillForm()
            preview()
        } catch (e: Exception) {
            Toast.makeText(this, "Non riesco a leggere questo file audio.", Toast.LENGTH_LONG).show()
        }
    }

    private fun save() {
        readForm()
        WidgetStore.save(this, widgetId, data)
        DayWidgetProvider.update(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        Toast.makeText(this, "Salvato", Toast.LENGTH_SHORT).show()
        finish()
    }
}
