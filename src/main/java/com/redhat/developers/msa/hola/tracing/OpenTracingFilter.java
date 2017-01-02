/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.hola.tracing;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * This is a JAX-RS filter that is applied to both the request and response phases.
 * During the request phase, we build a span context based on the incoming request data, which
 * might yield a span context for a trace started by an upstream server, or a span context
 * from scratch, in case we have no trace state.
 *
 * During the response phase, we finish the span we started on the request phase, adding the
 * status code to it.
 *
 * On a real world application, we could add some business info to the span.
 *
 * This filter might be replaced in the future by a more native integration between OpenTracing
 * and JAX-RS
 *
 */
@Provider
public class OpenTracingFilter implements ContainerResponseFilter, ContainerRequestFilter {
    private Tracer tracer = TracerResolver.getTracer();

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        SpanContext spanContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpHeadersExtractAdapter(containerRequestContext));
        Span requestSpan = tracer.buildSpan(containerRequestContext.getMethod())
                .asChildOf(spanContext)
                .start();
        Tags.HTTP_URL.set(requestSpan, containerRequestContext.getUriInfo().getRequestUri().toString());
        containerRequestContext.setProperty("tracing.requestSpan", requestSpan);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Span requestSpan = (Span) requestContext.getProperty("tracing.requestSpan");
        Tags.HTTP_STATUS.set(requestSpan, responseContext.getStatus());
        requestSpan.finish();
    }
}
