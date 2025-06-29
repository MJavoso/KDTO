package com.marcode.kdto.annotations.definitions

import kotlin.reflect.KClass

/**
 * Defines an interface as a base for DTO generation.
 *
 * This annotation allows specifying a source class and configuring which properties
 * will be included in the generated DTO. Unlike [@Dto][com.marcode.kdto.annotations.Dto],
 * this annotation is applied to interfaces and allows adding additional properties
 * that don't exist in the original class.
 *
 * For the annotated interface, a corresponding data class
 * will be generated, along with an extension function that maps an instance of the
 * source class to its DTO.
 *
 * Important: The annotated interface must be either:
 * - Marked as `private` when using the interface name for the generated DTO, or
 * - Have an explicit [dtoName] specified
 *
 * This requirement prevents naming conflicts and ambiguous references, as the generated DTO
 * will use either the interface name (when [dtoName] is empty) or the specified [dtoName].
 * Without this restriction, you would have two types with the same name:
 * the interface and the generated DTO class.
 *
 * Example:
 * ```
 * @DtoDef(
 *     sourceClass = User::class,
 *     exclude = ["password"]
 * )
 * private interface UserView {
 *     val displayName: String // Additional field not present in User
 * }
 *
 * // Auto-generated:
 * data class UserView(
 *     val displayName: String,
 *     val id: Int,
 *     val name: String
 * )
 *
 * fun User.toUserView(displayName: String): UserView
 * ```
 *
 * @property sourceClass The source class from which base properties will be taken.
 * @property dtoName the name for the auto generated DTO. If empty, the DTO name will be the name of the annotated interface
 * @property include Array of property names to include. If empty, all properties except excluded ones are included.
 * @property exclude Array of property names to exclude.
 * @property includeSourceAnnotations Controls class-level annotation inheritance in the generated DTO:
 *   - When `true`: The generated DTO class will include both annotations from the source class
 *     AND any annotations defined on the interface
 *   - When `false`: The generated DTO class will only include annotations defined on the interface
 *   Note: This setting affects only class-level annotations. For property-level annotation inheritance,
 *   use [includePropertySourceAnnotations], or see [@DtoProperty][com.marcode.kdto.annotations.definitions.DtoProperty]
 * @property includePropertySourceAnnotations Controls property-level annotation inheritance in the generated DTO:
 *   - When `true`: Properties in the generated DTO will include both annotations from the source class properties
 *     AND any annotations defined on the interface properties
 *   - When `false`: Properties in the generated DTO will only include annotations defined on the interface properties
 *   Note: This is a global setting that can be overridden for individual properties using
 *   [@DtoProperty][com.marcode.kdto.annotations.definitions.DtoProperty]
 * @throws com.marcode.kdto.annotations.exceptions.PropertyNotFoundException if properties defined in [include] or [exclude] are not found on source class
 * @throws com.marcode.kdto.annotations.exceptions.DtoDefinitionConflictException when interface is not private and no [dtoName] is provided. For more information,
 * see [DtoDefinitionConflictException][com.marcode.kdto.annotations.exceptions.DtoDefinitionConflictException]'s documentation
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class DtoDef(
    val sourceClass: KClass<*>,
    val dtoName: String = "",
    val include: Array<String> = [],
    val exclude: Array<String> = [],
    val includeSourceAnnotations: Boolean = true,
    val includePropertySourceAnnotations: Boolean = true
)