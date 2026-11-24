package de.seuhd.campuscoffee.tests

import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult
import tools.jackson.module.kotlin.jacksonTypeRef

/**
 * The response to a request that [UserRequests] or [ConsumptionRequests] sent, for a test that checks more than the
 * status code. A successful response (status 2xx) has a [body] of type [T], such as a [UserJson]. Any other response
 * has an [error], the error response of Coffee Kitty.
 *
 * @param status  the status code, such as 201 or 404
 * @param headers the response headers, such as `Location` or `WWW-Authenticate`
 */
class Response<T : Any>(
    val status: Int,
    val headers: HttpHeaders,
    private val successBody: T?,
    private val errorBody: ErrorJson?
) {
    /** The body of a successful response. Fails if the status is not 2xx or if the response has no body. */
    val body: T
        get() = checkNotNull(successBody) { "The status $status is not 2xx, or the response has no body." }

    /** The error response. Fails if the status is 2xx or if the response has no body. */
    val error: ErrorJson
        get() = checkNotNull(errorBody) { "The status $status is 2xx, or the response has no body." }

    /**
     * Asserts that a setup request returned the status [expected]. Without this check, a later assertion could pass
     * only because the setup request failed.
     *
     * @param expected the status code of a successful setup request
     */
    fun requireStatus(expected: Int) {
        assertThat(status).`as`("setup request status").isEqualTo(expected)
    }
}

/**
 * Reads the status code, the headers, and the body of this response into a [Response]. For a status of 2xx, the
 * function parses the body as [T], and for any other status as an [ErrorJson]. It parses with [STRICT_JSON], so a body
 * with other fields than the target class fails the test.
 */
inline fun <reified T : Any> RestTestClient.ResponseSpec.toResponse(): Response<T> {
    val result = returnResult<ByteArray>()
    val successful = result.status.is2xxSuccessful
    val bytes = result.responseBody?.takeIf { it.isNotEmpty() }
    return Response(
        status = result.status.value(),
        headers = result.responseHeaders,
        successBody = if (successful) bytes?.let { STRICT_JSON.readValue(it, jacksonTypeRef<T>()) } else null,
        errorBody = if (successful) null else bytes?.let { STRICT_JSON.readValue(it, ErrorJson::class.java) }
    )
}
