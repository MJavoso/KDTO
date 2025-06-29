package com.marcode.kdto.processor.data

import com.google.devtools.ksp.symbol.KSType
import kotlin.reflect.KClass

internal data class DtoDefAnnotation(
    val sourceClass: KSType,
    val dtoName: String,
    val include: List<String>,
    val exclude: List<String>,
    val includeSourceAnnotations: Boolean,
    val includePropertySourceAnnotations: Boolean
)
