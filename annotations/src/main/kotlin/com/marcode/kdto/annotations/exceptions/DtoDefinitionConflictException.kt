package com.marcode.kdto.annotations.exceptions

/**
 * Exception thrown during DTO generation when there's a potential naming conflict between
 * the interface definition and its generated DTO class.
 *
 * This exceptions occurs when any of these conditions are met:
 * 1. A property is declared, but it is also excluded on the
 * [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef] exclusion list.
 * 2. When both of these condutions occur:
 * - The interface annotated with [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 *    is not marked as `private`
 *  - No explicit `dtoName` is specified in the [@DtoDef][com.marcode.kdto.annotations.definitions.DtoDef]
 *    annotation
 * 3. When a property defined in the annotated interface declares "from" field in
 * [@DtoProperty][com.marcode.kdto.annotations.definitions.DtoProperty] annotation, but the property doesn't exist in the source class.
 *
 * @see DtoDef
 */

class DtoDefinitionConflictException(
    message: String
): Exception(message)