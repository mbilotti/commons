package com.twitter.hashtag_aggregator.text;

import java.util.List;

/**
 * Extracts hashtags from tweet text.
 */
public interface Extractor {

  /**
   * Extracts hashtags from the provided text.
   *
   * @param text text from which to extract hashtags.
   * @return List of hashtags extracted from the text; possibly empty.
   */
  List<String> extractHashtags(String text);
}
