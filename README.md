# KDTO

**KDTO** is a Kotlin Symbol Processor (KSP) that generates multiple DTOs from a single class. It helps reduce boilerplate by automatically creating DTO classes and their corresponding mapping functions.

Inspired by tools like Django’s ability to include or exclude fields when generating Form or Serializer classes from a Model, or like TypeScript’s utility types (Pick, Omit, etc.), KDTO brings similar flexibility to Kotlin. It lets you declaratively control which properties to include or exclude when generating DTOs from your data classes — no need for repetitive mapping or boilerplate.

## Install
In your `build.gradle.kts` apply the plugin:
````kotlin
plugins {
    kotlin("jvm") version "2.2.0"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" // KSP required
    id("io.github.mjavoso.kdto.plugin") version "1.0.0-beta01"
}

repositories {
    mavenCentral()
}
````

## Usage

### Classes with @Dto annotation
Annotate your data class with the @Dto annotation and provide one or more DtoSpec configurations:

````kotlin
@Dto(
    dtoSpec = [DtoSpec("UserDTO", exclude = ["password"])]
)
data class User(val id: Int, val name: String, val password: String)
````

The plugin will auto generate the following class:

```kotlin
data class UserDTO(val id: Int, val name: String)
fun User.toUserDTO(): UserDTO = UserDTO(
    id = this.id,
    name = this.name,
)
```

### Classes with @DtoDef

You can also create your DTOs with the `@DtoDef` annotation, which gives you more control over the generation, unlike `@Dto` with basic configuration.

```kotlin
// Spring Boot example
@kotlinx.serialization.Serializable // Make class also compatible with kotlinx.serialization API
data class User(
    val id: Int,
    @field:NotBlank(message = "First name is required")
    val name: String,
    val lastName: String,
    val password: String
)

@DtoDef(
    sourceClass = User::class,
    exclude = ["id"]
)
private interface UserRequest {
    @DtoProperty(
        from = "name",
        includeSourceAnnotations = false
    )
    @field:MyAwesomeAnnotation // Only annotations in this interface will be in generated DTO
    val firstName: String
    val customField: Int // Field not present in User class
}
```

One of the requirements is that the annotation should be used in interfaces, and one of these:
1. It must be private, to avoid confusion between choosing the interface or the DTO data class
2. You must provide the annotation parameter "from". Currently, it does not validate if the field equals to the interface name, so if you give the same name, it is a way to bypass the restriction.

Generated DTO class:
```kotlin
@kotlinx.serialization.Serializable
data class UserRequest(
    @field:MyAwesomeAnnotation // <- NotBlank not included
    val firstName: String,
    val lastName: String,
    val password: String,
    val customField: Int
)

fun User.toUserRequest(customField: Int): UserRequest = UserRequest(
    firstName = this.name,
    lastName = this.lastName,
    password = this.password,
    customField = customField
)
```

Generated mapper function will require you to pass fields that do not exist in the source class.

## Generation

To generate the DTO classes, run the Gradle task generateKDto. You can do this from the command line:
```
.\gradlew :generateKDto
```

Or by using the Gradle tool window in IntelliJ IDEA, under the kdto task group.

## Features

- Supports multiple DTOs from a single source class.
- Extend your DTOs with more fields
- Optionally include or exclude specific properties.
- Generates clean data class DTOs.
- Generates mapping extension functions automatically.