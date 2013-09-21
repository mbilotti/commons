package com.twitter.hashtag_aggregator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import com.twitter.hbc.SitestreamController;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.StatsReporter;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;

/**
 * Hosebird {@link Client} that replays a local snapshot file.
 */
public class ReplaySnapshotClient implements Client {
  private static final Logger LOG = Logger.getLogger(ReplaySnapshotClient.class.getName());

  private final File snapshotFile;
  private final int delayMillis;
  private final BlockingQueue<String> msgQueue;
  private final ReplayClientBase replayClientBase;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private volatile boolean isDone = false;

  /**
   * Constructs a ReplaySnapshotClient.
   *
   * @param snapshotFile snapshot file to replay.
   * @param replayTPS velocity of replay in tweets per second.
   * @param msgQueue hosebird message queue that will be populated.
   */
  public ReplaySnapshotClient(File snapshotFile, int replayTPS,  BlockingQueue<String> msgQueue) {
    this.snapshotFile = snapshotFile;
    delayMillis = 1000 / replayTPS;
    this.msgQueue = msgQueue;
    replayClientBase = new ReplayClientBase();
  }

  @Override
  public void connect() {
    executorService.execute(replayClientBase);
  }

  @Override
  public void reconnect() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stop() {
    executorService.shutdown();
  }

  @Override
  public void stop(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDone() {
    return isDone && msgQueue.isEmpty();
  }

  @Override
  public String getName() {
    return ReplaySnapshotClient.class.getName();
  }

  @Override
  public StreamingEndpoint getEndpoint() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SitestreamController createSitestreamController() {
    throw new UnsupportedOperationException();
  }

  @Override
  public StatsReporter.StatsTracker getStatsTracker() {
    throw new UnsupportedOperationException();
  }

  class ReplayClientBase implements Runnable {
    public void run() {
      LOG.info("Started replaying snapshot file: " + snapshotFile);

      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            new GZIPInputStream(new FileInputStream(snapshotFile))));
        String line;
        while ((line = br.readLine()) != null) {
          msgQueue.offer(line);
          try {
            Thread.sleep(delayMillis);
          } catch (InterruptedException e) {
            LOG.severe("Interrupted while waiting for next tweet!");
            Thread.currentThread().interrupt();
          }
        }
      } catch (IOException e) {
        LOG.severe("Caught IOException consuming next tweet: " + e.getMessage());
      } finally {
        isDone = true;
      }
    }
  }
}
