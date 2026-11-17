package com.amar.hifz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import java.util.Locale

/** Kružni prikaz napretka (crta se samo kad se vrijednost promijeni – bez animacija u petlji). */
class RingView(ctx: Context, attrs: AttributeSet?) : View(ctx, attrs) {

    private val d = resources.displayMetrics.density
    var strokeDp = 12f
        set(v) { field = v; track.strokeWidth = v * d; arc.strokeWidth = v * d; invalidate() }

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokeDp * d; color = ctx.getColor(R.color.track)
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokeDp * d; strokeCap = Paint.Cap.ROUND
        color = ctx.getColor(R.color.blue)
    }
    private val rect = RectF()

    var progress = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }

    var arcColor: Int
        get() = arc.color
        set(v) { arc.color = v; invalidate() }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val h = track.strokeWidth / 2f
        val l = (width - s) / 2f
        val t = (height - s) / 2f
        rect.set(l + h, t + h, l + s - h, t + s - h)
        c.drawOval(rect, track)
        if (progress > 0f) c.drawArc(rect, -90f, 360f * progress, false, arc)
    }
}

/**
 * Traka mushafa: 604 stranice s lijeva (1) na desno (604).
 * Plavo = naučeno, svijetloplavo = stranica koju sada hifziš, tanka linija ispod = današnje ponavljanje.
 */
class MushafStrip(ctx: Context, attrs: AttributeSet?) : View(ctx, attrs) {

    private val d = resources.displayMetrics.density
    private val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ctx.getColor(R.color.track) }
    private val learnP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ctx.getColor(R.color.blue) }
    private val curP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ctx.getColor(R.color.blue_soft) }
    private val oldP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ctx.getColor(R.color.text2) }
    private val clip = Path()
    private val r = RectF()

    private var newPage = Store.TOTAL
    private var pageDone = false
    private var oldFrom = 1
    private var oldTo = 20

    fun set(newPage: Int, pageDone: Boolean, oldFrom: Int, oldTo: Int) {
        this.newPage = newPage; this.pageDone = pageDone; this.oldFrom = oldFrom; this.oldTo = oldTo
        invalidate()
    }

    private val barH get() = height - 9 * d

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        clip.reset()
        clip.addRoundRect(0f, 0f, w.toFloat(), h - 9 * d, 6 * d, 6 * d, Path.Direction.CW)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val pw = w / Store.TOTAL
        c.save()
        c.clipPath(clip)
        c.drawRect(0f, 0f, w, barH, trackP)
        // naučeno: od (newPage+1) do 604, ili od newPage ako je danas završena
        val firstLearned = if (pageDone) newPage else newPage + 1
        if (firstLearned <= Store.TOTAL) c.drawRect((firstLearned - 1) * pw, 0f, w, barH, learnP)
        if (newPage in 1..Store.TOTAL && !pageDone) {
            val x = (newPage - 1) * pw
            c.drawRect(x - maxOf(0f, 3 * d - pw), 0f, x + pw, barH, curP)
        }
        c.restore()
        // današnje ponavljanje
        val y = height - 4 * d
        r.set((oldFrom - 1) * pw, y, maxOf(oldTo * pw, (oldFrom - 1) * pw + 3 * d), y + 3 * d)
        c.drawRoundRect(r, 1.5f * d, 1.5f * d, oldP)
    }
}

/* ---------- male pomoćne funkcije ---------- */

/** Edge-to-edge: odmakni sadržaj od status/navigacione trake. */
@Suppress("DEPRECATION")
fun View.padForSystemBars() {
    val l = paddingLeft; val t = paddingTop; val rr = paddingRight; val b = paddingBottom
    setOnApplyWindowInsetsListener { v, ins ->
        val top: Int
        val bottom: Int
        if (Build.VERSION.SDK_INT >= 30) {
            val i = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            top = i.top; bottom = i.bottom
        } else {
            top = ins.systemWindowInsetTop
            bottom = ins.systemWindowInsetBottom
        }
        v.setPadding(l, t + top, rr, b + bottom)
        ins
    }
    requestApplyInsets()
}

val BS: Locale = Locale.forLanguageTag("bs-BA")

/** 1 stranica, 2 stranice, 5 stranica */
fun stranica(n: Int): String {
    val a = n % 10; val b = n % 100
    return when {
        a == 1 && b != 11 -> "stranica"
        a in 2..4 && b !in 12..14 -> "stranice"
        else -> "stranica"
    }
}

/** 1 dan, 2 dana, 21 dan */
fun dan(n: Int): String = if (n % 10 == 1 && n % 100 != 11) "dan" else "dana"

fun range(from: Int, to: Int): String = if (from == to) "$from" else "$from–$to"
