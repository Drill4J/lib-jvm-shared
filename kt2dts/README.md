# Drill Kotlin API -> TypeScript interfaces

A very limited reflection based converter of serializable Kotlin classes to Typescript interfaces.
The converter processes classes with @Serializable annotation from the classpath.

Limitations:
* Maps with keys other than String or Number
* Polymorphic hierarchies without @SerialName annotation

## Usage

Using Gradle:
```shell script
./gradlew :kt2dts:run --args="--cp=my-sample.jar --module=my-module"
```

Executing fat jar:
1. Build the fat jar: ```./gradlew :kt2dts:fatJar```
2. After successful build the fat jar (kt2dts-cli-0.1.0.jar) can be found at: ```./kt2dts-cli/build/lib```
3. Run the converter: ```java -jar kt2dts-cli-0.1.0.jar --cp=my-sample.jar --module=my-module```
