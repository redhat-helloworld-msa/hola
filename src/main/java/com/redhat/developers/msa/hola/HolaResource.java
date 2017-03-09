/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
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
package com.redhat.developers.msa.hola;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;

import com.redhat.developers.msa.hola.tracing.HolaHttpRequestInterceptor;
import com.redhat.developers.msa.hola.tracing.HolaHttpResponseInterceptor;
import com.redhat.developers.msa.hola.tracing.TracerResolver;

import feign.Logger;
import feign.Logger.Level;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.swagger.annotations.ApiOperation;

@Path("/")
public class HolaResource {

    private Tracer tracer = TracerResolver.getTracer();

    @Context
    private SecurityContext securityContext;

    @Context
    private HttpServletRequest servletRequest;

    @GET
    @Path("/hola")
    @Produces("text/plain")
    @ApiOperation("Returns the greeting in Spanish")
    public String hola() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
        String translation = ConfigResolver
            .resolve("hello")
            .withDefault("Hola de %s")
            .logChanges(true)
            // 5 Seconds cache only for demo purpose
            .cacheFor(TimeUnit.SECONDS, 5)
            .getValue();

        return String.format(translation, hostname);
    }

    @GET
    @Path("/hola-chaining")
    @Produces("application/json")
    @ApiOperation("Returns the greeting plus the next service in the chain")
    public List<String> holaChaining() {
        List<String> greetings = new ArrayList<>();
        greetings.add(hola());
        greetings.addAll(getNextService().aloha());
        return greetings;
    }

    @GET
    @Path("/hola-secured")
    @Produces("text/plain")
    @ApiOperation("Returns a message that is only available for authenticated users")
    public String holaSecured() {
        // this will set the user id as userName
        String userName = securityContext.getUserPrincipal().getName();

        if (securityContext.getUserPrincipal() instanceof KeycloakPrincipal) {
            @SuppressWarnings("unchecked")
            KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) securityContext.getUserPrincipal();

            // this is how to get the real userName (or rather the login name)
            userName = kp.getKeycloakSecurityContext().getToken().getName();
        }
        return "This is a Secured resource. You are logged as " + userName;

    }

    @GET
    @Path("/logout")
    @Produces("text/plain")
    @ApiOperation("Logout")
    public String logout() throws ServletException {
        servletRequest.logout();
        return "Logged out";
    }

    @GET
    @Path("/health")
    @Produces("text/plain")
    @ApiOperation("Used to verify the health of the service")
    public String health() {
        return "I'm ok";
    }

    /**
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a REST endpoint with
     * Hystrix fallback support.
     *
     * @return The feign pointing to the service URL and with Hystrix fallback.
     */
    private AlohaService getNextService() {
        String url = System.getenv("ALOHA_SERVER_URL");
        if (null == url || url.isEmpty()) {
            String host = System.getenv("ALOHA_SERVICE_HOST");
            String port = System.getenv("ALOHA_SERVICE_PORT");
            if (null == host) {
                url = "http://aloha:8080/";
            } else {
                url = String.format("http://%s:%s", host, port);
            }
        }

        Span parentSpan = (Span) servletRequest.getAttribute("tracing.requestSpan");

        final CloseableHttpClient httpclient;
        if (tracer != null) {
            Span span = tracer.buildSpan("GET").asChildOf(parentSpan).start();
            httpclient = HttpClients.custom()
                    .addInterceptorFirst(new HolaHttpRequestInterceptor(span))
                    .addInterceptorFirst(new HolaHttpResponseInterceptor(span))
                    .build();
        } else {
            httpclient = HttpClients.custom().build();
        }

        return HystrixFeign.builder()
                .logger(new Logger.ErrorLogger()).logLevel(Level.BASIC)
                .client(new ApacheHttpClient(httpclient))
                .decoder(new JacksonDecoder())
                .target(AlohaService.class, url, () -> Collections.singletonList("Aloha response (fallback)"));
    }
}
