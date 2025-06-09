package com.marcode.kdto.annotations

/**
 * Defines the specification for a generated DTO class.
 *
 * If both [include] and [exclude] are empty, all properties from the original class will be included.
 *
 * @property dtoName The name of the generated DTO class.
 * @property include An optional list of property names to include in the DTO.
 * @property exclude An optional list of property names to exclude from the DTO.
 */
@Retention(AnnotationRetention.BINARY)
annotation class DtoSpec(
    val dtoName: String,
    val include: Array<String> = [],
    val exclude: Array<String> = []
)
