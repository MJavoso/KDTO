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
}