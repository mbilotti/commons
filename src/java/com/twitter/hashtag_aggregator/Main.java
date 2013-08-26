package com.twitter.hashtag_aggregator;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.twitter.common.application.AbstractApplication;
import com.twitter.common.application.AppLauncher;
import com.twitter.common.application.Lifecycle;
import com.twitter.common.application.modules.HttpModule;
import com.twitter.common.application.modules.LogModule;
import com.twitter.common.application.modules.StatsModule;
import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.common.args.constraints.Positive;
import com.twitter.hashtag_aggregator.api.ApiModule;
import com.twitter.hashtag_aggregator.model.ModelModule;
import com.twitter.hashtag_aggregator.text.TextModule;
import com.twitter.hbc.twitter4j.Twitter4jStatusClient;

public class Main extends AbstractApplication {

  @CmdLine(name = "num_model_updater_threads", help = "Number of model updater threads to spawn.")
  @Positive
  private static final Arg<Integer> NUM_MODEL_UPDATER_THREADS = Arg.create(1);

  @Inject
  private Twitter4jStatusClient twitter4jClient;

  @Inject
  private Lifecycle lifecycle;

  @Override
  public Iterable<? extends com.google.inject.Module> getModules() {
    return ImmutableList.of(
        new StatsModule(),
        new LogModule(),
        new HttpModule(),
        new HosebirdClientModule(),
        new TextModule(),
        new ModelModule(NUM_MODEL_UPDATER_THREADS.get()),
        new ApiModule()
    );
  }

  @Override
  public void run() {
    twitter4jClient.connect();

    for (int i = 0; i < NUM_MODEL_UPDATER_THREADS.get(); i++) {
      twitter4jClient.process();
    }

    lifecycle.awaitShutdown();
  }

  public static void main(String[] args) {
    AppLauncher.launch(Main.class, args);
  }
}
