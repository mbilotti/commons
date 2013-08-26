package com.twitter.hashtag_aggregator.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * Streaming {@link Model} implementation that maintains the top {@link Hashtag}s observed within
 * a sliding window of recent tweets.
 */
class SlidingWindowModelImpl implements Model {
  private final Interner<String> interner = Interners.newStrongInterner();

  /**
   * Maintains top Hashtag counts.
   */
  private final HashtagImpl[] hashtags;
  private int nextHashtag = 0;
  private final Reference2IntMap<String> hashtagLookup;

  /**
   * Circular buffer of Tweets observed; used to decrement Hashtag counts as Tweets exit the
   * sliding window.  {@code nextTweet} points to the index of the next Tweet to exit the window,
   * and it is also the index where the next Tweet entering the window will be stored.
   */
  private final Tweet[] tweets;
  private int nextTweet = 0;

  /**
   * Hashtag at the top of the {@code minHeap} will be the first to be evicted once we hit
   * {@code maxHashtags}.
   */
  private final Heap<HashtagImpl> minHeap;

  /**
   * Hashtags sorted in descending order of frequency.
   */
  private final Heap<HashtagImpl> maxHeap;

  /**
   * Snapshot of {@code maxHeap} used to satisfy queries.
   */
  private final Heap<HashtagImpl> queryHeap;

  SlidingWindowModelImpl(int maxHashtags, int windowSize) {
    Preconditions.checkArgument(maxHashtags > 0, "maxHashtags must be positive!");
    Preconditions.checkArgument(windowSize > 0, "windowSize must be positive!");

    hashtags = new HashtagImpl[maxHashtags];
    hashtagLookup = new Reference2IntOpenHashMap(maxHashtags);
    hashtagLookup.defaultReturnValue(-1);

    tweets = new Tweet[windowSize];

    minHeap = new Heap(hashtags, HashtagImpl.EVICTION_COMPARATOR);
    maxHeap = new Heap(hashtags);
    queryHeap = new Heap(hashtags);
  }

  @Override
  public synchronized void report(Map<String, Integer> hashtagToCountMap) {

    /**
     * Keep track of Hashtag indices that need to be updated.
     */
    IntSet dirtyHashtags = new IntOpenHashSet();

    /**
     * Decrement Hashtag counts for the Tweet exiting the sliding window, if any.
     */
    Tweet exitingTweet = tweets[nextTweet];
    if (exitingTweet != null) {
      // decrement counts for Hashtags still in the Model
      for (Entry<String, Integer> entry : exitingTweet.getCounts().entrySet()) {
        String hashtag = interner.intern(entry.getKey());
        int index = hashtagLookup.getInt(hashtag);
        if (index != -1) {
          hashtags[index].decrement(entry.getValue());
          dirtyHashtags.add(index);
        }
      }
    }

    /**
     * Save counts for the current Tweet entering the sliding window.
     */
    tweets[nextTweet] = new Tweet(hashtagToCountMap);

    /**
     * Increment {@code nextTweet} circularly.
     */
    nextTweet++;
    if (nextTweet == tweets.length) {
      nextTweet = 0;
    }

    /**
     * Increment counts for Hashtags in the current Tweet.
     */
    for (Entry<String, Integer> entry : hashtagToCountMap.entrySet()) {
      String hashtag = interner.intern(entry.getKey());
      int count = entry.getValue();
      int index = hashtagLookup.getInt(hashtag);

      if (index == -1) {
        // interned hashtag String is not already in the Model
        if (nextHashtag < hashtags.length) {
          // there is space in the Model, so simply add it
          index = nextHashtag++;

          hashtags[index] = new HashtagImpl(hashtag, count);
          hashtagLookup.put(hashtag, index);

          minHeap.enqueue(index);
          maxHeap.enqueue(index);
        } else {
          // there is no room in the Model, so evict the Hashtag with the lowest count
          index = minHeap.dequeue();

          // in case this index has been updated
          dirtyHashtags.remove(index);

          hashtags[index] = new HashtagImpl(hashtag, count);
          hashtagLookup.put(hashtag, index);

          minHeap.enqueue(index);
          maxHeap.changed(index);
        }
      } else {
        // interned term String is already present in the Model; so simply update weight
        hashtags[index].increment(count);
        dirtyHashtags.add(index);
      }
    }

    /**
     * Update dirty Hashtags.
     */
    for (int index : dirtyHashtags) {
      minHeap.changed(index);
      maxHeap.changed(index);
    }
  }

  @Override
  public List<Hashtag> query(int requested) {
    int n = requested > nextHashtag ? nextHashtag : requested;

    synchronized (this) {
      maxHeap.copyInto(queryHeap);
    }

    ImmutableList.Builder<Hashtag> builder = ImmutableList.builder();
    for (int i = 0; i < n; i++) {
      builder.add(new HashtagImpl(hashtags[queryHeap.dequeue()]));
    }
    return builder.build();
  }

  static class HashtagImpl implements Hashtag, Comparable<HashtagImpl> {
    static final Comparator<HashtagImpl> EVICTION_COMPARATOR = new Comparator<HashtagImpl>() {
      @Override public int compare(HashtagImpl h1, HashtagImpl h2) {
        // lowest count first, with ties broken in reverse lexical order
        int i = Double.compare(h1.count, h2.count);
        if (i == 0) {
          return h1.hashtag.compareTo(h2.hashtag);
        }
        return i;
      }
    };

    private final String hashtag;
    private int count;

    HashtagImpl(String hashtag, int count) {
      this.hashtag = hashtag;
      this.count = count;
    }

    HashtagImpl(Hashtag copy) {
      this.hashtag = copy.getHashtag();
      this.count = copy.getCount();
    }

    @Override
    public String getHashtag() {
      return hashtag;
    }

    @Override
    public int getCount() {
      return count;
    }

    void decrement(int n) {
      count -= n;
      if (count < 0) {
        count = 0;
      }
    }

    void increment(int n) {
      count += n;
    }

    @Override
    public int compareTo(HashtagImpl h) {
      // highest count first, with ties broken in lexical order
      int i = Double.compare(h.getCount(), count);
      if (i == 0) {
        return hashtag.compareTo(h.getHashtag());
      }
      return i;
    }
  }

  static class Tweet {
    private final Map<String, Integer> hashtagToCountMap;

    Tweet(Map<String, Integer> hashtagToCountMap) {
      this.hashtagToCountMap = ImmutableMap.copyOf(hashtagToCountMap);
    }

    Map<String, Integer> getCounts() {
      return hashtagToCountMap;
    }
  }

  /**
   * Heap that can be copied.
   */
  private static class Heap<T> extends ObjectHeapIndirectPriorityQueue<T> {

    Heap(T[] refArray) {
      super(refArray);
    }

    Heap(T[] refArray, Comparator<T> c) {
      super(refArray, c);
    }

    /**
     * Copies the inversion and heap arrays into the provided Heap.
     *
     * @param dest destination Heap into which to copy the arrays.
     */
    void copyInto(Heap<T> dest) {
      System.arraycopy(inv, 0, dest.inv, 0, inv.length);
      System.arraycopy(heap, 0, dest.heap, 0, heap.length);
      dest.size = size;
    }
  }
}
