// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.hello.resource;

import static io.vlingo.common.serialization.JsonSerialization.serialized;
import static io.vlingo.http.Response.Status.Created;
import static io.vlingo.http.Response.Status.NotFound;
import static io.vlingo.http.Response.Status.Ok;
import static io.vlingo.http.ResponseHeader.Location;
import static io.vlingo.http.ResponseHeader.headers;
import static io.vlingo.http.ResponseHeader.of;
import static io.vlingo.http.resource.ResourceBuilder.get;
import static io.vlingo.http.resource.ResourceBuilder.patch;
import static io.vlingo.http.resource.ResourceBuilder.post;
import static io.vlingo.http.resource.ResourceBuilder.resource;

import io.vlingo.actors.AddressFactory;
import io.vlingo.actors.World;
import io.vlingo.common.Completes;
import io.vlingo.hello.infra.DescriptionData;
import io.vlingo.hello.infra.GreetingData;
import io.vlingo.hello.infra.MessageData;
import io.vlingo.hello.infra.persistence.Queries;
import io.vlingo.hello.infra.persistence.QueryModelStoreProvider;
import io.vlingo.hello.model.Greeting;
import io.vlingo.http.Response;
import io.vlingo.http.resource.Resource;

public class GreetingResource {
  private final AddressFactory addressFactory;
  private final Queries queries;
  private final World world;

  public GreetingResource(World world) {
    this.world = world;
    this.addressFactory = world.addressFactory();
    this.queries = QueryModelStoreProvider.instance().queries;
  }

  public Completes<Response> defineGreeting(GreetingData data) {
    return Greeting
      .defineWith(world.stage(), data.message, data.description)
      .andThenTo(state -> Completes.withSuccess(Response.of(Created, headers(of(Location, greetingLocation(state.id))), serialized(GreetingData.from(state)))));
  }

  public Completes<Response> changeGreetingMessage(String greetingId, MessageData message) {
    return world.stage().actorOf(Greeting.class, addressFactory.from(greetingId))
            .andThenTo(greeting -> greeting.withMessage(message.value))
            .andThenTo(state -> Completes.withSuccess(Response.of(Ok, serialized(GreetingData.from(state)))))
            .otherwise(noGreeting -> Response.of(NotFound, greetingLocation(greetingId)));
  }

  public Completes<Response> changeGreetingDescription(String greetingId, DescriptionData description) {
    return world.stage().actorOf(Greeting.class, addressFactory.from(greetingId))
            .andThenTo(greeting -> greeting.withDescription(description.value))
            .andThenTo(state -> Completes.withSuccess(Response.of(Ok, serialized(GreetingData.from(state)))))
            .otherwise(noGreeting -> Response.of(NotFound, greetingLocation(greetingId)));
  }

  public Completes<Response> queryGreetings() {
    return queries.greetings()
            .andThenTo(data -> Completes.withSuccess(Response.of(Ok, serialized(data))));
  }

  public Completes<Response> queryGreeting(String greetingId) {
    return queries.greetingOf(greetingId)
            .andThenTo(data -> Completes.withSuccess(Response.of(Ok, serialized(data))))
            .otherwise(noData -> Response.of(NotFound, greetingLocation(greetingId)));
  }

  public Resource<?> routes() {
    return resource("Greeting Resource",
      post("/greetings")
        .body(GreetingData.class)
        .handle(this::defineGreeting),
      patch("/greetings/{greetingId}/message")
        .param(String.class)
        .body(MessageData.class)
        .handle(this::changeGreetingMessage),
      patch("/greetings/{greetingId}/description")
        .param(String.class)
        .body(DescriptionData.class)
        .handle(this::changeGreetingDescription),
      get("/greetings")
        .handle(this::queryGreetings),
      get("/greetings/{greetingId}")
        .param(String.class)
        .handle(this::queryGreeting));
  }

  private String greetingLocation(final String greetingId) {
    return "/greetings/" + greetingId;
  }
}
