package com.weighttracker.app.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val PT_BR = Locale("pt", "BR")

/** 78.4 → "78,4" */
fun kg(value: Double): String = String.format(PT_BR, "%.1f", value)

/** −0.3 → "−0,3", 0.3 → "+0,3" (typographic minus). */
fun kgSigned(value: Double): String {
    val sign = when {
        value <= -0.05 -> "−"
        value >= 0.05 -> "+"
        else -> ""
    }
    return sign + kg(abs(value))
}

/** 84.0 → "84", 72.5 → "72,5" */
fun kgShort(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else kg(value)

/** Accepts both "80.5" and "80,5" (Portuguese keyboards). */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** Portuguese date texts used across the screens. */
object PtDate {
    private val WEEKDAYS = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    private val MONTHS = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val SAVED_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

    fun weekday(d: LocalDate) = WEEKDAYS[d.dayOfWeek.value - 1]
    fun month(d: LocalDate) = MONTHS[d.monthValue - 1]
    fun month3(d: LocalDate) = month(d).take(3)

    /** "Domingo, 4 de outubro" */
    fun long(d: LocalDate) = "${weekday(d)}, ${d.dayOfMonth} de ${month(d)}"

    /** "Hoje, 4 de outubro de 2026" / "Sábado, 3 de outubro de 2026" */
    fun full(d: LocalDate, today: LocalDate) =
        (if (d == today) "Hoje" else weekday(d)) + ", ${d.dayOfMonth} de ${month(d)} de ${d.year}"

    /** "Domingo, 4 out" */
    fun title(d: LocalDate) = "${weekday(d)}, ${d.dayOfMonth} ${month3(d)}"

    /** "12 ago" */
    fun dayMonth(d: LocalDate) = "${d.dayOfMonth} ${month3(d)}"

    /** "18 de novembro" (+ year when it differs from [today]'s) */
    fun eta(d: LocalDate, today: LocalDate) =
        "${d.dayOfMonth} de ${month(d)}" + if (d.year != today.year) " de ${d.year}" else ""

    /** "Outubro 2026" */
    fun monthYear(m: YearMonth) = MONTHS[m.monthValue - 1].replaceFirstChar { it.uppercase() } + " ${m.year}"

    /** ISO timestamp → "04/10 07:12" */
    fun savedAt(iso: String?): String? = try {
        iso?.let { LocalDateTime.parse(it).format(SAVED_AT) }
    } catch (e: Exception) {
        null
    }
}
