package com.marcode.kdto.annotations

/**
 * Marks a class as eligible for DTO generation.
 *
 * This annotation allows defining one or more [DtoSpec]s for the annotated class.
 *
 * For each DTO specified, a corresponding data class will be generated, along with
 * an extension function that maps an instance of the annotated class to its DTO.
 *
 * Example:
 * ```
 * @Dto(
 *     dtoSpec = [DtoSpec("UserDTO", exclude = ["password"])]
 * )
 * data class User(val id: Int, val name: String, val password: String)
 *
 * // Auto-generated:
 * data class UserDTO(val id: Int, val name: String)
 * fun User.toUserDTO(): UserDTO
 * ```
 *
 * @property dtoSpec A non-empty array of DTO specifications.
 * @property ignoreAnnotationDefaultValues Controls whether annotation default values should be ignored when generating the DTO.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Dto(
    val dtoSpec: Array<DtoSpec>,
    val ignoreAnnotationDefaultValues: Boolean = true
)
