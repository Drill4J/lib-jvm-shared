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
package mu

import mu.internal.KLoggerFactory

/**
 * A class with logging capabilities
 * usage example:
 * ```
 * class ClassWithLogging {
 *   companion object: KLogging()
 *   fun test() {
 *     logger.info{"test ClassWithLogging"}
 *   }
 * }
 * ```
 */
public open class KLogging : KLoggable {
    override val logger: KLogger = logger()
}

/**
 * A class with logging capabilities and explicit logger name
 */
public open class NamedKLogging(name: String) : KLoggable {
    override val logger: KLogger = logger(name)
}

/**
 * An interface representing class with logging capabilities
 * implemented using a logger
 * obtain a logger with logger() method
 */
public interface KLoggable {

    /**
     * The member that performs the actual logging
     */
    public val logger: KLogger

    /**
     * get logger for the class
     */
    public fun logger(): KLogger = KLoggerFactory.logger(this)

    /**
     * get logger by explicit name
     */
    public fun logger(name: String): KLogger = KLoggerFactory.logger(name)
}



