# Drill KNI gradle plugin

## What does KNI plugin do:

For all java classes annotated with the @Kni annotation, a platform class will be generated and added to platform folder
(for platforms you specified in build.gradle file):
```kotlin
kotlin {
    kni {
        jvmTargets = sequenceOf()
        additionalJavaClasses = sequenceOf()
        nativeCrossCompileTarget = sequenceOf(mingwX64(), linuxX64(), macosX64())
    }
}
```

## What will be generated:

For example, you have simple kotlin/java class:
```kotlin
@Kni
object ExampleClass {
    fun testMethod() {
        println("Do smth")
    }
}
```

After compilation will be generated a stub class:
```kotlin
@ThreadLocal
public object ExampleClassStub {
    private val classRef: jclass = FindClass("com/epam/drill/agent/instrument/ExampleClass")!!

    public var selfMethodId: jfieldID? = GetStaticFieldID(classRef, "INSTANCE",
        "Lcom/epam/drill/agent/instrument/ExampleClass;")

    private lateinit var testMethodRef39797: jmethodID

    init {
        testMethodRef39797 = GetMethodID(classRef, "testMethod", "()V")!!
    }

    private val objectRef: jobject
        get() = GetStaticObjectField(classRef, selfMethodId)!!

    public fun Any.self(): jobject? = null

    public operator fun invoke(ignored: jobject): ExampleClassStub = this

    public fun self(): jobject = objectRef

    public fun testMethod(): Unit {
        val kniResult =
            CallVoidMethod(
                objectRef,
                testMethodRef39797

            )!!
        return kniResult
    }
}
```

Stub class is a platform class and when calling the methods of it will in invoke same java method.

## Problems and solution example

The main problem is kni plugin search class annotated with ```@Kni``` annotation in all transitive dependencies.

For example you are having such project dependency tree:
```bash
java agent - with kni plugin
|-- http-clients-instrumentation - with kni plugin
|   `-- logger
|       `-- Class with @Kni annotation
`-- logger
    `-- Class with @Kni annotation  
```

Problems:
- In ```http-client-instrumentation``` dependency, because it has ```logger``` as transitive dependency, so it will
  generate stubs for ```loger``` classes, ```java-agent``` also have ```logger``` dependency, and stub will be generated
  to, so we will get `unsatisfied link error`.
- ```http-client-instrumentation``` have classes with @Kni and there is a same problem as with logger, java agent will
  try to generate stubs for classes.

Solution:
- For the first case we need to exclude logger from stub generation. For this in build.gradle file in ```kni``` block
  set ```excludedClasses``` property. Example:
```kotlin
kotlin {
    kni {
        jvmTargets = sequenceOf()
        additionalJavaClasses = sequenceOf()
        nativeCrossCompileTarget = sequenceOf()
        excludedClasses = sequenceOf("com.epam.drill.logger.NativeApi")
    }
}
```
- Second problem will be solved automatically. Classes generated in ```http-client-instrumentation``` will be written
  to ```kni-meta-info``` file and added to jar resources. Before generating stubs kni plugin will find ```kni-meta-info```
  file and will ignore this classes from stub generation.
