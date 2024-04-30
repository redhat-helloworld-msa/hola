# hola

hola microservice using Java EE (JAX-RS) on Quarkus/Microprofile

The detailed instructions to run _Red Hat Helloworld MSA_ demo, can be found at the following repository: <https://github.com/redhat-helloworld-msa/helloworld-msa>

## Build and Deploy hola locally

1.  Open a command prompt and navigate to the root directory of this microservice.
2.  Type this command to build and execute the application:

        mvn clean package

3.  This will create a uber jar at `target/quarkus-app/quarkus-run.jar` and execute it.
4.  The application will be running at the following URL: <http://localhost:8080/api/hola>
