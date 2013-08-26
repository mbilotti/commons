package com.twitter.hashtag_aggregator.model;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.common.args.constraints.Positive;

import twitter4j.StatusListener;

/**
 * Guice binding Module for {@link SlidingWindowModelImpl}.
 *
 * Provided bindings:
 *
 * <ul>
 *   <li>{@link Model}</li>
 *   <li>{@link ExecutorService}</li>
 * </ul>
 */
public class ModelModule extends AbstractModule {

  @CmdLine(name = "max_hashtags", help = "Max number of hashtags to track.")
  @Positive
  private static final Arg<Integer> MAX_HASHTAGS = Arg.create(5000);

  @CmdLine(name = "sliding_window_size", help = "Sliding window size, in tweets.")
  @Positive
  private static final Arg<Integer> SLIDING_WINDOW_SIZE = Arg.create(18000);

  private final int numModelUpdaterThreads;

  public ModelModule(int numModelUpdaterThreads) {
    this.numModelUpdaterThreads = numModelUpdaterThreads;
  }

  @Override
  protected void configure() {
    bind(StatusListener.class).to(ModelUpdater.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  Model providesModel() {
    return new SlidingWindowModelImpl(MAX_HASHTAGS.get(), SLIDING_WINDOW_SIZE.get());
  }

  @Provides
  @Singleton
  ExecutorService providesExecutorService() {
    return Executors.newFixedThreadPool(numModelUpdaterThreads);
  }
}
