package com.twitter.hashtag_aggregator.model;

import java.util.Map;

import com.google.common.collect.Maps;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SlidingWindowModelImplTest {
  private static final String A = "#a";
  private static final String B = "#b";
  private static final String C = "#c";
  private static final String D = "#d";
  private static final String E = "#e";
  private static final String F = "#f";

  private Model model;

  @Before
  public void setUp() {
    model = new SlidingWindowModelImpl(4, 5);
  }

  @Test
  public void testSlidingWindowModelImpl() {
    report(A);
    assertModel(5, A);

    report(B, D);
    assertModel(2, A, B);

    report(C);
    assertModel(3, A, B, C);

    report(D);
    assertModel(3, D, D, A, B);

    report(B);
    assertModel(3, B, B, D, D, A);

    report(E);
    assertModel(3, B, B, D, D, C);

    report(B, D, F);
    assertModel(4, B, B, D, D, E, F);
  }

  private void report(String... hashtags) {
    model.report(toMap(hashtags));
  }

  private void assertModel(int n, String... hashtags) {
    Map<String, Integer> observed = Maps.newHashMap();
    for (Model.Hashtag hashtag : model.query(n)) {
      observed.put(hashtag.getHashtag(), hashtag.getCount());
    }
    assertEquals(toMap(hashtags), observed);
  }

  private Map<String, Integer> toMap(String... hashtags) {
    Map<String, Integer> map = Maps.newHashMap();
    for (String hashtag : hashtags) {
      if (map.containsKey(hashtag)) {
        map.put(hashtag, map.get(hashtag) + 1);
      } else {
        map.put(hashtag, 1);
      }
    }
    return map;
  }
}
