package com.amar.hifz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : Activity() {

    private lateinit var s: Store
    private val dp by lazy { resources.displayMetrics.density }

    private lateinit var content: View
    private lateinit var overlay: View
    private lateinit var progressCard: View
    private lateinit var miniRing: RingView
    private lateinit var tvMiniPct: TextView
    private var backCallback: Any? = null
    private lateinit var ring: RingView
    private lateinit var tvPercent: TextView
    private lateinit var tvPercentSub: TextView
    private lateinit var strip: MushafStrip
    private lateinit var tvLearned: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var tvEnd: TextView
    private lateinit var tvEndSub: TextView
    private lateinit var tvNewPage: TextView
    private lateinit var tvNewJuz: TextView
    private lateinit var dots: LinearLayout
    private lateinit var tvNewStatus: TextView
    private lateinit var btnCounter: Button
    private lateinit var tvOldRange: TextView
    private lateinit var tvOldSub: TextView
    private lateinit var btnOld: ImageView

    private val fmtEnd = DateTimeFormatter.ofPattern("d. M. yyyy.", BS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        s = Store(this)

        content = findViewById(R.id.root)
        overlay = findViewById(R.id.overlay)
        progressCard = findViewById(R.id.progressCard)
        miniRing = findViewById(R.id.miniRing)
        tvMiniPct = findViewById(R.id.tvMiniPct)
        ring = findViewById(R.id.ring)
        tvPercent = findViewById(R.id.tvPercent)
        tvPercentSub = findViewById(R.id.tvPercentSub)
        strip = findViewById(R.id.strip)
        tvLearned = findViewById(R.id.tvLearned)
        tvRemaining = findViewById(R.id.tvRemaining)
        tvEnd = findViewById(R.id.tvEnd)
        tvEndSub = findViewById(R.id.tvEndSub)
        tvNewPage = findViewById(R.id.tvNewPage)
        tvNewJuz = findViewById(R.id.tvNewJuz)
        dots = findViewById(R.id.dots)
        tvNewStatus = findViewById(R.id.tvNewStatus)
        btnCounter = findViewById(R.id.btnCounter)
        tvOldRange = findViewById(R.id.tvOldRange)
        tvOldSub = findViewById(R.id.tvOldSub)
        btnOld = findViewById(R.id.btnOld)

        content.padForSystemBars()
        ring.strokeDp = 13f
        miniRing.strokeDp = 3.5f

        findViewById<View>(R.id.btnProgress).setOnClickListener { showProgress() }
        overlay.setOnClickListener { hideProgress() }

        for (i in 0 until Store.DAYS) {
            val v = View(this)
            val lp = LinearLayout.LayoutParams((12 * dp).toInt(), (12 * dp).toInt())
            lp.marginStart = (7 * dp).toInt()
            dots.addView(v, lp)
        }

        btnCounter.setOnClickListener {
            startActivity(Intent(this, CounterActivity::class.java))
        }
        btnOld.setOnClickListener { v ->
            s.sync()
            s.oldDone = !s.oldDone
            v.performHapticFeedback(
                if (s.oldDone && Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.VIRTUAL_KEY
            )
            render()
        }
        findViewById<View>(R.id.btnSettings).setOnClickListener { openSettings() }
    }

    override fun onResume() {
        super.onResume()
        s.sync()
        render()
    }

    private fun render() {
        val today = LocalDate.now()

        // ---- napredak ----
        val learned = s.learned
        val pct = learned * 100f / Store.TOTAL
        tvPercent.text = String.format(BS, "%.1f%%", pct)
        tvPercentSub.text = "$learned od ${Store.TOTAL} stranica"
        ring.progress = learned / Store.TOTAL.toFloat()
        miniRing.progress = ring.progress
        tvMiniPct.text = String.format(BS, "%.0f%%", pct)
        tvLearned.text = "$learned"
        tvRemaining.text = "${Store.TOTAL - learned}"

        val rem = s.remainingSessions
        if (rem == 0) {
            tvEnd.text = "Hatma"
            tvEndSub.text = "elhamdulillah"
        } else {
            val end = s.endDate()
            val days = ChronoUnit.DAYS.between(today, end).toInt()
            tvEnd.text = end.format(fmtEnd)
            tvEndSub.text = when (days) {
                0 -> "završetak danas"
                else -> "završetak · za $days ${dan(days)}"
            }
        }

        val oJuz = s.oldJuz
        val oFrom = Store.juzStart(oJuz)
        val oTo = Store.juzEnd(oJuz)
        strip.set(s.newPage, s.pageDoneToday, oFrom, oTo)

        // ---- novi hifz ----
        val blue = getColor(R.color.blue)
        val green = getColor(R.color.green)
        val track = getColor(R.color.track)
        val page = s.newPage
        if (page == 0) {
            tvNewPage.text = "Završeno"
            tvNewJuz.text = "Cijeli Kur'an, elhamdulillah"
            tvNewStatus.text = "Allah ti ga učvrstio u srcu."
            btnCounter.visibility = View.GONE
        } else {
            btnCounter.visibility = View.VISIBLE
            tvNewPage.text = "Stranica $page"
            tvNewJuz.text = "${Store.juz(page)}. džuz"
            val eff = s.effectiveDays
            val c = s.count
            tvNewStatus.text = when {
                s.pageDoneToday && page > 1 ->
                    "Stranica naučena ✓  ·  sutra počinješ stranicu ${page - 1}"
                s.pageDoneToday -> "Posljednja stranica naučena ✓"
                s.atMilestone -> "Završeno za danas ✓  ·  dan $eff od ${Store.DAYS}"
                else -> "Danas ${c}×  ·  dan ${eff + 1} od ${Store.DAYS}  ·  ukupno ${s.newDays * Store.REPS + c}/${Store.REPS * Store.DAYS}"
            }
            tvNewStatus.setTextColor(if (s.atMilestone || s.pageDoneToday) green else getColor(R.color.text2))
            btnCounter.text = when {
                s.atMilestone || s.pageDoneToday -> "Otvori brojač"
                c > 0 -> "Nastavi  ·  ${c}×"
                else -> "Počni novi hifz"
            }
            for (i in 0 until dots.childCount) {
                val g = GradientDrawable().apply { shape = GradientDrawable.OVAL }
                when {
                    i < eff -> g.setColor(if (s.pageDoneToday) green else blue)
                    i == eff -> { g.setColor(0); g.setStroke((2 * dp).toInt(), blue) }
                    else -> g.setColor(track)
                }
                dots.getChildAt(i).background = g
            }
        }

        // ---- stari hifz ----
        tvOldRange.text = range(oFrom, oTo)
        val n = oTo - oFrom + 1
        val bg = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        if (s.oldDone) {
            val nj = Store.nextJuz(oJuz)
            tvOldSub.text = "Završeno ✓  ·  sutra $nj. džuz (${range(Store.juzStart(nj), Store.juzEnd(nj))})"
            tvOldSub.setTextColor(green)
            bg.setColor(green)
            btnOld.setColorFilter(getColor(R.color.card))
        } else {
            tvOldSub.text = "$oJuz. džuz  ·  $n ${stranica(n)}"
            tvOldSub.setTextColor(getColor(R.color.text2))
            bg.setColor(0)
            bg.setStroke((2 * dp).toInt(), track)
            btnOld.setColorFilter(track)
        }
        btnOld.background = bg
    }

    /* ---------------- Pop-up napretka ---------------- */

    private fun showProgress() {
        if (overlay.visibility == View.VISIBLE) return
        if (Build.VERSION.SDK_INT >= 31) {
            content.setRenderEffect(RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP))
        }
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(180).start()
        progressCard.scaleX = 0.94f; progressCard.scaleY = 0.94f
        progressCard.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
        if (Build.VERSION.SDK_INT >= 33) {
            val cb = OnBackInvokedCallback { hideProgress() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb
            )
            backCallback = cb
        }
    }

    private fun hideProgress() {
        if (overlay.visibility != View.VISIBLE) return
        if (Build.VERSION.SDK_INT >= 33) {
            (backCallback as? OnBackInvokedCallback)?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
            }
            backCallback = null
        }
        overlay.animate().alpha(0f).setDuration(150).withEndAction {
            overlay.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= 31) content.setRenderEffect(null)
        }.start()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (overlay.visibility == View.VISIBLE) hideProgress() else super.onBackPressed()
    }

    /* ---------------- Postavke ---------------- */

    private fun openSettings() {
        s.sync()
        val pad = (22 * dp).toInt()
        val red = getColor(R.color.error)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (8 * dp).toInt(), pad, 0)
        }
        class Field(val edit: EditText, val err: TextView, val range: IntRange, val msg: String) {
            fun valid(): Boolean = edit.text.toString().toIntOrNull()?.let { it in range } ?: false
            fun show() {
                val ok = valid()
                err.visibility = if (ok) View.GONE else View.VISIBLE
                edit.setTextColor(if (ok) getColor(R.color.text) else red)
                edit.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (ok) getColor(R.color.blue) else red
                )
            }
        }
        fun field(label: String, value: Int, range: IntRange, msg: String, last: Boolean): Field {
            box.addView(TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(getColor(R.color.text2))
                setPadding(0, (14 * dp).toInt(), 0, 0)
            })
            val e = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                imeOptions = if (last) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
                setSingleLine()
                setText(value.toString())
                setSelection(text.length)          // kursor iza zadnje cifre
                hint = "${range.first}–${range.last}"
                setTextColor(getColor(R.color.text))
                setOnFocusChangeListener { v, has -> if (has) (v as EditText).setSelection(v.text.length) }
            }
            box.addView(e)
            val err = TextView(this).apply {
                text = msg
                textSize = 12f
                setTextColor(red)
                visibility = View.GONE
            }
            box.addView(err)
            return Field(e, err, range, msg)
        }
        val fNew = field("Novi hifz – stranica koju sada hifziš", maxOf(s.newPage, 1), 1..Store.TOTAL,
            "Stranica mora biti od 1 do 604", last = false)
        val fOld = field("Stari hifz – današnji džuz", s.oldJuz, 1..Store.JUZ,
            "Džuz mora biti od 1 do 30", last = true)
        box.addView(TextView(this).apply {
            text = "Novi hifz ide unazad: 604, 603, 602…\nStari hifz: jedan džuz dnevno (1–21, 22–41 … 582–604), pa ispočetka."
            textSize = 12f
            setTextColor(getColor(R.color.text2))
            setPadding(0, (14 * dp).toInt(), 0, 0)
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle("Postavke")
            .setView(box)
            .setNegativeButton("Odustani", null)
            .setPositiveButton("Sačuvaj", null)
            .create()

        fun allValid() = fNew.valid() && fOld.valid()
        fun save() {
            if (!allValid()) return
            val np = fNew.edit.text.toString().toInt()
            val op = fOld.edit.text.toString().toInt()
            if (np != s.newPage) { s.newPage = np; s.newDays = 0 }
            if (op != s.oldJuz) { s.oldJuz = op; s.oldDone = false }
            render()
            dialog.dismiss()
        }
        fun refresh() {
            fNew.show(); fOld.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = allValid()
        }
        for (f in listOf(fNew, fOld)) {
            f.edit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(t: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(t: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(t: Editable?) = refresh()
            })
        }
        // "Done" (zadnje polje) = Sačuvaj, samo ako je sve ispravno; "Next" samo prelazi na sljedeće polje
        fOld.edit.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) { save(); true } else false
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { save() }
        refresh()
        fNew.edit.requestFocus()
    }
}
