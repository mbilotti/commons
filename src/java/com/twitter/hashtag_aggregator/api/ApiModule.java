package com.twitter.hashtag_aggregator.api;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
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
  }

  /*


  @Provides
  @Singleton
  @Internal
  protected ObjectMapper provideObjectMapper() {
    // configured Jackson ObjectMapper to deal with thrift types
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }
  */

  @Provides
  @Singleton
  protected JacksonJsonProvider provideJacksonJsonProvider(ObjectMapper mapper) {
    return new JacksonJsonProvider(mapper);
  }
}
