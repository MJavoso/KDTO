package com.marcode.kdto.processor.util

import com.google.devtools.ksp.symbol.KSAnnotation

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.getArgument(index: Int): T? {
    return arguments[index].value as? T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.getArgument(argumentName: String): T? {
    return arguments.firstOrNull { it.name?.asString() == argumentName }?.value as? T
}