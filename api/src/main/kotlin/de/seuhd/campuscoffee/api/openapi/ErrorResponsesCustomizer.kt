package de.seuhd.campuscoffee.api.openapi

import de.seuhd.campuscoffee.api.exceptions.ErrorResponse
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.api.security.OpenEndpoint
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.utils.SpringDocAnnotationsUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.method.HandlerMethod
import java.util.UUID
import org.springframework.http.MediaType as SpringMediaType

/**
 * Adds to each operation of the OpenAPI definition the error responses that follow from rules for every endpoint: the
 * firewall and the reading of the request (400), the acting user (401, ADR-011), the `Accept` header (406), the
 * `Content-Type` of a request body (415), and an unexpected error (500). A controller function declares its other
 * statuses with `@ApiResponse`, such as [UserNotFoundResponse], and then needs `@ResponseStatus`, because springdoc
 * leaves out the success status otherwise. Every error response gets the schema of [ErrorResponse].
 */
@Component
class ErrorResponsesCustomizer :
    OperationCustomizer,
    OpenApiCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod
    ): Operation {
        val responses = operation.responses ?: ApiResponses()
        derivedResponses(operation, handlerMethod).forEach { (status, description) ->
            responses.putIfAbsent(status.value().toString(), ApiResponse().description(description))
        }
        val errorContent =
            Content().addMediaType(
                SpringMediaType.APPLICATION_JSON_VALUE,
                MediaType().schema(Schema<Any>().`$ref`(ERROR_RESPONSE_SCHEMA))
            )
        responses.filterKeys(::isError).values.forEach { it.content = errorContent }
        operation.responses =
            responses.toSortedMap().entries.fold(ApiResponses()) { sorted, (status, response) ->
                sorted.addApiResponse(status, response)
            }
        return operation
    }

    override fun customise(openApi: OpenAPI) {
        val components = openApi.components ?: Components().also { openApi.components = it }
        SpringDocAnnotationsUtils.extractSchema(components, ErrorResponse::class.java, null, null, openApi.specVersion)
    }

    private fun derivedResponses(
        operation: Operation,
        handlerMethod: HandlerMethod
    ): Map<HttpStatus, String> {
        val parameters = handlerMethod.methodParameters
        val hasBody = parameters.any { it.hasParameterAnnotation(RequestBody::class.java) }
        val pathVariables = parameters.filter { it.hasParameterAnnotation(PathVariable::class.java) }
        val hasResponseBody =
            operation.responses.orEmpty().any { (status, response) ->
                status.startsWith("2") && !response.content.isNullOrEmpty()
            }
        val examples =
            listOfNotNull(
                "an id that is not a UUID".takeIf { pathVariables.any { it.parameterType == UUID::class.java } },
                "a body that breaks a rule of its schema".takeIf { hasBody }
            ).ifEmpty { listOf("a path with a semicolon") }
        return buildMap {
            put(HttpStatus.BAD_REQUEST, "The request is malformed, such as ${examples.joinToString(" or ")}.")
            put(
                HttpStatus.UNAUTHORIZED,
                if (handlerMethod.hasMethodAnnotation(OpenEndpoint::class.java)) UNKNOWN_ACTING_USER else NO_ACTING_USER
            )
            if (hasResponseBody) put(HttpStatus.NOT_ACCEPTABLE, "The ${HttpHeaders.ACCEPT} header excludes $JSON.")
            if (hasBody) put(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The ${HttpHeaders.CONTENT_TYPE} is not $JSON.")
            put(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred, such as a failure of the database.")
        }
    }

    private fun isError(status: String): Boolean =
        status.toIntOrNull()?.let { HttpStatusCode.valueOf(it).isError } == true

    private companion object {
        const val ERROR_RESPONSE_SCHEMA = "ErrorResponse"
        const val JSON = SpringMediaType.APPLICATION_JSON_VALUE
        const val NO_ACTING_USER =
            "The header ${CurrentUserProvider.ACTING_USER_HEADER} is missing or blank or names no user."
        const val UNKNOWN_ACTING_USER = "The header ${CurrentUserProvider.ACTING_USER_HEADER} names no user."
    }
}
