package com.epam.drill.common.util

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object JavaZip {

    fun compress(input: ByteArray, level: Int = 1): ByteArray = Deflater(level, true).run {
        ByteArrayOutputStream().use { stream ->
            this.setInput(input)
            this.finish()
            val readBuffer = ByteArray(1024)
            val readed: (Int) -> Boolean = { it > 0 }
            while (!this.finished()) {
                this.deflate(readBuffer).takeIf(readed)?.also { stream.write(readBuffer, 0, it) }
            }
            this.end()
            stream.toByteArray()
        }
    }

    fun decompress(input: ByteArray): ByteArray = Inflater(true).run {
        ByteArrayOutputStream().use { stream ->
            this.setInput(input)
            val readBuffer = ByteArray(1024)
            val readed: (Int) -> Boolean = { it > 0 }
            while (!this.finished()) {
                this.inflate(readBuffer).takeIf(readed)?.also { stream.write(readBuffer, 0, it) }
            }
            this.end()
            stream.toByteArray()
        }
    }

}
