package com.twitter.hashtag_aggregator;

import java.io.File;
import java.util.concurrent.BlockingQueue;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.hbc.core.Client;

/**
 * Guice binding module for a hosebird {@link Client} that reads from a local snapshot.
 *
 * Required bindings:
 *
 * <ul>
 *   <li>{@code BlockingQueue&lt;String&gt;}</li>
 * </ul>
 *
 * Provided bindings:
 *
 * <ul>
 *   <li>{@link Client}</li>
 * </ul>
 */
public class LocalHosebirdClientModule extends AbstractModule {
  private final File snapshotFile;

  @CmdLine(name = "snapshot_replay_tps", help = "velocity for snapshot replay in tweets per sec.")
  private static final Arg<Integer> SNAPSHOT_REPLAY_TPS = Arg.create(1000);

  public LocalHosebirdClientModule(File snapshotFile) {
    this.snapshotFile = snapshotFile;
  }

  @Override
  protected void configure() {
    requireBinding(Key.get(new TypeLiteral<BlockingQueue<String>>() { }));
  }

  @Provides
  @Singleton
  Client providesClient(BlockingQueue<String> msgQueue) {
    return new ReplaySnapshotClient(snapshotFile, SNAPSHOT_REPLAY_TPS.get(), msgQueue);
  }
}
