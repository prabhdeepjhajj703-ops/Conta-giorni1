package com.contagiorni.app

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class WidgetData(
    val title: String,
    val date: String,
    val phrase: String,
    val theme: String,
    val color: Int,
    val imagePath: String?,
    val audioPath: String?,
    val audioName: String,
    val volume: Float,
    val loop: Boolean
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("title", title)
        o.put("date", date)
        o.put("phrase", phrase)
        o.put("theme", theme)
        o.put("color", color)
        o.put("imagePath", imagePath ?: JSONObject.NULL)
        o.put("audioPath", audioPath ?: JSONObject.NULL)
        o.put("audioName", audioName)
        o.put("volume", volume.toDouble())
        o.put("loop", loop)
        return o.toString()
    }

    companion object {
        val COLORS = intArrayOf(
            0xFF1456D6.toInt(), 0xFF0E8F6B.toInt(), 0xFFC2410C.toInt(),
            0xFFB4234B.toInt(), 0xFF6D28D9.toInt(), 0xFF1B2733.toInt()
        )

        fun default() = WidgetData(
            title = "Nuovo evento",
            date = LocalDate.now().plusDays(30).toString(),
            phrase = "",
            theme = "colore",
            color = COLORS[0],
            imagePath = null,
            audioPath = null,
            audioName = "",
            volume = 0.7f,
            loop = true
        )

        fun fromJson(s: String): WidgetData? {
            return try {
                val o = JSONObject(s)
                WidgetData(
                    title = o.optString("title"),
                    date = o.optString("date"),
                    phrase = o.optString("phrase"),
                    theme = o.optString("theme", "colore"),
                    color = o.optInt("color", COLORS[0]),
                    imagePath = if (o.isNull("imagePath")) null else o.optString("imagePath"),
                    audioPath = if (o.isNull("audioPath")) null else o.optString("audioPath"),
                    audioName = o.optString("audioName"),
                    volume = o.optDouble("volume", 0.7).toFloat(),
                    loop = o.optBoolean("loop", true)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

object DayMath {
    fun daysFrom(date: String): Int? {
        return try {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date)).toInt()
        } catch (e: Exception) {
            null
        }
    }

    /** Restituisce (numero grande, testo sotto il numero). */
    fun describe(d: Int?): Pair<String, String> {
        if (d == null) return Pair("–", "imposta una data")
        if (d == 0) return Pair("Oggi", "è il grande giorno")
        val n = abs(d)
        return if (d > 0) {
            Pair(n.toString(), if (n == 1) "giorno mancante" else "giorni mancanti")
        } else {
            Pair(n.toString(), if (n == 1) "giorno passato" else "giorni passati")
        }
    }
}

object WidgetStore {
    private const val PREFS = "conta_giorni"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(c: Context, id: Int): WidgetData? {
        val s = prefs(c).getString("w_$id", null) ?: return null
        return WidgetData.fromJson(s)
    }

    fun save(c: Context, id: Int, d: WidgetData) {
        prefs(c).edit().putString("w_$id", d.toJson()).apply()
    }

    fun delete(c: Context, id: Int) {
        val d = get(c, id)
        if (d != null) {
            d.imagePath?.let { path -> File(path).delete() }
            d.audioPath?.let { path -> File(path).delete() }
        }
        prefs(c).edit().remove("w_$id").apply()
    }

    fun playingId(c: Context): Int = prefs(c).getInt("playing", -1)

    fun setPlaying(c: Context, id: Int) {
        prefs(c).edit().putInt("playing", id).apply()
    }
}
