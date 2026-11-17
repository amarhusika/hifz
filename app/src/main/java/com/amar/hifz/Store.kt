package com.amar.hifz

import android.content.Context
import java.time.LocalDate

/**
 * Sva stanja aplikacije (SharedPreferences – nema baze, nema servisa, nema rada u pozadini).
 *
 * Novi hifz: ide unazad 604 → 603 → … → 1. Svaka stranica: 10× dnevno, 4 dana (ukupno 40×).
 * Stari hifz: jedan džuz dnevno (1–21, 22–41, 42–61 … 582–604), pa ispočetka.
 */
class Store(ctx: Context) {

    companion object {
        const val TOTAL = 604
        const val REPS = 10          // ponavljanja dnevno
        const val DAYS = 4           // dana po stranici (4 × 10 = 40)
        const val JUZ = 30

        /** Džuzevi (Medinski mushaf): 1–21, 22–41, 42–61 … 562–581, 582–604. */
        fun juz(page: Int): Int = if (page < 22) 1 else minOf(JUZ, (page + 18) / 20)
        fun juzStart(j: Int): Int = if (j <= 1) 1 else 20 * j - 18
        fun juzEnd(j: Int): Int = if (j >= JUZ) TOTAL else juzStart(j + 1) - 1
        fun nextJuz(j: Int): Int = if (j >= JUZ) 1 else j + 1
    }

    private val p = ctx.getSharedPreferences("hifz", Context.MODE_PRIVATE)

    /** Stranica koju trenutno hifziš (0 = sve završeno). */
    var newPage: Int
        get() = p.getInt("newPage", TOTAL)
        set(v) = p.edit().putInt("newPage", v.coerceIn(0, TOTAL)).apply()

    /** Završeni dani na trenutnoj stranici PRIJE današnjeg dana (0..3). */
    var newDays: Int
        get() = p.getInt("newDays", 0)
        set(v) = p.edit().putInt("newDays", v.coerceIn(0, DAYS - 1)).apply()

    /** Današnji brojač. */
    var count: Int
        get() = p.getInt("count", 0)
        set(v) = p.edit().putInt("count", v.coerceIn(0, 999)).apply()

    /** Stari hifz – današnji džuz (1..30). */
    var oldJuz: Int
        get() = p.getInt("oldJuz", 1)
        set(v) = p.edit().putInt("oldJuz", v.coerceIn(1, JUZ)).apply()

    var oldDone: Boolean
        get() = p.getBoolean("oldDone", false)
        set(v) = p.edit().putBoolean("oldDone", v).apply()

    private var lastDate: String
        get() = p.getString("lastDate", "") ?: ""
        set(v) = p.edit().putString("lastDate", v).apply()

    /** Prelazak na novi dan: zaključi jučerašnji rad. Zove se pri svakom otvaranju. */
    fun sync() {
        val today = LocalDate.now().toString()
        val last = lastDate
        if (last == today) return
        if (last.isNotEmpty()) {
            if (count >= REPS && newPage > 0) {
                // svakih 10 ponavljanja = jedan dan (20× u jednom danu = 2 dana)
                val d = newDays + count / REPS
                if (d >= DAYS) {
                    p.edit().putInt("newDays", 0).putInt("newPage", newPage - 1).apply()
                } else newDays = d
            }
            count = 0
            if (oldDone) {
                oldJuz = nextJuz(oldJuz)
                oldDone = false
            }
        }
        lastDate = today
    }

    val doneToday: Boolean get() = count >= REPS

    /** "Završeno za danas" samo kad je brojač tačno na 10, 20, 30 … (ako vratiš na 19, nije završeno). */
    val atMilestone: Boolean get() = count > 0 && count % REPS == 0

    /** Dani zarađeni danas (10× = 1 dan, 20× = 2 dana …). */
    val daysToday: Int get() = minOf(count / REPS, DAYS - newDays)

    /** Završeni dani na trenutnoj stranici, uključujući danas. */
    val effectiveDays: Int get() = newDays + daysToday

    /** Trenutna stranica je kompletirana danas (4. dan završen). */
    val pageDoneToday: Boolean get() = newPage > 0 && effectiveDays >= DAYS

    /** Broj naučenih stranica (od trenutne do 604). */
    val learned: Int get() = TOTAL - newPage + if (pageDoneToday) 1 else 0

    /** Koliko još dana (sesija) do kraja. */
    val remainingSessions: Int get() = maxOf(0, newPage * DAYS - effectiveDays)

    /** Procijenjeni dan završetka hifza. */
    fun endDate(): LocalDate {
        val today = LocalDate.now()
        val r = remainingSessions.toLong()
        if (r == 0L) return today
        return if (doneToday) today.plusDays(r) else today.plusDays(r - 1)
    }
}
