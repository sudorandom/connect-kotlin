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

package com.connectrpc.protocols

/**
 * Configuration for enabling HTTP QUERY (RFC 10008) requests for the Connect
 * protocol.
 *
 * When enabled, unary RPCs that are side effect free are sent using the QUERY
 * method instead of POST. The request is otherwise identical to a POST: the
 * message is sent as the request body. QUERY replaces POST, not GET. If
 * [GETConfiguration] is also enabled, requests that qualify for GET are still
 * sent with GET, and the POST fallback of
 * [GETConfiguration.EnabledWithFallback] becomes QUERY.
 *
 * The gRPC and gRPC-Web protocols ignore this configuration.
 */
sealed class QUERYConfiguration {
    object Disabled : QUERYConfiguration() {
        override fun useQUERY(): Boolean {
            return false
        }
    }

    object Enabled : QUERYConfiguration() {
        override fun useQUERY(): Boolean {
            return true
        }
    }

    abstract fun useQUERY(): Boolean
}
