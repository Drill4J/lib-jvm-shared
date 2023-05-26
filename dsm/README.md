# Drill storage management Project

[![License](https://camo.githubusercontent.com/8e7da7b6b632d5ef4bce9a550a5d5cfe400ca1fe/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d4170616368652532304c6963656e7365253230322e302d626c75652e7376673f7374796c653d666c6174)](http://www.apache.org/licenses/LICENSE-2.0)

DSM is kotlin serialization-based ORM for Postgres database.

Where kotlin data classes which marked with @Serializable, can easily be stored or retrieved from Postgres DB without
describing schemas.

## Example

### Jsonb table

Add @Serializable and @Id to data classes:
```kotlin
@Serializable
data class SimpleObject(
    @Id val id: String,
    val string: String,
    val int: Int,
    val last: Last,
)

@Serializable
data class Last(val string: Byte)
```

Storing:
```kotlin
val agentStore = StoreClient("schema_name")
val simpleObject = SimpleObject("id", "subStr", 12, Last(2.toByte()))
agentStore.store(simpleObject)
```

[see full code example](src/test/kotlin/DsmCoreTest.kt)

In DB will create table 'simple_object' in schema 'schema_name' with fields:
- id varchar(256): hash of the object
- json_body: jsonb

Example jsonb:
```json
{
  "id": "id_1",
  "string": "string value",
  "int": 12,
  "last": {
    "string": "2"
  }
}
```

### Binarya table

1) Add @Serializable and @Id to data classes
2) Add @Serializable(with = BinarySerializer::class) for ByteArray
```kotlin
@Serializable
data class BinaryClass(
    @Id
    val id: String,
    @Suppress("ArrayInDataClass")
    @Serializable(with = BinarySerializer::class)
    val bytes: ByteArray,
)
```

[see full code example](src/test/kotlin/BinaryTest.kt)

It will create two tables :
1) 'binary_class' with fields id & json_body. Example jsonb:
```json
{
  "id": "id_1",
  "data": "randomUUID"
}
```
2) 'binarya' with fields:
- id: value of randomUUID from 'binary_class'
- binarya: data

### Collections Storing

Classes with collections, for example:
```kotlin
@Serializable
data class ObjectWithList(
    @Id val id: String,
    val data: List<Data>
)

@Serializable
data class Data(
    val classId: String,
    val className: String,
    val testName: String,
)
```

[see code example](src/test/kotlin/StoreCollections.kt)

It will create two tables:
1) object_with_list, where instead of a collection from Data there will be keys from another table with Data:
```json
{
  "id": "f9baa088-736f-488d-8f3f-a4df3c3eed29",
  "data": [
    "-21053161590",
    "-21053161591",
    "-21053161592",
    "-21053161593",
    "-21053161594",
    "-21053161595",
    "-21053161596",
    "-21053161597",
    "-21053161598",
    "-21053161599"
  ]
}
```
2) data, each item in the Data collection will be in a separate row in the table
