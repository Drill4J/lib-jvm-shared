/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")

package mu.internal

import org.slf4j.Logger
import mu.KLogger
import mu.Marker

/**
 * A class wrapping a [Logger] instance that is not location aware
 * all methods of [KLogger] has default implementation
 * the rest of the methods are delegated to [Logger]
 * Hence no implemented methods
 */
@Suppress("TooManyFunctions")
internal class LocationIgnorantKLogger(override val underlyingLogger: Logger) : KLogger {
    override val name: String
        get() = underlyingLogger.name

    /**
     * Lazy add a log message if isTraceEnabled is true
     */
    override fun trace(msg: () -> Any?) {
        if (underlyingLogger.isTraceEnabled) underlyingLogger.trace(msg.toStringSafe())
    }

    /**
     * Lazy add a log message if isDebugEnabled is true
     */
    override fun debug(msg: () -> Any?) {
        if (underlyingLogger.isDebugEnabled) underlyingLogger.debug(msg.toStringSafe())
    }

    /**
     * Lazy add a log message if isInfoEnabled is true
     */
    override fun info(msg: () -> Any?) {
        if (underlyingLogger.isInfoEnabled) underlyingLogger.info(msg.toStringSafe())
    }

    /**
     * Lazy add a log message if isWarnEnabled is true
     */
    override fun warn(msg: () -> Any?) {
        if (underlyingLogger.isWarnEnabled) underlyingLogger.warn(msg.toStringSafe())
    }

    /**
     * Lazy add a log message if isErrorEnabled is true
     */
    override fun error(msg: () -> Any?) {
        if (underlyingLogger.isErrorEnabled) underlyingLogger.error(msg.toStringSafe())
    }

    /**
     * Lazy add a log message with throwable payload if isTraceEnabled is true
     */
    override fun trace(t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isTraceEnabled) underlyingLogger.trace(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with throwable payload if isDebugEnabled is true
     */
    override fun debug(t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isDebugEnabled) underlyingLogger.debug(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with throwable payload if isInfoEnabled is true
     */
    override fun info(t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isInfoEnabled) underlyingLogger.info(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with throwable payload if isWarnEnabled is true
     */
    override fun warn(t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isWarnEnabled) underlyingLogger.warn(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with throwable payload if isErrorEnabled is true
     */
    override fun error(t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isErrorEnabled) underlyingLogger.error(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with a marker if isTraceEnabled is true
     */
    override fun trace(marker: Marker?, msg: () -> Any?) {
        if (underlyingLogger.isTraceEnabled) underlyingLogger.trace(msg.toStringSafe())
    }

    /**
     * Lazy add a log message with a marker if isDebugEnabled is true
     */
    override fun debug(marker: Marker?, msg: () -> Any?) {
        if (underlyingLogger.isDebugEnabled) underlyingLogger.debug(msg.toStringSafe())
    }

    /**
     * Lazy add a log message with a marker if isInfoEnabled is true
     */
    override fun info(marker: Marker?, msg: () -> Any?) {
        if (underlyingLogger.isInfoEnabled) underlyingLogger.info(msg.toStringSafe())
    }

    /**
     * Lazy add a log message with a marker if isWarnEnabled is true
     */
    override fun warn(marker: Marker?, msg: () -> Any?) {
        if (underlyingLogger.isWarnEnabled) underlyingLogger.warn(msg.toStringSafe())
    }

    /**
     * Lazy add a log message with a marker if isErrorEnabled is true
     */
    override fun error(marker: Marker?, msg: () -> Any?) {
        if (underlyingLogger.isErrorEnabled) underlyingLogger.error(msg.toStringSafe())
    }

    /**
     * Lazy add a log message with a marker and throwable payload if isTraceEnabled is true
     */
    override fun trace(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isTraceEnabled) underlyingLogger.trace(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with a marker and throwable payload if isDebugEnabled is true
     */
    override fun debug(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isDebugEnabled) underlyingLogger.debug(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with a marker and throwable payload if isInfoEnabled is true
     */
    override fun info(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isInfoEnabled) underlyingLogger.info(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with a marker and throwable payload if isWarnEnabled is true
     */
    override fun warn(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isWarnEnabled) underlyingLogger.warn(msg.toStringSafe(), t)
    }

    /**
     * Lazy add a log message with a marker and throwable payload if isErrorEnabled is true
     */
    override fun error(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (underlyingLogger.isErrorEnabled) underlyingLogger.error(msg.toStringSafe(), t)
    }

    override inline fun entry(vararg argArray: Any?) {
        if (underlyingLogger.isTraceEnabled) {
            underlyingLogger.trace("entry({})", argArray)
        }
    }

    override inline fun exit() {
        if (underlyingLogger.isTraceEnabled) {
            underlyingLogger.trace("exit")
        }
    }

    override inline fun <T : Any?> exit(result: T): T {
        if (underlyingLogger.isTraceEnabled) {
            underlyingLogger.trace("exit({})", result)
        }
        return result
    }

    override inline fun <T : Throwable> throwing(throwable: T): T {
        if (underlyingLogger.isErrorEnabled) {
            underlyingLogger.error("throwing($throwable)", throwable)
        }
        return throwable
    }

    override inline fun <T : Throwable> catching(throwable: T) {
        if (underlyingLogger.isErrorEnabled) {
            underlyingLogger.error("catching($throwable)", throwable)
        }
    }
}
