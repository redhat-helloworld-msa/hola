/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
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
package com.redhat.developers.msa.hola;

import java.io.File;
import java.io.FileNotFoundException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.undertow.WARArchive;

import com.redhat.developers.msa.hola.tracing.TracerResolver;

/**
 * @author rafaelbenevides
 *
 */
public class Main {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Instantiate the container
        Swarm swarm = new Swarm(args);

        // Create one or more deployments
        WARArchive deployment = ShrinkWrap.create(WARArchive.class);

        // Add resource to deployment
        deployment.addPackage(Main.class.getPackage());
        deployment.addPackage(TracerResolver.class.getPackage());
        deployment.addAllDependencies();

        // Add Web resources
        deployment.addAsWebResource(
            new ClassLoaderAsset("index.html", Main.class.getClassLoader()), "index.html");
        deployment.addAsWebInfResource(
            new ClassLoaderAsset("WEB-INF/web.xml", Main.class.getClassLoader()), "web.xml");
        deployment.addAsWebInfResource(
            new ClassLoaderAsset("WEB-INF/beans.xml", Main.class.getClassLoader()), "beans.xml");

        // If There's a KEYCLOAK_SERVER_URL env var, then read the file
        if (System.getenv("KEYCLOAK_AUTH_SERVER_URL") != null) {
            String keyCloackFile = System.getenv("KEYCLOAK_FILE");
            if (null == keyCloackFile){
                throw new FileNotFoundException("You set KEYCLOAK_AUTH_SERVER_URL, but you have forgotten to specify the KEYCLOAK_FILE env var.");
            }
            deployment.addAsWebInfResource(new File(keyCloackFile));
        }

        swarm.start().deploy(deployment);

    }

}
