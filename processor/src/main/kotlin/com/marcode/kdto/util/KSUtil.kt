package com.marcode.kdto.util

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.getArgument(index: Int): T? {
    return arguments[index].value as? T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.getArgument(argumentName: String): T? {
    return arguments.firstOrNull { it.name?.asString() == argumentName }?.value as? T
}

internal fun KSValueArgument.getFormattedValue(): String = when(val value = this.value) {
    is String ->  "\"${value}\""
    is Char -> "'$value'"
    is Boolean, is Short, is Byte, is Int, is Float, is Double, is Long -> value.toString()
    is List<*> -> value.joinToString(separator = ", ", prefix = "[", postfix = "]") { getFormattedValue() }
    is KSType -> {
        val decl = value.declaration
        val className = decl.qualifiedName?.asString() ?: decl.simpleName.asString()
        "$className::class"
    }
    is KSName -> value.asString()
    else -> value.toString()
}