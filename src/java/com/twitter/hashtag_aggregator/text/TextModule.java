package com.twitter.hashtag_aggregator.text;

import com.google.inject.AbstractModule;

/**
 * Guice binding Module for the {@link Extractor}.
 *
 * Provided bindings:
 *
 * <ul>
 *   <li>{@link Extractor}</li>
 * </ul>
 */
public class TextModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Extractor.class).to(ExtractorImpl.class);
  }
}
