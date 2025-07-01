package com.marcode.kdto.annotations.definitions

/**
 * Marks a property within a DTO definition as being mapped from a specific property
 * in the source class.
 *
 * This annotation can be used in conjunction with interfaces annotated with [DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 * to explicitly map a property in the target DTO to a differently named property
 * in the source class.
 *
 * @property from Specifies the name of the property in the source class to be used
 *                as the value for the annotated property in the DTO. If empty, processor will assume
 *                that a property with the same name exists in the source class.
 * @property includeSourceAnnotations Controls annotation inheritance for this specific property:
 *   - When `true`: The generated DTO property will include both annotations from the source property
 *     AND any annotations defined on this interface property
 *   - When `false`: The generated DTO property will only include annotations defined on this
 *     interface property.
 *
 *   This setting overrides the global setting defined in [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 * @throws com.marcode.kdto.annotations.exceptions.PropertyNotFoundException
 * When [from] is provided, but property doesn't exist in the source class.
 * @throws com.marcode.kdto.annotations.exceptions.DtoDefinitionConflictException if property is excluded on exclude
 * list of [DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class DtoProperty(
    val from: String = "",
    val includeSourceAnnotations: Boolean = true
)
