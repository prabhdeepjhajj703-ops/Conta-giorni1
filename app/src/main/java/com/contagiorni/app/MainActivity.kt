package com.contagiorni.app

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(32), dp(20), dp(32))

        val h1 = TextView(this)
        h1.text = "Conta Giorni"
        h1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        h1.setTypeface(h1.typeface, android.graphics.Typeface.BOLD)
        col.addView(h1)

        val intro = TextView(this)
        intro.text = "Aggiungi un widget alla schermata Home, scegli data, frase, foto e musica. " +
            "Tocca il widget per modificarlo, tocca il pulsante in alto a destra per la musica."
        intro.setPadding(0, dp(8), 0, dp(16))
        col.addView(intro)

        val add = Button(this)
        add.text = "Aggiungi un widget alla Home"
        add.setOnClickListener { requestPin() }
        col.addView(add, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val h2 = TextView(this)
        h2.text = "I tuoi widget"
        h2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        h2.setTypeface(h2.typeface, android.graphics.Typeface.BOLD)
        h2.setPadding(0, dp(28), 0, dp(8))
        col.addView(h2)

        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        col.addView(list)

        val scroll = ScrollView(this)
        scroll.addView(col)
        setContentView(scroll)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        list.removeAllViews()
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, DayWidgetProvider::class.java))
        if (ids.isEmpty()) {
            val t = TextView(this)
            t.text = "Nessun widget sulla Home. Usa il pulsante qui sopra."
            list.addView(t)
            return
        }
        ids.forEach { id ->
            val d = WidgetStore.get(this, id)
            val b = Button(this)
            b.isAllCaps = false
            b.text = "Modifica: " + (d?.title?.ifBlank { "Senza titolo" } ?: "Nuovo widget")
            b.setOnClickListener {
                startActivity(
                    Intent(this, WidgetConfigActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                )
            }
            list.addView(b)
        }
    }

    private fun requestPin() {
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, DayWidgetProvider::class.java), null, null)
        } else {
            Toast.makeText(
                this, "Tieni premuta la Home, scegli Widget e poi Conta Giorni.", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
