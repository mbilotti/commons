package com.twitter.hashtag_aggregator.text;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExtractorImplTest {

  @Test
  public void testExtractorImpl() {
    ExtractorImpl tokenizer = new ExtractorImpl();
    assertEquals(ImmutableList.of("#foo", "#bar"),
        tokenizer.extractHashtags("hello #foo world #bar twitter"));
  }
}
