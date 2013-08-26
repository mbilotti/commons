package com.twitter.hashtag_aggregator.api;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.google.inject.Inject;

import com.twitter.hashtag_aggregator.model.Model;

/**
 * REST API for the hashtag aggregator.
 */
@Path("/")
@Produces("application/json; charset=UTF-8")
public class Api {
  private final Model model;

  @Inject
  Api(Model model) {
    this.model = model;
  }

  @GET
  @Path("/model.json")
  public List<Model.Hashtag> model(@DefaultValue("100") @QueryParam("count") int count) {
    return model.query(count);
  }
}
