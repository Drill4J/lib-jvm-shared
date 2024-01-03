package com.epam.drill.agent.configuration

import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import platform.posix.getpid
import platform.posix.pclose
import platform.posix.popen
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input

actual object AgentProcessMetadata {

    actual val commandLine: String
        get() = commandLine()

    actual val environmentVars: Map<String, String>
        get() = environmentVars()

    private fun commandLine() = memScoped {
        val pid = getpid()
        executeCommand("wmic process where \"processid='$pid'\" get commandline", "r").lines()[1]
    }

    private fun environmentVars() = memScoped {
        executeCommand("set", "r").lines()
            .filter(String::isNotEmpty)
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
    }

    private fun executeCommand(command: String, mode: String) = memScoped {
        val file = popen!!.invoke(command.cstr.getPointer(this), mode.cstr.getPointer(this))!!
        Input(file).readText().also { pclose!!.invoke(file) }
    }

}
