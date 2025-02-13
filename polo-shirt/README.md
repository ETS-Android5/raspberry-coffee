# Polo Shirt 👕 - Feasibility test
### An annotated REST Server case study

---
An _**annotation-based**_ REST server, like JAX-RS (Jersey => Polo 😉, ahaha!), latching on the [http-tiny-server](../http-tiny-server).  
Consider it as an example of the way to process annotations. It does not come with the tools OpenAPI provides, 
like `yaml` specification, documentation, etc.  
This kind tooling is in reach, but would be yours to develop and implement.  
Again, the idea here is to facilitate the job, _on a very small machine_ like a Raspberry Pi Zero, that might not support
all the tools, libraries and frameworks, because of the small amount of memory available. It is not about re-inventing the wheel.

We are going to show how to implement the _same_ service, using `Polo-Shirt`, and then `Swagger`, which is sort of a de-facto standard, 
so we can compare the two  approaches.


In the Polo-Shirt example:
- The main is `restserver.PoloServer`.
- The request manager is `restserver.PoloRESTRequestManager implements http.RESTRequestManager`.
    - Annotations are managed in the method `buildOperationList`, and used in the method `processRequest`.
- An _**example**_ of implementation - with its annotations - is in `restserver.AnnotatedRESTImplementation`.
- Annotation definitions are in the package `restserver.annotations`.

> Build it using a `../gradlew shadowJar`  
> The run it with `./runAnnotatedServer.sh`   
>> The stdout in the console will tell you how to reach out to the server, with `curl`.

#### Then, the "standard" way: OpenAPI Spec (formerly Swagger)
OpenAPI is not related to this project, but it could be considered. Since recently, it is not only a way to document and generate your API, it can also be run directly on Jetty.
A single command can generate, build, package and run the services.

> Note the `yaml` structure has changed between version 2 and version 3.

