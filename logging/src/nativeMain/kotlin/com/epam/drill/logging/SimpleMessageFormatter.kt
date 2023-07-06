package com.epam.drill.logging

import io.ktor.util.date.GMTDate
import mu.Formatter
import mu.KotlinLoggingLevel
import mu.Marker
import mu.internal.ErrorMessageProducer

object SimpleMessageFormatter : Formatter {

    override fun formatMessage(level: KotlinLoggingLevel, loggerName: String, msg: () -> Any?) =
        "${formatPrefix(level, loggerName)} ${msg.toStringSafe()}"

    override fun formatMessage(level: KotlinLoggingLevel, loggerName: String, t: Throwable?, msg: () -> Any?) =
        "${formatPrefix(level, loggerName)} ${msg.toStringSafe()}${t.throwableToString()}"

    override fun formatMessage(level: KotlinLoggingLevel, loggerName: String, marker: Marker?, msg: () -> Any?) =
        "${formatPrefix(level, loggerName)} ${marker.wrapToString()}${msg.toStringSafe()}"

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        t: Throwable?,
        msg: () -> Any?
    ) = "${formatPrefix(level, loggerName)} ${marker.wrapToString()}${msg.toStringSafe()}${t.throwableToString()}"

    private inline fun formatPrefix(level: KotlinLoggingLevel, loggerName: String) =
        "${GMTDate().dateToString()} ${level.name.padEnd(7)} [$loggerName]"

    private inline fun GMTDate.dateToString(): String {
        val padStart: Int.(Int) -> String = { this.toString().padStart(it, '0') }
        return "$year-${month.ordinal.inc().padStart(2)}-${dayOfMonth.padStart(2)} " +
                "${hours.padStart(2)}:${minutes.padStart(2)}:${seconds.padStart(2)}.000"
    }

    private inline fun Marker?.wrapToString(): String {
        val wrapToParentheses: (String) -> String = { "($it) " }
        return this?.let(Marker::getName)?.takeIf(String::isNotEmpty)?.let(wrapToParentheses) ?: ""
    }

    private inline fun (() -> Any?).toStringSafe(): String {
        return try {
            invoke().toString()
        } catch (e: Exception) {
            ErrorMessageProducer.getErrorLog(e)
        }
    }

    private fun Throwable?.throwableToString(): String {
        if (this == null) return ""
        var msg = "\n"
        var current = this
        while (current != null && current.cause != current) {
            if (current != this) msg += "Caused by: "
            msg += "$current\n"
            current.getStackTrace().forEach { msg += "    at $it\n" }
            current = current.cause
        }
        return msg
    }

}
