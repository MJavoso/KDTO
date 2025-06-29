package com.marcode.kdto.annotations.exceptions

/**
 * Exception thrown during DTO generation when there's a potential naming conflict between
 * the interface definition and its generated DTO class.
 *
 * This exception occurs when both of these conditions are met:
 * 1. The interface annotated with [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 *    is not marked as `private`
 * 2. No explicit `dtoName` is specified in the [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 *    annotation
 *
 * To resolve this issue, either:
 * - Mark the interface as `private`, or
 * - Specify an explicit `dtoName` in the [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 *   annotation
 *
 * Example of valid definitions:
 * ```kotlin
 * // Option 1: Private interface
 * @DtoDef(sourceClass = User::class)
 * private interface UserView
 *
 * // Option 2: Public interface with explicit DTO name
 * @DtoDef(sourceClass = User::class, dtoName = "UserViewDto")
 * interface UserView
 * ```
 *
 * @see DtoDef
 */

class DtoDefinitionConflictException(
    message: String
): Exception(message)