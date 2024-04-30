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
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/")
public class HolaResource {

    @RestClient
    private AlohaService alohaService;

    @Inject
    JsonWebToken jwt;

    @Inject
    @ConfigProperty(name = "hola")
    private String translation;

    @GET
    @Path("/hola")
    @Produces("text/plain")
    @Operation(description = "Returns the greeting in Spanish")
    public String hola() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
        return String.format(translation, hostname);
    }

    @GET
    @Path("/hola-chaining")
    @Produces("application/json")
    @Operation(description = "Returns the greeting plus the next service in the chain")
    public List<String> holaChaining() {
        List<String> greetings = new ArrayList<>();
        greetings.add(hola());
        greetings.addAll(alohaService.aloha());
        return greetings;
    }

    @GET
    @Path("/hola-secured")
    @Produces("text/plain")
    @PermitAll
    @Operation(description = "Returns a message that is only available for authenticated users")
    public String holaSecured() {
        // this will set the user id as userName
        return "This is a Secured resource. You are logged as " + jwt.getName();
    }

    @GET
    @Path("/health")
    @Produces("text/plain")
    @Operation(description = "Used to verify the health of the service")
    @Traced(value = false)
    public String health() {
        return "I'm ok";
    }
}
