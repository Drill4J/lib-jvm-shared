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
import org.slf4j.Logger


public actual object KotlinLogging {
    /**
     * This method allow defining the logger in a file in the following way:
     * ```
     * val logger = KotlinLogging.logger {}
     * ```
     */
    public actual fun logger(func: () -> Unit): KLogger = KLoggerFactory.logger(func)

    public actual fun logger(name: String): KLogger = KLoggerFactory.logger(name)

    public fun logger(underlyingLogger: Logger): KLogger = KLoggerFactory.wrapJLogger(underlyingLogger)
}

public fun Logger.toKLogger(): KLogger = KotlinLogging.logger(this)
