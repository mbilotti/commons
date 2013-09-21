package com.twitter.hashtag_aggregator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.ImmutableList;
import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.common.args.constraints.Positive;
import com.twitter.common.stats.SampledStat;
import com.twitter.common.stats.Stats;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.twitter4j.Twitter4jStatusClient;

import twitter4j.StatusListener;

/**
 * Guice binding Module for the hashtag aggregator.
 *
 * Required bindings:
 *
 * <ul>
 *   <li>{@link Client}</li>
 * </ul>
 *
 * Exposed bindings:
 *
 * <ul>
 *   <li>{@code BlockingQueue&lt;String&gt;}</li>
 *   <li>{@link Twitter4jStatusClient}</li>
 * </ul>
 */
public class HosebirdClientModule extends PrivateModule {
  @CmdLine(name = "msg_queue_depth", help = "Max number of messages to queue.")
  @Positive
  private static final Arg<Integer> MSG_QUEUE_DEPTH = Arg.create(100000);

  @Override
  protected void configure() {
    requireBinding(Client.class);
  }

  @Provides
  @Exposed
  @Singleton
  BlockingQueue<String> providesMsgQueue() {
    final BlockingQueue<String> queue = new LinkedBlockingQueue<String>(MSG_QUEUE_DEPTH.get());
    Stats.export(new SampledStat<Integer>("msg_queue_depth", 0) {
      @Override public Integer doSample() {
        return queue.size();
      }
    });

    return queue;
  }

  @Provides
  @Exposed
  @Singleton
  Twitter4jStatusClient providesTwitter4jClient(
      Client client,
      BlockingQueue<String> msgQueue,
      StatusListener statusListener,
      ExecutorService executorService) {
    return new Twitter4jStatusClient(client, msgQueue, ImmutableList.of(statusListener),
        executorService);
  }
}
