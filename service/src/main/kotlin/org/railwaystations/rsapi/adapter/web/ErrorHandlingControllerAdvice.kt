package org.railwaystations.rsapi.adapter.web

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.railwaystations.rsapi.core.ports.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
@ResponseBody
class ErrorHandlingControllerAdvice {
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest?
    ): ResponseEntity<ErrorResponse> {
        val details = ex.constraintViolations
            .map { obj: ConstraintViolation<*> -> obj.message }

        val error = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, details.toString())
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String?> {
        return ResponseEntity.badRequest().body(e.message)
    }

    @ExceptionHandler(ManageProfileUseCase.ProfileConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleProfileConflictException(e: ManageProfileUseCase.ProfileConflictException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)
    }

    @ExceptionHandler(ManageInboxUseCase.InboxEntryNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleInboxEntryNotFoundException(e: ManageInboxUseCase.InboxEntryNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(ManageInboxUseCase.InboxEntryNotOwnerException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleInboxEntryNotOwnerException(e: ManageInboxUseCase.InboxEntryNotOwnerException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
    }

}
