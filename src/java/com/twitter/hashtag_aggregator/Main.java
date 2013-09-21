package com.twitter.hashtag_aggregator;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Module;

import com.twitter.common.application.AbstractApplication;
import com.twitter.common.application.AppLauncher;
import com.twitter.common.application.Lifecycle;
import com.twitter.common.application.ShutdownRegistry;
import com.twitter.common.application.modules.HttpModule;
import com.twitter.common.application.modules.LogModule;
import com.twitter.common.application.modules.StatsModule;
import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.common.args.constraints.Exists;
import com.twitter.common.args.constraints.Positive;
import com.twitter.common.base.Command;
import com.twitter.common.webassets.bootstrap.BootstrapModule;
import com.twitter.common.webassets.jquery.JQueryModule;
import com.twitter.hashtag_aggregator.api.ApiModule;
import com.twitter.hashtag_aggregator.model.ModelModule;
import com.twitter.hashtag_aggregator.text.TextModule;
import com.twitter.hbc.twitter4j.Twitter4jStatusClient;

public class Main extends AbstractApplication {

  @CmdLine(name = "num_model_updater_threads", help = "Number of model updater threads to spawn.")
  @Positive
  private static final Arg<Integer> NUM_MODEL_UPDATER_THREADS = Arg.create(1);

  @CmdLine(name = "replay_snapshot_file", help = "If set, replay from snapshot file.")
  @Exists
  private static final Arg<File> REPLAY_SNAPSHOT_FILE = Arg.create();

  @Inject
  private Twitter4jStatusClient twitter4jClient;

  @Inject
  private Lifecycle lifecycle;

  @Inject
  private ShutdownRegistry shutdownRegistry;

  @Override
  public Iterable<? extends Module> getModules() {
    List<Module> modules = Lists.newArrayList(
        new StatsModule(),
        new LogModule(),
        new HttpModule(),
        new TextModule(),
        new HosebirdClientModule(),
        new ModelModule(NUM_MODEL_UPDATER_THREADS.get()),
        new ApiModule(),
        new BootstrapModule(),
        new JQueryModule()
    );

    if (REPLAY_SNAPSHOT_FILE.hasAppliedValue()) {
      modules.add(new LocalHosebirdClientModule(REPLAY_SNAPSHOT_FILE.get()));
    } else {
      modules.add(new StreamingHosebirdClientModule());
    }

    return modules;
  }

  @Override
  public void run() {
    twitter4jClient.connect();

    for (int i = 0; i < NUM_MODEL_UPDATER_THREADS.get(); i++) {
      twitter4jClient.process();
    }

    shutdownRegistry.addAction(new Command() {
      @Override public void execute() throws RuntimeException {
        twitter4jClient.stop();
      }
    });

    if (REPLAY_SNAPSHOT_FILE.hasAppliedValue()) {
      while (!twitter4jClient.isDone()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      lifecycle.shutdown();
    } else {
      lifecycle.awaitShutdown();
    }
  }

  public static void main(String[] args) {
    AppLauncher.launch(Main.class, args);
  }
}