- [Swagger](https://swagger.io/)
- [Swagger Codegen](https://swagger.io/tools/swagger-codegen/)
- [OpenAPI](https://openapi-generator.tech/)
- [Customizing the Swagger generator](https://github.com/swagger-api/swagger-codegen#customizing-the-generator)
- Online yaml editor: <https://app.swaggerhub.com/apis/OlivierLD/PoloShirt.101/1.0.0>

---

- <https://symfony.com/doc/current/components/yaml/yaml_format.html>
- <https://swagger.io/docs/specification/data-models/data-types/>
- <https://gettaurus.org/docs/YAMLTutorial/>

---

> Note: For Swagger and Jetty related stuff, use Java 8, _not_ 9 (for some `jaxb` reasons).

#### To install OpenAPI Generator
Look [here](https://openapi-generator.tech/docs/installation).

For Linux, (or any system)):
<!--
```
 $ wget https://oss.sonatype.org/content/repositories/releases/io/swagger/swagger-codegen-cli/2.2.1/swagger-codegen-cli-2.2.1.jar
 $ wget -e use_proxy=yes -e http_proxy=http://www-proxy.us.oracle.com:80 -e https_proxy=http://www-proxy.us.oracle.com:80 https://oss.sonatype.org/content/repositories/releases/io/swagger/swagger-codegen-cli/2.2.1/swagger-codegen-cli-2.2.1.jar
 $ wget https://repo1.maven.org/maven2/io/swagger/swagger-codegen-cli/3.0.0-rc1/swagger-codegen-cli-3.0.0-rc1.jar
 $ wget -e use_proxy=yes -e http_proxy=http://www-proxy.us.oracle.com:80 -e https_proxy=http://www-proxy.us.oracle.com:80 https://repo1.maven.org/maven2/io/swagger/swagger-codegen-cli/3.0.0-rc1/swagger-codegen-cli-3.0.0-rc1.jar
```
Also try
-->
```
 $ wget https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/5.3.0/openapi-generator-cli-5.3.0.jar -O openapi-generator-cli.jar
```

Then to run it (depending on the version you want):
```
 $ java -jar openapi-generator-cli.jar help
```

Try that:
```
 $ ./swagger.sh
 . . .
 $ cd generated/jaxrs
 $ mvn clean package jetty:run [--settings ../../settings.xml] -Dmaven.test.skip=true
```
> There is a `README.md` worth looking at, generated in the `generated/jaxrs` folder.

Then from another console:
```
 $ curl -X GET http://localhost:2345/oplist [ | jq ]
 {"code":4,"type":"ok","message":"magic!"}
```
> _Note_: in IntelliJ, right-click on the `pom.xml` in `generated/jaxrs`, and `Add as Maven Project`. 

Then flesh out your methods (that one in `oliv.io.impl.TopRootApiServiceImpl`, where it says `do some magic!`).  
For example:
```java
@Override
public Response greetV3(GreetingRequest body, SecurityContext securityContext) throws NotFoundException {
    // do some magic!
    GreetingMessage response = new GreetingMessage();
    response.setMessage(String.format("%s %s!",
        body != null && body.getSalutation() != null ? body.getSalutation() : "Hello",
        body != null && body.getName() != null ? body.getName() : "World"));
//  return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, response.toString())).build();
    return Response.ok().entity(response).build();
}
```
> Note: This generated code is semantically identical to what _**you**_ would write, for Polo-Shirt (at the top of this doc).  
> Compare `generated/jaxrs/src/gen/java/oliv/io/TopRootApi.java` (Swagger generated) and `src/main/java/restserver/AnnotatedRESTImplementation.java` (written with polo-shirt annotations).  
> Here is the polo-shirt code equivalent to the Swagger one above:
> ```java
>	@OperationDefinition(
>			verb = OperationDefinition.Verbs.POST,
>			path = "/greeting/v3",
>			description = "A simple example, taking a BodyParam, returning a Bean. Try curl -X POST http://localhost:2345/top-root/greeting/v3 -d '{ \"name\": \"Ducon\", \"salutation\": \"Salut\" }' | jq"
>	)
>	protected static Message greetV3(@BodyParam() GreetingObject greetingObj) {
>		String greeting = String.format("%s %s!",
>				(greetingObj.getSalutation() == null ? "Hello" : greetingObj.getSalutation()),
>				(greetingObj.getName() != null ? greetingObj.getName() : "world"));
>		Message message = new Message();
>		message.message = greeting;
>		return message;
>	}
> ```

Rebuild, re-run, and then (notice the simple-quotes around the payload)
```
curl -X POST "http://localhost:2345/top-root/greeting/v3" -H "accept: application/json" -H "Content-Type: application/json" -d '{"name":"Oliv","salutation":"Salut"}'
{"message":"Salut Oliv!"}
```

#### Customizing the code generation
As explained [here](https://openapi-generator.tech/docs/templating), you can modify existing code templates, or come up with your owns.

I have un-archived the jar `openapi-generator-cli.jar` into `~/.openapi-generator`.

For example, the code generated for my service looks like this:
```java
    @POST
    @Path("/greeting/v3")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Say Hello, in yet another way", description = "With one body parameter, returns result as an object. ", tags={ "Greeting" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "The final greeting", content = @Content(schema = @Schema(implementation = GreetingMessage.class))) })
    public Response greetV3(@Parameter(description = "An object, with name and salutation" ) GreetingRequest body, @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.greetV3(body,securityContext);
    }
```
specifically, the method signature is:
```java
    public Response greetV3(@Parameter(description = "An object, with name and salutation" ) GreetingRequest body, @Context SecurityContext securityContext)
```
I may very well need more parameters from the context, like `@Context ServletContext servletContext`, `@Context UriInfo uriInfo`, etc.
Let's say I want to add:
- `@Context Application app`
- `@Context ServletContext context`
- `@Context ServletConfig config`
- `@Context HttpHeaders headers`
- `@Context UriInfo uriInfo`
        
To do so, modify the mustache template named `api.mustache` (in `JavaJaxRS/libraries/jersey`) as follow (operation section only here), from
```java
{{#operation}}
    @{{httpMethod}}
    {{#subresourceOperation}}@Path("{{{path}}}"){{/subresourceOperation}}
    {{#hasConsumes}}@Consumes({ {{#consumes}}"{{{mediaType}}}"{{#hasMore}}, {{/hasMore}}{{/consumes}} }){{/hasConsumes}}
    {{#hasProduces}}@Produces({ {{#produces}}"{{{mediaType}}}"{{#hasMore}}, {{/hasMore}}{{/produces}} }){{/hasProduces}}
    @io.swagger.annotations.ApiOperation(value = "{{{summary}}}", notes = "{{{notes}}}", response = {{{returnBaseType}}}.class{{#returnContainer}}, responseContainer = "{{{returnContainer}}}"{{/returnContainer}}{{#hasAuthMethods}}, authorizations = {
        {{#authMethods}}@io.swagger.annotations.Authorization(value = "{{name}}"{{#isOAuth}}, scopes = {
            {{#scopes}}@io.swagger.annotations.AuthorizationScope(scope = "{{scope}}", description = "{{description}}"){{#hasMore}},
            {{/hasMore}}{{/scopes}}
        }{{/isOAuth}}){{#hasMore}},
        {{/hasMore}}{{/authMethods}}
    }{{/hasAuthMethods}}, tags={ {{#vendorExtensions.x-tags}}"{{tag}}"{{#hasMore}}, {{/hasMore}}{{/vendorExtensions.x-tags}} })
    @io.swagger.annotations.ApiResponses(value = { {{#responses}}
        @io.swagger.annotations.ApiResponse(code = {{{code}}}, message = "{{{message}}}", response = {{{baseType}}}.class{{#containerType}}, responseContainer = "{{{containerType}}}"{{/containerType}}){{#hasMore}},{{/hasMore}}{{/responses}} })
    public Response {{nickname}}(
        {{#allParams}}{{>queryParams}}{{>pathParams}}{{>headerParams}}{{>bodyParams}}{{>formParams}},
        {{/allParams}}@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.{{nickname}}({{#allParams}}{{#isFile}}inputStream, fileDetail{{/isFile}}{{^isFile}}{{paramName}}{{/isFile}},{{/allParams}}securityContext);
    }
{{/operation}}
```
to 
```java
{{#operation}}
    @{{httpMethod}}
    {{#subresourceOperation}}@Path("{{{path}}}"){{/subresourceOperation}}
    {{#hasConsumes}}@Consumes({ {{#consumes}}"{{{mediaType}}}"{{#hasMore}}, {{/hasMore}}{{/consumes}} }){{/hasConsumes}}
    {{#hasProduces}}@Produces({ {{#produces}}"{{{mediaType}}}"{{#hasMore}}, {{/hasMore}}{{/produces}} }){{/hasProduces}}
    @io.swagger.annotations.ApiOperation(value = "{{{summary}}}", notes = "{{{notes}}}", response = {{{returnBaseType}}}.class{{#returnContainer}}, responseContainer = "{{{returnContainer}}}"{{/returnContainer}}{{#hasAuthMethods}}, authorizations = {
        {{#authMethods}}@io.swagger.annotations.Authorization(value = "{{name}}"{{#isOAuth}}, scopes = {
            {{#scopes}}@io.swagger.annotations.AuthorizationScope(scope = "{{scope}}", description = "{{description}}"){{#hasMore}},
            {{/hasMore}}{{/scopes}}
        }{{/isOAuth}}){{#hasMore}},
        {{/hasMore}}{{/authMethods}}
    }{{/hasAuthMethods}}, tags={ {{#vendorExtensions.x-tags}}"{{tag}}"{{#hasMore}}, {{/hasMore}}{{/vendorExtensions.x-tags}} })
    @io.swagger.annotations.ApiResponses(value = { {{#responses}}
        @io.swagger.annotations.ApiResponse(code = {{{code}}}, message = "{{{message}}}", response = {{{baseType}}}.class{{#containerType}}, responseContainer = "{{{containerType}}}"{{/containerType}}){{#hasMore}},{{/hasMore}}{{/responses}} })
    public Response {{nickname}}(
        {{#allParams}}{{>queryParams}}{{>pathParams}}{{>headerParams}}{{>bodyParams}}{{>formParams}},
        {{/allParams}}@Context SecurityContext securityContext, @Context Application app, @Context ServletContext context, @Context ServletConfig config, @Context HttpHeaders headers, @Context UriInfo uriInfo)
    throws NotFoundException {
        return delegate.{{nickname}}({{#allParams}}{{#isFile}}inputStream, fileDetail{{/isFile}}{{^isFile}}{{paramName}}{{/isFile}},{{/allParams}}securityContext, app, context, config, headers, uriInfo);
    }
{{/operation}}
``` 
Same for `apiService.mustache`:
```java
  {{#operation}}
      public abstract Response {{nickname}}({{#allParams}}{{>serviceQueryParams}}{{>servicePathParams}}{{>serviceHeaderParams}}{{>serviceBodyParams}}{{>serviceFormParams}},{{/allParams}}SecurityContext securityContext)
      throws NotFoundException;
  {{/operation}}
``` 
becomes
```java
  {{#operation}}
      public abstract Response {{nickname}}({{#allParams}}{{>serviceQueryParams}}{{>servicePathParams}}{{>serviceHeaderParams}}{{>serviceBodyParams}}{{>serviceFormParams}},{{/allParams}}SecurityContext securityContext, Application app, ServletContext context, ServletConfig config, HttpHeaders headers, UriInfo uriInfo)
      throws NotFoundException;
  {{/operation}}
``` 
and `apiServiceImpl.mustache`:
```java
    {{#operation}}
    @Override
    public Response {{nickname}}({{#allParams}}{{>serviceQueryParams}}{{>servicePathParams}}{{>serviceHeaderParams}}{{>serviceBodyParams}}{{>serviceFormParams}}, {{/allParams}}SecurityContext securityContext)
    throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    {{/operation}}
```
becomes
```java
    {{#operation}}
    @Override
    public Response {{nickname}}({{#allParams}}{{>serviceQueryParams}}{{>servicePathParams}}{{>serviceHeaderParams}}{{>serviceBodyParams}}{{>serviceFormParams}}, {{/allParams}}SecurityContext securityContext, Application app, ServletContext context, ServletConfig config, HttpHeaders headers, UriInfo uriInfo)
    throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    {{/operation}}
```
You also need to add the corresponding imports in each template:
```java
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
``` 

Then the generation - with modified templates - is triggered by
```
$ openapi-generator generate \
                    --generator-name jaxrs-jersey \ 
                    --input-spec yaml/sample.yaml \
                    --output customTest \
                    --api-package oliv.api \ 
                    --template-dir ~/.openapi-generator/JavaJaxRS/libraries/jersey1 \ 
                    --verbose
```
> Note: the `--template-dir` is the directory where the template live in, not anything higher in the tree.

---
Good luck.

