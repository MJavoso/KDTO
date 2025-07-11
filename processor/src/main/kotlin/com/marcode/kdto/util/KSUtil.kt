package com.marcode.kdto.util

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

internal inline fun <reified T : Any> KSAnnotation.isOfType(classType: KClass<T>): Boolean {
    return this.annotationType.resolve()
        .declaration.run {
            qualifiedName?.asString() == classType.qualifiedName || simpleName.asString() == classType.simpleName
        }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.getArgument(index: Int): T? {
    return arguments[index].value as? T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.getArgument(argumentName: String): T? {
    return arguments.firstOrNull { it.name?.asString() == argumentName }?.value as? T
}

internal fun KSValueArgument.getFormattedValue(): String = getFormattedValue(this.value)

private fun getFormattedValue(value: Any?): String = when(value) {
    is String ->  "\"${value}\""
    is Char -> "'$value'"
    is Boolean, is Short, is Byte, is Int, is Float, is Double, is Long -> value.toString()
    is List<*> -> value.joinToString(separator = ", ", prefix = "[", postfix = "]") { getFormattedValue(it) }
    is KSType -> {
        val decl = value.declaration
        val className = decl.qualifiedName?.asString() ?: decl.simpleName.asString()
        "$className::class"
    }
    is KSName -> value.asString()
    else -> value.toString()
}

internal fun KSType.toTypeName(): TypeName {
    val decl = this.declaration
    val pkg = decl.packageName.asString()
    val simpleName = decl.simpleName.asString()

    val typeArguments = this.arguments.map { arg ->
        arg.type?.resolve()?.toTypeName() ?: STAR
    }

    val className = ClassName(pkg, simpleName)
    val typeName = if (typeArguments.isNotEmpty()) {
        className.parameterizedBy(typeArguments)
    } else {
        className
    }

    return if (this.nullability == Nullability.NULLABLE) {
        typeName.copy(nullable = true)
    } else {
        typeName
    }
}