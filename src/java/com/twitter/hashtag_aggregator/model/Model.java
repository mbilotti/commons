package com.twitter.hashtag_aggregator.model;

import java.util.List;
import java.util.Map;

/**
 * Model that keeps track of top Hashtags.
 */
public interface Model {

  /**
   * Reports to the Model that one or more Hashtags has been observed in a tweet.
   *
   * @param hashtagToCountMap maps Hashtag to number of times it occurred in the tweet.
   */
  void report(Map<String, Integer> hashtagToCountMap);

  /**
   * Queries the Model for the top-n Hashtags.
   *
   * @param n number of top Hashtags to retrieve.
   * @return List of top-n Hashtags, in descending order.
   */
  List<Hashtag> query(int n);

  /**
   * A Hashtag and its associated count.
   */
  interface Hashtag {

    /**
     * Retrieves the textual form of the Hashtag, as used in the tweet.
     *
     * @return the textual form of the Hashtag, as used in the tweet.
     */
    String getHashtag();

    /**
     * Retrieves the number of times the Hashtag was observed.
     *
     * @return the number of times the Hashtag was observed.
     */
    int getCount();
  }
}
