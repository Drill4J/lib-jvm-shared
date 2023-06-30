# Removed obsolete features

## Native plugins support

* Revision: f88002dd2de8b33296ca837b27b465952dd2ffd6
* Author: epamrsa <epamrsa@gmail.com>
* Date: Friday, 30 June, 2023 16:17:26
* Message:
```
feat: Change java agent to transfer only class metadata instead of class bytes to the admin side

Remove obsolete support for native plugins

Refs: EPMDJ-10603
```
* Changes:
```
Deleted: agent/src/commonMain/kotlin/com/epam/drill/DynamicLoader.kt
Modified: agent/src/jvmMain/kotlin/com/epam/drill/ActualStubs.kt
Deleted: agent/src/mingwX64Main/kotlin/com/epam/drill/DynamicLoader.kt
Modified: agent/src/nativeMain/kotlin/com/epam/drill/core/AgentApi.kt
Modified: agent/src/nativeMain/kotlin/com/epam/drill/core/ws/WsRouter.kt
Deleted: agent/src/nativeMain/kotlin/com/epam/drill/plugin/api/processing/NativePart.kt
Deleted: agent/src/posixMain/kotlin/com/epam/drill/DynamicLoader.kt
Modified: common/src/commonMain/kotlin/com/epam/drill/common/PluginMetadata.kt
```
