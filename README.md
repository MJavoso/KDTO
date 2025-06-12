# KDTO

**KDTO** is a Kotlin Symbol Processor (KSP) that generates multiple DTOs from a single class. It helps reduce boilerplate by automatically creating DTO classes and their corresponding mapping functions.

Inspired by tools like Django’s ability to include or exclude fields when generating Form or Serializer classes from a Model, or like TypeScript’s utility types (Pick, Omit, etc.), KDTO brings similar flexibility to Kotlin. It lets you declaratively control which properties to include or exclude when generating DTOs from your data classes — no need for repetitive mapping or boilerplate.

## Install
In your `build.gradle.kts` apply the plugin:
````
plugins {
    kotlin("jvm") version "2.1.21"
    id("com.google.devtools.ksp") version "2.1.21-2.0.1" // KSP required
    id("io.github.mjavoso.kdto.plugin") version "1.0.0-alpha02"
}

repositories {
    mavenCentral()
}
````

## Usage
Annotate your data class with the @Dto annotation and provide one or more DtoSpec configurations:

````
@Dto(
    dtoSpec = [DtoSpec("UserDTO", exclude = ["password"])]
)
data class User(val id: Int, val name: String, val password: String)
````

The plugin will auto generate the following class:

```
data class UserDTO(val id: Int, val name: String)
fun User.toUserDTO(): UserDTO = UserDTO(
    id = this.id,
    name = this.name,
)
```

To generate the DTO classes, run the Gradle task generateKDto. You can do this from the command line:
```
.\gradlew :generateKDto
```

Or by using the Gradle tool window in IntelliJ IDEA, under the kdto task group.

## Features

- Supports multiple DTOs from a single source class.
- Optionally include or exclude specific properties.
- Generates clean data class DTOs.
- Generates mapping extension functions automatically.

## ToDo
- [X] Being able to pass annotations to the DTOs, for example, if a class is annotated with `@Serializable`