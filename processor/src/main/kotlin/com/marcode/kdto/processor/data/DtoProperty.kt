package com.marcode.kdto.processor.data

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Represents a property to be included in a Data Transfer Object (DTO) generation process and encapsulates
 * metadata about the property and its relation to the source class.
 *
 * @property property The Kotlin Symbol Processing (KSP) representation of the target property.
 * @property propertyExistsInSourceClass Indicates that the property already exists in the source class
 * or it is new.
 * @property from An optional value indicating the source of the property, such as a specific property name.
 * @property includeSourceAnnotations Indicates whether annotations from the source property should be included.
 * @property isSourceClassProperty Indicates whether the property is taken directly from the source class
 * and is not overriden in the annotated interface.
 * @property sourceAnnotations A list of annotations that come from the original property in the source class.
 */
internal data class DtoProperty(
    val property: KSPropertyDeclaration,
    val propertyExistsInSourceClass: Boolean,
    val from: String = "",
    val includeSourceAnnotations: Boolean = true,
    val isSourceClassProperty: Boolean = false,
    val sourceAnnotations: List<KSAnnotation> = emptyList()
)
