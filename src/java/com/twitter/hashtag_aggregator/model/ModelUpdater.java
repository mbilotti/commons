package com.twitter.hashtag_aggregator.model;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.common.args.constraints.NotNegative;
import com.twitter.common.stats.SampledStat;
import com.twitter.common.stats.Stats;
import com.twitter.hashtag_aggregator.text.Extractor;

import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

/**
 * {@link StatusListener} implementation that updates the {@link Model}.
 */
class ModelUpdater implements StatusListener {
  private static final Logger LOG = Logger.getLogger(ModelUpdater.class.getName());
  private static final AtomicInteger STATUSES_COUNT = new AtomicInteger();

  @CmdLine(name = "status_log_interval", help = "Log every Nth status; or 0 to disable")
  @NotNegative
  private static final Arg<Integer> STATUS_LOG_INTERVAL = Arg.create(0);

  private final Model model;
  private final Extractor extractor;

  static {
    Stats.export(new SampledStat<Integer>("statuses_consumed", 0) {
      @Override public Integer doSample() {
        return STATUSES_COUNT.get();
      }
    });
  }

  @Inject
  ModelUpdater(Model model, Extractor extractor) {
    this.model = model;
    this.extractor = extractor;
  }

  @Override
  public void onStatus(Status status) {
    int interval = STATUS_LOG_INTERVAL.get();
    if (interval > 0) {
      int count = STATUSES_COUNT.get();
      if (count % interval == 0) {
        LOG.info("Status [" + count + "] @" + status.getUser().getScreenName() + ": "
            + status.getText());
      }
    }

    STATUSES_COUNT.incrementAndGet();

    Map<String, Integer> hashtagCounts = Maps.newHashMap();
    for (String hashtag : extractor.extractHashtags(status.getText())) {
      if (hashtagCounts.containsKey(hashtag)) {
        hashtagCounts.put(hashtag, hashtagCounts.get(hashtag) + 1);
      } else {
        hashtagCounts.put(hashtag, 1);
      }
    }
    model.report(hashtagCounts);
  }

  @Override
  public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
    // ignore
  }

  @Override
  public void onTrackLimitationNotice(int i) {
    // ignore
  }

  @Override
  public void onScrubGeo(long l, long l2) {
    // ignore
  }

  @Override
  public void onException(Exception e) {
    LOG.severe("caught Exception: " + e.getMessage());
  }
}
