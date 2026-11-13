// Copyright 2022-2026 The Connect Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.connectrpc.okhttp

import com.connectrpc.MethodSpec
import com.connectrpc.StreamType
import com.connectrpc.http.HTTPMethod
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.UnaryHTTPRequest
import io.ktor.http.Url
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies that the OkHttp client sends the HTTP QUERY method (RFC 10008) with a
 * request body, which OkHttp does not do on its own for unknown methods.
 */
class QueryMethodTest {

    @get:Rule val mockWebServerRule = MockWebServerRule()

    @Test
    fun `query request is sent with method and body`() {
        mockWebServerRule.server.enqueue(MockResponse.Builder().code(200).body("response").build())
        val response = send(HTTPMethod.QUERY)
        assertThat(response.status).isEqualTo(200)
        val recorded = mockWebServerRule.server.takeRequest()
        assertThat(recorded.method).isEqualTo("QUERY")
        assertThat(recorded.target).isEqualTo("/svc/Method")
        assertThat(recorded.headers["Content-Type"]).isEqualTo("application/proto")
        assertThat(recorded.headers["Content-Length"]).isEqualTo("7")
        assertThat(recorded.body?.utf8()).isEqualTo("message")
    }

    @Test
    fun `post request is still sent with body`() {
        mockWebServerRule.server.enqueue(MockResponse.Builder().code(200).body("response").build())
        send(HTTPMethod.POST)
        val recorded = mockWebServerRule.server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.body?.utf8()).isEqualTo("message")
    }

    @Test
    fun `method not allowed for query is surfaced without retry`() {
        mockWebServerRule.server.enqueue(MockResponse.Builder().code(405).addHeader("Allow", "POST").build())
        val response = send(HTTPMethod.QUERY)
        assertThat(response.status).isEqualTo(405)
        assertThat(mockWebServerRule.server.requestCount).isEqualTo(1)
    }

    private fun send(method: HTTPMethod): HTTPResponse {
        val client = ConnectOkHttpClient(OkHttpClient.Builder().build())
        val latch = CountDownLatch(1)
        var response: HTTPResponse? = null
        client.unary(
            UnaryHTTPRequest(
                url = Url(mockWebServerRule.server.url("/svc/Method").toString()),
                contentType = "application/proto",
                timeout = null,
                headers = emptyMap(),
                methodSpec = MethodSpec(
                    path = "svc/Method",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
                message = Buffer().write("message".encodeUtf8()),
                httpMethod = method,
            ),
        ) {
            response = it
            latch.countDown()
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
        return response!!
    }
}
