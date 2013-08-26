package com.twitter.hashtag_aggregator.text;

import java.util.List;

import com.google.common.collect.ImmutableList;

import com.twitter.common.text.DefaultTextTokenizer;
import com.twitter.common.text.TextTokenizer;
import com.twitter.common.text.filter.TokenTypeFilter;
import com.twitter.common.text.token.TokenStream;
import com.twitter.common.text.token.attribute.CharSequenceTermAttribute;
import com.twitter.common.text.token.attribute.TokenType;
import com.twitter.common.text.token.attribute.TokenTypeAttribute;

/**
 * Concrete {@link Extractor} implementation.
 */
class ExtractorImpl implements Extractor {
  private final TokenStream tokenStream;

  ExtractorImpl() {
    TextTokenizer tokenizer = new DefaultTextTokenizer.Builder().setKeepPunctuation(true).build();
    tokenStream = new TokenTypeFilter.Builder(tokenizer.getDefaultTokenStream())
        .setMode(TokenTypeFilter.Mode.ACCEPT)
        .setTypesToFilter(TokenType.HASHTAG)
        .build();
  }

  @Override
  public List<String> extractHashtags(String text) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    tokenStream.reset(text);

    while (tokenStream.incrementToken()) {
      TokenTypeAttribute typeAttribute = tokenStream.getAttribute(TokenTypeAttribute.class);
      if (typeAttribute.getType() == TokenType.HASHTAG) {

        CharSequenceTermAttribute termAttribute = tokenStream
            .getAttribute(CharSequenceTermAttribute.class);

        // copy the token
        builder.add(new StringBuilder(termAttribute.getTermCharSequence()).toString());
      }
    }

    return builder.build();
  }
}
