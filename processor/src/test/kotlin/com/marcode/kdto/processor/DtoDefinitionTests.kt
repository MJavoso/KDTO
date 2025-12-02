@file:OptIn(ExperimentalCompilerApi::class)

package com.marcode.kdto.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DtoDefinitionTests {
    @Test
    fun `@DtoDefinition generates class`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        annotation class ClassAnnotation

        @ClassAnnotation
        data class User(
            val id: Int? = null,
            val name: String,
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        private interface UserDto {
            val nickname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que la clase se genera correctamente
        assertContains(classContent, "@ClassAnnotation", ignoreCase = true, "Class annotation not found")
        assertContains(classContent, "data class UserDto(", ignoreCase = true)

        // Verificar que las propiedades se generan correctamente
        assertContains(classContent, "val nickname: String", ignoreCase = true, "Generated class does not include additional property")
        assertContains(classContent, "val name: String", ignoreCase = true, "Generated class does not include source property")
        assertContains(classContent, "val surname: String", ignoreCase = true, "Generated class does not include source property")

        // Verificar que la propiedad excluida no está presente
        assertFalse("val id: Int" in classContent, "Generated class includes excluded property")

        // Verificar que se genera la función de mapeo
        assertContains(classContent, "fun User.toUserDto(nickname: String): UserDto = UserDto(", ignoreCase = true, "Generated mapper function not found")
        assertContains(classContent, "nickname = nickname", ignoreCase = true, "Generated mapper does not handle additional property")
        assertContains(classContent, "name = this.name", ignoreCase = true, "Generated mapper does not handle source property")
        assertContains(classContent, "surname = this.surname", ignoreCase = true, "Generated mapper does not handle source property")
    }

    @Test
    fun `@DtoDefinition throws exception when interface is not private and no dtoName is provided`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        data class User(
            val id: Int? = null,
            val name: String,
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        interface UserDto {  // Note: interface is not private
            val nickname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        // Verificar que el mensaje de error contiene la información esperada
        assertContains(
            result.messages,
            "DtoDefinitionConflictException",
            ignoreCase = true,
            message = "Expected DtoDefinitionConflictException"
        )
    }

    @Test
    fun `@DtoDefinition generates class when interface is not private and dtoName is provided`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        data class User(
            val id: Int? = null,
            val name: String,
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            dtoName = "UserDto",
            exclude = ["id"]
        )
        interface UserDtoDefinition {  // Note: interface is not private
            val nickname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        assertContains(classContent, "data class UserDto(", ignoreCase = true, "Class name does not match")
    }

    @Test
    fun `@DtoDefinition supports property renaming with @DtoProperty`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        import com.marcode.kdto.annotations.definitions.DtoProperty
        
        data class User(
            val id: Int? = null,
            val name: String,
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        private interface UserDto {
            val nickname: String
            @DtoProperty(from = "surname")
            val lastName: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que la clase se genera correctamente
        assertContains(classContent, "data class UserDto(", ignoreCase = true)

        // Verificar que las propiedades se generan correctamente
        assertContains(classContent, "val nickname: String", ignoreCase = true, "Generated class does not include additional property")
        assertContains(classContent, "val name: String", ignoreCase = true, "Generated class does not include source property")
        assertContains(classContent, "val lastName: String", ignoreCase = true, "Generated class does not include renamed property")

        // Verificar que la propiedad original no está presente
        assertFalse("val surname: String" in classContent, "Generated class includes original property name")

        assertFalse("@DtoProperty" in classContent, "Generated class includes @DtoProperty annotation")

        // Verificar que se genera la función de mapeo correctamente
        assertContains(classContent, "fun User.toUserDto(nickname: String): UserDto = UserDto(", ignoreCase = true, "Generated mapper function not found")
        assertContains(classContent, "nickname = nickname", ignoreCase = true, "Generated mapper does not handle additional property")
        assertContains(classContent, "name = this.name", ignoreCase = true, "Generated mapper does not handle source property")
        assertContains(classContent, "lastName = this.surname", ignoreCase = true, "Generated mapper does not handle renamed property")
    }

    @Test
    fun `@DtoDefinition inherits all annotations by default`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        @Deprecated("Use NewUser instead")
        data class User(
            @Deprecated("Use identifier instead")
            val id: Int? = null,
            @JvmField
            val name: String,
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        private interface UserDto {
            val nickname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que las anotaciones de clase se heredan
        assertContains(classContent, "@Deprecated(", ignoreCase = true, "Class annotation not inherited")
        assertContains(classContent, "\"Use NewUser instead\"", ignoreCase = true, "Class annotation message not inherited")

        // Verificar que las anotaciones de propiedades se heredan
        assertContains(classContent, "@JvmField", ignoreCase = true, "Property annotation not inherited")
    }

    @Test
    fun `@DtoDefinition inherits annotations from interface definition`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.definitions.DtoDef
            
            annotation class ExtraAnnotation
            
            data class User(
                val id: Int? = null,
                val name: String,
                val surname: String
            )
        
            @DtoDef(
                sourceClass = User::class,
                exclude = ["id"]
            )
            @ExtraAnnotation
            private interface UserDto {
                val nickname: String
            }
        """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que la clase se genera correctamente
        assertContains(classContent, "data class UserDto(", ignoreCase = true)

        // Verificar que la anotación de la interfaz de definición se hereda al DTO generado
        assertContains(
            classContent,
            "@ExtraAnnotation",
            ignoreCase = true,
            "Generated DTO should inherit annotation from definition interface"
        )

        // Verificar que las propiedades se generan correctamente
        assertContains(classContent, "val nickname: String", ignoreCase = true)
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val surname: String", ignoreCase = true)

        // Verificar que la propiedad excluida no está presente
        assertFalse("val id: Int" in classContent, "Generated class includes excluded property")
    }

    @Test
    fun `@DtoDefinition only inherits property annotations when class annotations are disabled`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        @Deprecated("Use NewUser instead")
        data class User(
            @Deprecated("Use identifier instead")
            val id: Int? = null,
            @JvmField
            val name: String,
            @Deprecated
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"],
            includeClassSourceAnnotations = false
        )
        private interface UserDto {
            val nickname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()
        println("Class content:\n$classContent")

        // Verificar que las anotaciones de clase NO se heredan
        assertFalse("@Deprecated(\nmessage = \"Use NewUser instead" in classContent, "Class annotation was inherited when disabled")

        // Verificar que las anotaciones de propiedades SÍ se heredan
        assertContains(classContent, "@JvmField", ignoreCase = true, "Property annotation not inherited")
        assertContains(classContent, "@Deprecated", ignoreCase = true, "Property annotation not inherited")
    }

    @Test
    fun `@DtoDefinition does not inherit any annotations when both are disabled`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        @Deprecated("Use NewUser instead")
        data class User(
            @Deprecated("Use identifier instead")
            val id: Int? = null,
            @JvmField
            val name: String,
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"],
            includeClassSourceAnnotations = false,
            includePropertySourceAnnotations = false
        )
        private interface UserDto {
            val nickname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que las anotaciones de clase NO se heredan
        assertFalse("@Deprecated(" in classContent, "Class annotation was inherited when disabled")

        // Verificar que las anotaciones de propiedades NO se heredan
        assertFalse("@JvmField" in classContent, "Property annotation was inherited when disabled")
    }

    @Test
    fun `@DtoDefinition with disabled property annotations but enabled via @DtoProperty`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        import com.marcode.kdto.annotations.definitions.DtoProperty
        
        data class User(
            val id: Int? = null,
            @JvmField
            val name: String,
            @Deprecated("Use fullName instead")
            val surname: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"],
            includePropertySourceAnnotations = false
        )
        private interface UserDto {
            val nickname: String
            val name: String  // Sin @DtoProperty, no debería heredar @JvmField
            @DtoProperty  // Con @DtoProperty, debería heredar @Deprecated
            val surname: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que las propiedades están presentes
        assertContains(classContent, "val nickname: String", ignoreCase = true, "Generated class does not include additional property")
        assertContains(classContent, "val name: String", ignoreCase = true, "Generated class does not include source property")
        assertContains(classContent, "val surname: String", ignoreCase = true, "Generated class does not include source property")

        // Verificar que la anotación @JvmField NO se hereda para 'name' (ya que no usa @DtoProperty)
        //assertFalse(classContent.matches("""@JvmField\s+val name: String""".toRegex()), "Property annotation for name was inherited when disabled globally")
        assertFalse("@JvmField" in classContent, "Property annotation for name was inherited when disabled globally")

        // Verificar que la anotación @Deprecated SÍ se hereda para 'surname' (ya que usa @DtoProperty)
        assertContains(
            classContent,
            "@Deprecated(",
            ignoreCase = true,
            "Property annotation not inherited when enabled via @DtoProperty"
        )
        assertContains(
            classContent,
            "message = \"Use fullName instead\"",
            ignoreCase = true,
            "Property annotation not inherited when enabled via @DtoProperty"
        )
    }

    @Test
    fun `@DtoDefinition inherits annotations except for specific property with @DtoProperty`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        import com.marcode.kdto.annotations.definitions.DtoProperty
        
        data class User(
            val id: Int? = null,
            @JvmField
            val name: String,
            @Deprecated("Use fullName instead")
            val surname: String,
            @JvmName("getAge")
            val age: Int
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        private interface UserDto {
            val nickname: String
            val name: String  // Heredará @JvmField
            @DtoProperty(includeSourceAnnotations = false)  // No heredará @Deprecated
            val surname: String
            val age: Int  // Heredará @JvmName
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que las propiedades están presentes
        assertContains(classContent, "val nickname: String", ignoreCase = true)
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val surname: String", ignoreCase = true)
        assertContains(classContent, "val age: Int", ignoreCase = true)

        // Verificar que @JvmField se hereda para 'name'
        assertContains(classContent, "@JvmField", ignoreCase = true, "Property annotation @JvmField not inherited for 'name'")

        // Verificar que @Deprecated NO se hereda para 'surname'
        assertFalse("@Deprecated(" in classContent, "Property annotation @Deprecated was inherited for 'surname' when disabled")
        assertFalse("message = \"use fullName instead\"" in classContent, "Property annotation @Deprecated was inherited for 'surname' when disabled")

        // Verificar que @JvmName se hereda para 'age'
        assertContains(classContent, "@JvmName(name = \"getAge\")", ignoreCase = true, "Property annotation @JvmName not inherited for 'age'")
    }

    @Test
    fun `validateRegularProperties throws exception when property is in exclude list`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        data class User(
            val id: Int,
            val name: String,
            val email: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        private interface UserDto {
            val id: Int  // Esta propiedad está en la lista de exclusión
            val name: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        // Verificar que la compilación falla
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)

        // Verificar el mensaje de error
        assertContains(
            result.messages,
            "DtoDefinitionConflictException: Class UserDto declares excluded properties [id]",
            ignoreCase = true,
            message = "Expected error message about excluded property"
        )
    }

    @Test
    fun `validateRegularProperties throws exception when property type does not match source type`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        
        data class User(
            val id: Int,
            val name: String,
            val age: Int
        )
        
        @DtoDef(
            sourceClass = User::class
        )
        private interface UserDto {
            val id: Int
            val name: String
            val age: String  // Tipo diferente al de la clase fuente (String vs Int)
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        // Verificar que la compilación falla
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)

        // Verificar el mensaje de error
        assertContains(
            result.messages,
            "DtoDefinitionConflictException: Class UserDto declares properties with different types than the ones in the source class User",
            ignoreCase = true,
            message = "Expected error message about type mismatch"
        )
    }

    @Test
    fun `@DtoProperty throws exception when 'from' property does not exist in source class`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        import com.marcode.kdto.annotations.definitions.DtoProperty
        
        data class User(
            val id: Int,
            val name: String,
            val email: String
        )
        
        @DtoDef(
            sourceClass = User::class
        )
        private interface UserDto {
            @DtoProperty(from = "nonExistentProperty")  // Propiedad que no existe en User
            val customName: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "PropertyNotFoundException: Property customName in class UserDto references non-existent source property 'nonExistentProperty'",
            ignoreCase = true
        )
    }

    @Test
    fun `@DtoProperty throws exception when referencing excluded property`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        import com.marcode.kdto.annotations.definitions.DtoProperty
        
        data class User(
            val id: Int,
            val name: String,
            val email: String
        )
        
        @DtoDef(
            sourceClass = User::class,
            exclude = ["id"]
        )
        private interface UserDto {
            @DtoProperty(from = "id")  // Intentando usar una propiedad excluida
            val userId: Int
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            "Error message doesn't match any expected case",
            result.messages.contains("DtoDefinitionConflictException: Class UserDto references excluded property id", ignoreCase = true) ||
                    result.messages.contains("(variable is in excluded list.) Property userId in class UserDto references non-existent source property 'id'", ignoreCase = true)
        )
    }

    @Test
    fun `@DtoProperty throws exception when property types do not match`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.definitions.DtoDef
        import com.marcode.kdto.annotations.definitions.DtoProperty
        
        data class User(
            val id: Int,
            val name: String,
            val age: Int
        )
        
        @DtoDef(
            sourceClass = User::class
        )
        private interface UserDto {
            @DtoProperty(from = "age")  // Intentando mapear Int a String
            val userAge: String
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "DtoDefinitionConflictException: Property userAge in class UserDto has different type than source property 'age'",
            ignoreCase = true
        )
    }

    @Test
    fun `@DtoDefinition uses original annotation syntax when no explicit parameters provided`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.definitions.DtoDef
            
            annotation class DefaultParamsAnnotation(val value: String = "default")
            
            @DefaultParamsAnnotation
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
        
            @DtoDef(
                sourceClass = User::class,
                exclude = ["id"]
            )
            private interface UserDto {
                val nickname: String
            }
        """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que la clase se genera correctamente
        assertContains(classContent, "data class UserDto(", ignoreCase = true)

        // Verificar que la anotación usa la sintaxis original sin paréntesis
        assertContains(
            classContent,
            "@DefaultParamsAnnotation",
            ignoreCase = false,
            "Annotation should use original syntax without parentheses"
        )

        // Verificar que NO tiene paréntesis con el valor por defecto
        assertFalse(
            classContent.contains("@DefaultParamsAnnotation(") ||
                    classContent.contains("@DefaultParamsAnnotation(value = \"default\")"),
            "Annotation should not include default parameter value"
        )

        // Verificar que las propiedades se generan correctamente
        assertContains(classContent, "val nickname: String", ignoreCase = true)
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val email: String", ignoreCase = true)
    }

    @Test
    fun `@DtoDefinition includes explicit annotation parameter value in generated class`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.definitions.DtoDef
            
            annotation class DefaultParamsAnnotation(val value: String = "default")
            
            @DefaultParamsAnnotation("custom value")
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
        
            @DtoDef(
                sourceClass = User::class,
                exclude = ["id"]
            )
            private interface UserDto {
                val nickname: String
            }
        """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que la clase se genera correctamente
        assertContains(classContent, "data class UserDto(", ignoreCase = true)

        // Verificar que la anotación incluye el valor explícito proporcionado
        assertContains(
            classContent,
            "@DefaultParamsAnnotation(",
            ignoreCase = false,
            "Annotation should include parentheses when explicit value is provided"
        )

        assertContains(
            classContent,
            "\"custom value\"",
            ignoreCase = false,
            "Annotation should include the explicit parameter value"
        )

        // Verificar que las propiedades se generan correctamente
        assertContains(classContent, "val nickname: String", ignoreCase = true)
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val email: String", ignoreCase = true)
    }

    @Test
    fun `@DtoDefinition includes default annotation parameters when ignoreAnnotationDefaultValues is false`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.definitions.DtoDef
            
            annotation class DefaultParamsAnnotation(val value: String = "default")
            
            @DefaultParamsAnnotation
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
        
            @DtoDef(
                sourceClass = User::class,
                exclude = ["id"],
                ignoreAnnotationDefaultValues = false
            )
            private interface UserDto {
                val nickname: String
            }
        """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/UserDto.kt")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        // Verificar que la clase se genera correctamente
        assertContains(classContent, "data class UserDto(", ignoreCase = true)

        // Verificar que la anotación incluye paréntesis (comportamiento antiguo)
        assertContains(
            classContent,
            "@DefaultParamsAnnotation(",
            ignoreCase = false,
            "Annotation should include parentheses when ignoreAnnotationDefaultValues is false"
        )

        // Verificar que incluye el valor por defecto explícitamente
        assertContains(
            classContent,
            "value = \"default\"",
            ignoreCase = false,
            "Annotation should include default parameter value when ignoreAnnotationDefaultValues is false"
        )

        // Verificar que NO usa la sintaxis sin paréntesis
        val annotationWithoutParens = classContent.lines().any { line ->
            line.trim() == "@DefaultParamsAnnotation" && !line.contains("(")
        }
        assertFalse(
            annotationWithoutParens,
            "Annotation should not use syntax without parentheses when ignoreAnnotationDefaultValues is false"
        )

        // Verificar que las propiedades se generan correctamente
        assertContains(classContent, "val nickname: String", ignoreCase = true)
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val email: String", ignoreCase = true)
    }
}