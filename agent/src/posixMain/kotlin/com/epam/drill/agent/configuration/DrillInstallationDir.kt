package com.epam.drill.agent.configuration

import kotlinx.cinterop.toKString
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.getenv
import platform.posix.open
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input

internal actual fun drillInstallationDir() = run {
    val isContainsAgentPath: (String) -> Boolean = { it.contains("-agentpath:") }
    val fromEnv: () -> String? = { getenv("JAVA_TOOL_OPTIONS")?.toKString() }
    val agentLine = fromEnv()?.takeIf(isContainsAgentPath) ?: fromProc().takeIf(isContainsAgentPath)
    val agentPath = Regex("-agentpath:(.+?)=.+").matchEntire(agentLine!!)!!.groups[1]!!.value
    agentPath.substringBeforeLast("/")
}

private fun fromProc() = run {
    val file = open("/proc/self/cmdline", O_RDONLY)
    Input(file).readText().also { close(file) }
}
