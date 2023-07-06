package com.epam.drill.logging

/**
 * Object for logging configuration initialization
 */
expect object LoggingConfiguration {

    /**
     * Read default logging configuration from /logging.properties file for JVM
     * or initialize default configuration for native
     */
    fun readDefaultConfiguration()

    /**
     * Set logging levels per logger for JVM or set global logging level for native
     * (using any of "", "com", "com.epam", "com.epam.drill" logger names)
     *
     * @param  levels
     *         list of level pairs, e.g. ("", "INFO"), ("com.epam.drill", "TRACE")
     */
    fun setLoggingLevels(levels: List<Pair<String, String>>)

    /**
     * Set logging levels per logger for JVM or set global logging level for native
     * (using any of "", "com", "com.epam", "com.epam.drill" logger names)
     *
     * @param  levels
     *         semicolon-separated string of level pairs,
     *         e.g. "INFO" or "=INFO;com.epam.drill=TRACE;something=INFO"
     */
    fun setLoggingLevels(levels: String)

}
