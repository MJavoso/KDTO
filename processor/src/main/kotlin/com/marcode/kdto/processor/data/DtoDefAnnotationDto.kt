package com.marcode.kdto.processor.data

import com.google.devtools.ksp.symbol.KSType

internal data class DtoDefAnnotationDto(
    val sourceClass: KSType,
    val dtoName: String,
    val include: List<String>,
    val exclude: List<String>,
    val includeClassSourceAnnotations: Boolean,
    val includePropertySourceAnnotations: Boolean
)
