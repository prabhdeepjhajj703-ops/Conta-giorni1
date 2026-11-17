package com.contagiorni.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.util.Locale

/** Disegna l'intero widget in una bitmap: sfondo, foto, testi, angoli arrotondati. */
object WidgetRenderer {

    fun render(ctx: Context, d: WidgetData, wDp: Float, hDp: Float): Bitmap {
        val density = ctx.resources.displayMetrics.density
        var w = wDp * density
        var h = hDp * density
        val k = minOf(1f, 720f / maxOf(w, h))
        w *= k
        h *= k
        val wi = maxOf(64, w.toInt())
        val hi = maxOf(64, h.toInt())
        val scale = density * k

        val bmp = Bitmap.createBitmap(wi, hi, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rect = RectF(0f, 0f, wi.toFloat(), hi.toFloat())
        val u = minOf(wi, hi).toFloat()
        val pad = u * 0.09f
        val light = d.theme == "chiaro"
        val fg = if (light) 0xFF142029.toInt() else Color.WHITE
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // ---- sfondo ----
        var hasPhoto = false
        if (d.theme == "foto" && d.imagePath != null) {
            val photo = try { BitmapFactory.decodeFile(d.imagePath) } catch (e: Exception) { null }
            if (photo != null) {
                hasPhoto = true
                c.drawBitmap(photo, coverRect(photo.width, photo.height, wi, hi), rect, paint)
                photo.recycle()
                paint.shader = LinearGradient(
                    0f, 0f, 0f, hi.toFloat(),
                    intArrayOf(0x66000000, 0x10000000, 0xB8000000.toInt()),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                c.drawRect(rect, paint)
                paint.shader = null
            }
        }
        if (!hasPhoto) {
            when (d.theme) {
                "chiaro" -> {
                    paint.color = 0xFFF4F6F8.toInt()
                    c.drawRect(rect, paint)
                }
                "foto" -> {
                    paint.shader = LinearGradient(
                        0f, 0f, wi.toFloat(), hi.toFloat(),
                        intArrayOf(d.color, darken(d.color)),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    c.drawRect(rect, paint)
                    paint.shader = null
                }
                else -> {
                    paint.color = d.color
                    c.drawRect(rect, paint)
                }
            }
        }

        // ---- testi ----
        val hasAudio = d.audioPath != null
        val textW = (wi - 2 * pad).toInt()
        val dpUnit = u / scale

        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.color = fg
        tp.alpha = 230
        tp.textSize = u * 0.075f
        tp.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        tp.letterSpacing = 0.08f
        val titleW = textW - (if (hasAudio) 52f * scale else 0f)
        val title = TextUtils.ellipsize(
            d.title.uppercase(Locale.ITALIAN), tp, maxOf(20f, titleW), TextUtils.TruncateAt.END
        )
        c.drawText(title.toString(), pad, pad + tp.textSize, tp)

        var y = hi - pad
        if (d.phrase.isNotBlank()) {
            val pp = TextPaint(Paint.ANTI_ALIAS_FLAG)
            pp.color = fg
            pp.textSize = u * 0.075f
            pp.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            val maxLines = when {
                dpUnit < 140f -> 1
                hi > wi * 0.9f -> 4
                else -> 2
            }
            val sl = StaticLayout.Builder.obtain(d.phrase, 0, d.phrase.length, pp, textW)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            y -= sl.height
            c.save()
            c.translate(pad, y)
            sl.draw(c)
            c.restore()
            y -= u * 0.04f
        }

        val info = DayMath.describe(DayMath.daysFrom(d.date))
        val up = TextPaint(Paint.ANTI_ALIAS_FLAG)
        up.color = fg
        up.textSize = u * 0.075f
        up.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        c.drawText(info.second, pad, y - up.descent(), up)
        y -= (up.descent() - up.ascent())

        val np = TextPaint(Paint.ANTI_ALIAS_FLAG)
        np.color = if (light) d.color else fg
        np.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        np.textSize = u * 0.34f
        while (np.measureText(info.first) > textW && np.textSize > 10f) {
            np.textSize *= 0.92f
        }
        c.drawText(info.first, pad, y - np.descent(), np)

        // ---- angoli arrotondati ----
        val mask = Paint(Paint.ANTI_ALIAS_FLAG)
        mask.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawRoundRect(rect, u * 0.14f, u * 0.14f, mask)
        return bmp
    }

    private fun coverRect(pw: Int, ph: Int, tw: Int, th: Int): Rect {
        val s = maxOf(tw.toFloat() / pw, th.toFloat() / ph)
        val sw = (tw / s).toInt()
        val sh = (th / s).toInt()
        val l = (pw - sw) / 2
        val t = (ph - sh) / 2
        return Rect(l, t, l + sw, t + sh)
    }

    private fun darken(color: Int): Int {
        val r = (Color.red(color) * 0.5f).toInt()
        val g = (Color.green(color) * 0.5f).toInt()
        val b = (Color.blue(color) * 0.5f).toInt()
        return Color.rgb(r, g, b)
    }
}

object ImageUtil {
    /** Legge una foto scelta dall'utente, corregge la rotazione e la riduce a maxSide pixel. */
    fun loadScaled(ctx: Context, uri: Uri, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Immagine non leggibile")

        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            ctx.contentResolver.openInputStream(uri)?.use {
                orientation = ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            orientation = ExifInterface.ORIENTATION_NORMAL
        }

        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        }
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        m.postScale(scale, scale)
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
