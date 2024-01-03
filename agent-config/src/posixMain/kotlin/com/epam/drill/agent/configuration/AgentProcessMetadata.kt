package com.epam.drill.agent.configuration

import kotlinx.cinterop.memScoped
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open
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
        val file = open("/proc/self/cmdline", O_RDONLY)
        Input(file).readText().also { close(file) }
    }

    private fun environmentVars() = memScoped {
        val file = popen("env", "r")!!
        Input(file).readText().also { pclose(file) }
            .lines().filter(String::isNotEmpty)
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
    }

}
