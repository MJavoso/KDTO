package com.marcode.kdto.processor.data

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal data class DtoProperty(
    val property: KSPropertyDeclaration,
    val from: String = "",
    val includeSourceAnnotations: Boolean = true,
    val extraAnnotations: List<KSAnnotation> = emptyList()
)
