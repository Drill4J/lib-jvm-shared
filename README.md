> **DISCLAIMER**: We use Google Analytics for sending anonymous usage information such as agent's type, version
> after a successful agent registration. This information might help us to improve both Drill4J backend and client sides. It is used by the
> Drill4J team only and is not supposed for sharing with 3rd parties.
> You are able to turn off by set system property `analytic.disable = true` or send PATCH request `/api/analytic/toggle`

[![Check](https://github.com/Drill4J/lib-jvm-shared/actions/workflows/check.yml/badge.svg)](https://github.com/Drill4J/lib-jvm-shared/actions/workflows/check.yml)
[![License](https://img.shields.io/github/license/Drill4J/lib-jvm-shared)](LICENSE)
[![Visit the website at drill4j.github.io](https://img.shields.io/badge/visit-website-green.svg?logo=firefox)](https://drill4j.github.io/)
[![Telegram Chat](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/drill4j)
![Docker Pulls](https://img.shields.io/docker/pulls/drill4j/lib-jvm-shared)
![GitHub contributors](https://img.shields.io/github/contributors/Drill4J/lib-jvm-shared)
![Lines of code](https://img.shields.io/tokei/lines/github/Drill4J/lib-jvm-shared)
![YouTube Channel Views](https://img.shields.io/youtube/channel/views/UCJtegUnUHr0bO6icF1CYjKw?style=social)

# Drill4J JVM-based shared libraries

The JVM-based shared libraries for Drill4J, used in java-agent, test2code-plugin and admin components of Drill4J.

## Modules

- **admin-analytics**: Google Analytics, see more in [here](admin-analytics/README.md)
- **agent**: Contains the abstract agent core used to create platform specific JVMTI agents (Java, .NET), , see more in [here](agent/README.md)
- **agent-runner-common**: Common part of plugins for build tools (gradle and maven) to easily run [autotest agents](https://github.com/Drill4J/autotest-agent)
- **agent-runner-gradle**: Gradle part of plugins for build tools to easily run [autotest agents](https://github.com/Drill4J/autotest-agent)
- **common**: Common back-end related Drill4J project parts (common part)
- **drill-hook**: Library for intercepting system functions.
- **dsm**: Serialization-based ORM for Postgres database
- **dsm-annotations**: Annotations classes for DSM
- **dsm-benchmarks**: Benchmark tests for DSM
- **dsm-test-framework**: Framework for DSM tests (database container)
- **gardle-plugin**: Cross-compilation plugin for gradle, obsolete
- **http-clients-instrumentation**: Common instrumentation for http clients
- **interceptor-http**: Library for intercepting http protocols calls
- **interceptor-http-test**: Tests for **interceptor-http**
- **interceptor-http2**: Library (v2) for intercepting http protocols calls
- **interceptor-http2-test**: Tests for **interceptor-http2**
- **interceptor-http2-test-grpc**: Tests for **interceptor-http2**
- **jvmapi**: Library to simplify JVMTI calls from java-agent code
- **knasm**: Port of Java ASM library to Kotlin native
- **kni-plugin**: Gradle plugin for KNI classes (stub classes to simplify JVMTI calls) generation
- **kni-runtime**: Annotation and interface classes to use with KNI plugin
- **kt2dts**: Kotlin API -> TypeScript interfaces converter implementation
- **kt2dts-api-sample**: **kt2dts** samples for tests
- **kt2dts-cli**: Module for **kt2dts** executable JAR generation
- **ktor-swagger**: Library for ktor with swagger integration
- **ktor-swagger-sample**: Sample implementation for **ktor-swagger**
- **logger**: Simple logging framework
- **logger-api**: Simple logging framework API (used in **logger** and **java-agent**)
- **logger-test-agent**: Agent for **logger** tests
- **plugin-api-admin**: Common back-end related Drill4J project parts (admin part)
- **plugin-api-agent**: Common back-end related Drill4J project parts (agent part)
- **test-data**: Test data for admin/test2code-plugin testing
- **test-plugin**: Module for test-plugin distribution generation (includes **test-plugin-admin** and **test-plugin-agent** jars)
- **test-plugin-admin**: Test-plugin for **admin** component tests (admin-part)
- **test-plugin-agent**: Test-plugin for **admin** component tests (agent-part)
- **test2code-api**: Classes for working with [admin](https://github.com/Drill4J/admin). It analyzes probes and send metrics & statistics.
- **transport**: Library with transport protocol for backend communication

## Modules kni-plugin and kni-runtime

**Kni-runtime** module container two classes:
- **annotation class Kni**: To mark classes for which kni-stub classes should be generated. Generated stub classes used for 2 purposes:
  - Call JVM methods from native code, example: `actual fun retrieveClassesData(config: String): ByteArray { return DataServiceStub.retrieveClassesData(config) }`
  - Call native methods from JVM code, example: `actual external fun adminAddressHeader(): String?`
- **interface JvmtiAgent**: Interface should be implemented by JVMTI-agent class which will be called in kni-generated function:
```kotlin
@CName("Agent_OnLoad")
public fun agentOnLoad(
    vmPointer: CPointer<JavaVMVar>,
    options: String,
    reservedPtr: Long
): Int = memScoped {
    com.epam.drill.jvmapi.vmGlobal.value = vmPointer.freeze()
    val vm = vmPointer.pointed
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(),
        com.epam.drill.jvmapi.gen.JVMTI_VERSION.convert())
    com.epam.drill.jvmapi.jvmti.value = jvmtiEnvPtr.value
    jvmtiEnvPtr.value.freeze()
    com.epam.drill.core.Agent.agentOnLoad(options) // call method of class configured in gradle file using kni-block 
}
```

**Kni-plugin** module contains classes for kni-stubs and JVMTI entry-point generation, generation details should be configured in gradle file using **kni** block, like:
```kotlin
    kni {
        jvmTargets = sequenceOf(jvm)
        jvmtiAgentObjectPath = "com.epam.drill.core.Agent"
        nativeCrossCompileTarget = sequenceOf(linuxX64, mingwX64, macosX64)
    }
```
