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
package com.epam.drill.common.ws

import kotlinx.serialization.Serializable

sealed class Communication {

    sealed class Agent {
        @Serializable
        @Topic("/agent/load")
        class PluginLoadEvent

        @Serializable
        @Topic("/agent/unload")
        class PluginUnloadEvent

        @Serializable
        @Topic("/agent/load-classes-data")
        class LoadClassesDataEvent

        @Serializable
        @Topic("/agent/set-packages-prefixes")
        class SetPackagePrefixesEvent

        //todo for what did it use?
        @Serializable
        @Topic("/agent/update-config")
        class UpdateConfigEvent

        @Serializable
        @Topic("/agent/update-parameters")
        class UpdateParametersEvent

        @Serializable
        @Topic("/agent/change-header-name")
        class ChangeHeaderNameEvent

        @Serializable
        @Topic("/agent/toggle")
        class ToggleEvent

        @Serializable
        @Topic("/agent/logging/update-config")
        class UpdateLoggingConfigEvent
    }

    sealed class Plugin {

        @Serializable
        @Topic("/plugin/updatePluginConfig")
        class UpdateConfigEvent

        @Serializable
        @Topic("/plugin/action")
        class DispatchEvent

        @Serializable
        @Topic("/plugin/togglePlugin")
        class ToggleEvent

        @Serializable
        @Topic("/plugin/unload")
        class UnloadEvent

        @Serializable
        @Topic("/plugin/resetPlugin")
        class ResetEvent
    }

}
