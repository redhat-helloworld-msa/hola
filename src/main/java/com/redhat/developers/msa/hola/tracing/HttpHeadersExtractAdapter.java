/*
 * Copyright 2016 Juraci Paixão Kröhling
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.hola.tracing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;

import com.google.common.collect.Multimap;

import io.opentracing.propagation.TextMap;

/**
 * This is a "Extract Adapter", used to tell the OpenTracing Tracer how to extract span data
 * for building a Span Context. On this case, we require a container request context from JAX-RS
 * and convert its headers into a map stored internally.
 * If there are HTTP headers indicating a "Trace state", like HWKAPMTRACEID (in case
 * of Hawkular APM), the Span Context will use that information. Otherwise, it will start a new context
 * from scratch.
 *
 * Note that this class might not be needed in the future, once OpenTracing provides a JAX-RS integration
 * library.
 *
 * @author Juraci Paixão Kröhling
 */
public class HttpHeadersExtractAdapter implements TextMap {
    private final Map<String, String> headers;

    public HttpHeadersExtractAdapter(final ContainerRequestContext containerRequestContext) {
        this.headers = new HashMap<>(containerRequestContext.getHeaders().size());
        containerRequestContext.getHeaders().forEach((key, values) -> {
            headers.put(key, values.get(0));
        });
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return headers.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("HttpHeadersExtractAdapter should only be used with Tracer.extract()");
    }
}
