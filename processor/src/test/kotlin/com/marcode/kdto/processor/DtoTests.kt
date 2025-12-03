@file:OptIn(ExperimentalCompilerApi::class)

package com.marcode.kdto.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class DtoTests {
    @Test
    fun `@Dto generates class`() {
        val source = SourceFile.kotlin("User.kt", """
            import com.marcode.kdto.annotations.Dto
            import com.marcode.kdto.annotations.DtoSpec

            @Dto(
                dtoSpec = [
                    DtoSpec("UserDto", exclude = ["id"])
                ]
            )
            data class User(
                val id: Int? = null,
                val name: String,
                val surname: String
            )
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
        assertContains(classContent, "data class UserDto(", ignoreCase = true, "Generated class does not match DtoSpec name")
        assertContains(classContent, "val name: String", ignoreCase = true, "Generated class does not include expected properties")
        assertContains(classContent, "val surname: String", ignoreCase = true, "Generated class does not include expected properties")

        assertFalse("val id: Int" in classContent, "Generated class includes excluded properties")

        assertContains(classContent, "fun User.toUserDto(): UserDto = UserDto(", ignoreCase = true, "Generated mapper function does not match DtoSpec name")
        assertContains(classContent, "name = this.name", ignoreCase = true, "Generated mapper function does not match DtoSpec name")
        assertContains(classContent, "surname = this.surname", ignoreCase = true, "Generated mapper function does not match DtoSpec name")
    }

    @Test
    fun `@Dto generates class with annotations`() {
        val source = SourceFile.kotlin(
            "User.kt", """
        import com.marcode.kdto.annotations.Dto
        import com.marcode.kdto.annotations.DtoSpec

        annotation class TestAnnotation
        annotation class PropertyAnnotation

        @TestAnnotation
        @Dto(
            dtoSpec = [
                DtoSpec("UserDto", includeAnnotations = true)
            ]
        )
        data class User(
            @PropertyAnnotation
            val name: String,
            val surname: String
        )
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

        // Verificar que la anotación de clase se ha copiado
        assertContains(classContent, "@TestAnnotation", ignoreCase = true, "Generated class does not include class annotation")

        // Verificar que la anotación de propiedad se ha copiado
        assertContains(classContent, "@PropertyAnnotation", ignoreCase = true, "Generated class does not include property annotation")
        assertContains(classContent, "val name: String", ignoreCase = true, "Generated class does not include expected property")

        // Verificar el resto de la estructura
        assertContains(classContent, "val surname: String", ignoreCase = true, "Generated class does not include expected property")
        assertContains(classContent, "fun User.toUserDto(): UserDto = UserDto(", ignoreCase = true, "Generated mapper function not found")
    }

    @Test
    fun `@Dto generates class without annotations`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.Dto
            import com.marcode.kdto.annotations.DtoSpec
    
            annotation class TestAnnotation
    
            @TestAnnotation
            @Dto(
                dtoSpec = [
                    DtoSpec("UserDto", includeAnnotations = false)
                ]
            )
            data class User(
                val name: String,
                val surname: String
            )
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
        assertContains(classContent, "data class UserDto(", ignoreCase = true)
        assertFalse("@TestAnnotation" in classContent, "Generated class includes source annotation")
    }

    @Test
    fun `@Dto handles array values in annotations without stack overflow`() {
        val source = SourceFile.kotlin(
            "Table.kt", """
        import com.marcode.kdto.annotations.Dto
        import com.marcode.kdto.annotations.DtoSpec
        
        annotation class AllowedIntValues(
            val values: IntArray,
            val message: String = "Invalid value"
        )
        
        @Dto(
            dtoSpec = [
                DtoSpec("TableDto", exclude = ["id"])
            ]
        )
        data class Table(
            val id: Int,
            @field:AllowedIntValues(
                values = [2, 4, 8, 12, 20],
                message = "Table capacity is not in the allowed range"
            )
            val capacity: Int,
            val number: Int
        )
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KDTOProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        val kotlinFile = compilation.kspSourcesDir.resolve("kotlin")
        val generatedClassPath = kotlinFile.resolve("generated/TableDto.kt")

        // Verify successful compilation
        assertFalse("StackOverflowError" in result.messages, "StackOverflowError found in compilation output")

        assertTrue("Generated class does not exist", generatedClassPath.exists())
        val classContent = generatedClassPath.readText().trimIndent()

        assertContains(
            classContent,
            "@field:AllowedIntValues",
            ignoreCase = true,
            message = "Array values not properly formatted in annotation"
        )
    }

    @Test
    fun `@Dto uses original annotation syntax when no explicit parameters provided`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.Dto
            import com.marcode.kdto.annotations.DtoSpec
            
            annotation class DefaultParamsAnnotation(val value: String = "default")
            
            @DefaultParamsAnnotation
            @Dto(
                dtoSpec = [
                    DtoSpec("UserDto", exclude = ["id"])
                ]
            )
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
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
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val email: String", ignoreCase = true)
        assertFalse("val id: Int" in classContent, "Generated class includes excluded property")
    }

    @Test
    fun `@Dto includes explicit annotation parameter value in generated class`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.Dto
            import com.marcode.kdto.annotations.DtoSpec
            
            annotation class DefaultParamsAnnotation(val value: String = "default")
            
            @DefaultParamsAnnotation("custom value")
            @Dto(
                dtoSpec = [
                    DtoSpec("UserDto", exclude = ["id"])
                ]
            )
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
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
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val email: String", ignoreCase = true)
        assertFalse("val id: Int" in classContent, "Generated class includes excluded property")
    }

    @Test
    fun `@Dto includes default annotation parameters when ignoreAnnotationDefaultValues is false`() {
        val source = SourceFile.kotlin(
            "User.kt", """
            import com.marcode.kdto.annotations.Dto
            import com.marcode.kdto.annotations.DtoSpec
            
            annotation class DefaultParamsAnnotation(val value: String = "default")
            
            @DefaultParamsAnnotation
            @Dto(
                dtoSpec = [
                    DtoSpec("UserDto", exclude = ["id"])
                ],
                ignoreAnnotationDefaultValues = false
            )
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
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
        assertContains(classContent, "val name: String", ignoreCase = true)
        assertContains(classContent, "val email: String", ignoreCase = true)
        assertFalse("val id: Int" in classContent, "Generated class includes excluded property")
    }
}