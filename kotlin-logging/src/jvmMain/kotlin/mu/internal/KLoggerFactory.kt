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
package mu.internal

import mu.KLoggable
import mu.KLogger
import mu.KLoggingContext
import org.slf4j.Logger

/**
 * factory methods to obtain a [Logger]
 */
@Suppress("NOTHING_TO_INLINE")
internal object KLoggerFactory {
    private val loggerContext = KLoggingContext.context
    /**
     * get logger for the class
     */
    internal inline fun logger(loggable: KLoggable): KLogger =
        logger(KLoggerNameResolver.name(loggable.javaClass))

    /**
     * get logger by explicit name
     */
    internal inline fun logger(name: String): KLogger = wrapJLogger(jLogger(name))

    /**
     * get logger for the method, assuming it was declared at the logger file/class
     */
    internal inline fun logger(noinline func: () -> Unit): KLogger =
        logger(KLoggerNameResolver.name(func))

    /**
     * get a java logger by name
     */
    private inline fun jLogger(name: String): Logger {
        return loggerContext.getLogger(name)
    }

    /**
     * wrap java logger based on location awareness
     */
    internal inline fun wrapJLogger(jLogger: Logger): KLogger = LocationIgnorantKLogger(jLogger)

}

