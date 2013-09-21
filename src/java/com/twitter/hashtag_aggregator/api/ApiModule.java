package com.twitter.hashtag_aggregator.api;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import com.twitter.common.application.http.Registration;

public class ApiModule extends AbstractModule {

  @Override
  protected void configure() {
    // register the jersey guice container
    Registration.registerServlet(binder(), "/api", GuiceContainer.class, false);

    // make the jaxrs resource visible to jersey
    bind(Api.class).in(Singleton.class);

    bind(ObjectMapper.class).in(Singleton.class);

    // register static assets here
    Registration.registerHttpAsset(binder(),
        "/public/static.txt", // path where static asset should be served
        // name of file under src/resources/com/twitter/hashtag_aggregator/api
        Resources.getResource(ApiModule.class, "static.txt"),
        MediaType.TEXT_PLAIN, // content type
        false // if true, hides the path
        );
  }

  @Provides
  @Singleton
  protected JacksonJsonProvider provideJacksonJsonProvider(ObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }
}
