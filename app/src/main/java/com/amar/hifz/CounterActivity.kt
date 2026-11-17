package com.amar.hifz

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Fokusirani brojač: cijeli ekran je jedno dugme.
 * - zaštita od slučajnog duplog dodira (0,8 s)
 * - ekran ostaje upaljen dok je brojač otvoren (u tamnoj temi čisto crno = minimalna potrošnja)
 */
class CounterActivity : Activity() {

    private lateinit var s: Store
    private lateinit var ring: RingView
    private lateinit var tvCount: TextView
    private lateinit var tvOf: TextView
    private lateinit var tvPage: TextView
    private lateinit var tvDay: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        s = Store(this)

        ring = findViewById(R.id.ringC)
        tvCount = findViewById(R.id.tvCount)
        tvOf = findViewById(R.id.tvOf)
        tvPage = findViewById(R.id.tvCPage)
        tvDay = findViewById(R.id.tvCDay)
        tvStatus = findViewById(R.id.tvCStatus)
        ring.strokeDp = 10f

        // broji odmah na dodir (ACTION_DOWN), bez čekanja i bez vibracije
        findViewById<View>(R.id.counterRoot).setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) tap()
            true
        }
        findViewById<View>(R.id.btnMinus).setOnClickListener {
            s.sync()
            if (s.count > 0) s.count = s.count - 1
            render()
        }
        findViewById<View>(R.id.btnEdit).setOnClickListener { editCount() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        s.sync()
        render()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    private fun tap() {
        if (s.newPage == 0) return
        s.sync()
        s.count = s.count + 1
        render()
        tvCount.animate().cancel()
        tvCount.scaleX = 1.08f; tvCount.scaleY = 1.08f
        tvCount.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
    }

    private fun render() {
        val c = s.count
        val page = s.newPage
        val green = getColor(R.color.green)
        tvPage.text = if (page == 0) "Hifz završen" else "Stranica $page"
        tvCount.text = "$c"
        // cilj raste u koracima od 10: 10, 20, 30 … (do kraja stranice)
        val maxReps = (Store.DAYS - s.newDays) * Store.REPS
        val target = minOf(maxOf(1, (c + Store.REPS - 1) / Store.REPS) * Store.REPS, maxOf(maxReps, Store.REPS))
        tvOf.text = "od $target"
        ring.progress = if (c == 0) 0f else ((c - 1) % Store.REPS + 1) / Store.REPS.toFloat()
        ring.arcColor = if (s.atMilestone || s.pageDoneToday) green else getColor(R.color.blue)

        val eff = s.effectiveDays
        val total = s.newDays * Store.REPS + minOf(c, maxReps)
        tvDay.text = "dan ${minOf(eff + if (c > 0 && c % Store.REPS == 0) 0 else 1, Store.DAYS)} od ${Store.DAYS}  ·  ukupno $total/${Store.REPS * Store.DAYS}"

        when {
            s.pageDoneToday -> {
                tvStatus.text = "Stranica $page naučena ✓"
                tvStatus.setTextColor(green)
            }
            s.atMilestone -> {
                tvStatus.text = if (s.daysToday > 1) "Završeno za danas ✓  (${s.daysToday} dana)" else "Završeno za danas ✓"
                tvStatus.setTextColor(green)
            }
            else -> {
                tvStatus.text = ""
                tvStatus.setTextColor(getColor(R.color.text2))
            }
        }
    }

    private fun editCount() {
        s.sync()
        val d = resources.displayMetrics.density
        val e = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine()
            setText(s.count.toString())
            setSelection(text.length)          // kursor iza zadnje cifre
            setTextColor(getColor(R.color.text))
        }
        val box = FrameLayout(this).apply {
            val p = (22 * d).toInt()
            setPadding(p, (8 * d).toInt(), p, 0)
            addView(e)
        }
        val save = {
            e.text.toString().toIntOrNull()?.let { s.count = it }
            render()
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Koliko je proučeno?")
            .setView(box)
            .setNegativeButton("Odustani", null)
            .setPositiveButton("Sačuvaj") { _, _ -> save() }
            .create()
        // "Done" na tastaturi = Sačuvaj
        e.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save()
                dialog.dismiss()
                true
            } else false
        }
        // tastatura se otvara odmah
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        e.requestFocus()
    }

    private fun hideBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }
}
