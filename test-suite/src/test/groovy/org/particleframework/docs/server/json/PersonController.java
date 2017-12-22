/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.particleframework.docs.server.json;

import com.fasterxml.jackson.core.JsonParseException;
import io.reactivex.*;
import org.particleframework.http.HttpRequest;
import org.particleframework.http.HttpResponse;
import org.particleframework.http.HttpStatus;
import org.particleframework.http.annotation.*;
import org.particleframework.http.hateos.Link;
import org.particleframework.http.hateos.VndError;
import org.particleframework.web.router.annotation.*;
import org.particleframework.web.router.annotation.Error;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
// tag::class[]
@Controller("/people")
@Singleton
public class PersonController {

    Map<String, Person> inMemoryDatastore = new LinkedHashMap<>();
// end::class[]

    @Get("/")
    public Collection<Person> index() {
        return inMemoryDatastore.values();
    }

    @Get("/{name}")
    public Maybe<Person> get(String name) {
        if (inMemoryDatastore.containsKey(name)) {
            return Maybe.just(inMemoryDatastore.get(name));
        }
        return Maybe.empty();
    }

    // tag::single[]
    @Post("/")
    public Single<HttpResponse<Person>> save(@Body Single<Person> person) { // <1>
        return person.map(p -> {
                    inMemoryDatastore.put(p.getFirstName(), p); // <2>
                    return HttpResponse.created(p); // <3>
                }
        );
    }
    // end::single[]

    // tag::future[]
    public CompletableFuture<HttpResponse<Person>> save(@Body CompletableFuture<Person> person) {
        return person.thenApply(p -> {
                    inMemoryDatastore.put(p.getFirstName(), p);
                    return HttpResponse.created(p);
                }
        );
    }
    // end::future[]

    // tag::regular[]
    public HttpResponse<Person> save(@Body Person person) {
        inMemoryDatastore.put(person.getFirstName(), person);
        return HttpResponse.created(person);
    }
    // end::regular[]

    // tag::localError[]
    public HttpResponse<VndError> jsonError(HttpRequest request, JsonParseException jsonParseException) { // <1>
        VndError error = new VndError("Invalid JSON: " + jsonParseException.getMessage()) // <2>
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<VndError>status(HttpStatus.BAD_REQUEST, "Fix Your JSON")
                .body(error); // <3>
    }
    // end::localError[]


    @Get("/error")
    public String throwError() {
        throw new RuntimeException("Something went wrong");
    }

    // tag::globalError[]
    @Error // <1>
    public HttpResponse<VndError> error(HttpRequest request, Throwable e) {
        VndError error = new VndError("Bad Things Happened: " + e.getMessage()) // <2>
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<VndError>serverError()
                .body(error); // <3>
    }
    // end::globalError[]

    // tag::statusError[]
    @Error(status = HttpStatus.NOT_FOUND)
    public HttpResponse<VndError> notFound(HttpRequest request) { // <1>
        VndError error = new VndError("Page Not Found") // <2>
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<VndError>notFound()
                .body(error); // <3>
    }
    // end::statusError[]
}