package com.twitter.hashtag_aggregator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import javax.inject.Singleton;

import com.google.common.base.Preconditions;
import com.google.inject.Exposed;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

import com.twitter.common.application.ShutdownRegistry;
import com.twitter.common.args.Arg;
import com.twitter.common.args.CmdLine;
import com.twitter.common.args.constraints.Exists;
import com.twitter.common.base.Command;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

/**
 * Guice binding module for a hosebird {@link Client} that connects to the streaming API.
 *
 * Required bindings:
 *
 * <ul>
 *   <li>{@code BlockingQueue&lt;String&gt;}: hosebird message queue</li>
 *   <li>{@link ShutdownRegistry}</li>
 * </ul>
 *
 * Exposed bindings:
 *
 * <ul>
 *   <li>{@link Client}</li>
 * </ul>
 */
public class StreamingHosebirdClientModule extends PrivateModule {
  private static final String CONSUMER_KEY = "consumerKey";
  private static final String CONSUMER_SECRET = "consumerSecret";
  private static final String TOKEN = "token";
  private static final String SECRET = "secret";

  @CmdLine(name = "config_file", help = "Configuration file containing credentials, etc.")
  @Exists
  private static final Arg<File> CONFIG_FILE = Arg.create();

  @Override
  protected void configure() {
    requireBinding(Key.get(new TypeLiteral<BlockingQueue<String>>() { }));
  }

  @Provides
  @Singleton
  Authentication providesHosebirdAuth() {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(CONFIG_FILE.get()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load credentials from: " + CONFIG_FILE.get(), e);
    }

    String consumerKey = checkProperty(properties, CONSUMER_KEY);
    String consumerSecret = checkProperty(properties, CONSUMER_SECRET);
    String token = checkProperty(properties, TOKEN);
    String secret = checkProperty(properties, SECRET);

    return new OAuth1(consumerKey, consumerSecret, token, secret);
  }

  @Provides
  @Singleton
  @Exposed
  Client providesClient(
      Authentication authentication,
      BlockingQueue<String> msgQueue,
      ShutdownRegistry shutdownRegistry) {
    ClientBuilder builder = new ClientBuilder()
        .hosts(new HttpHosts(Constants.STREAM_HOST))
        .authentication(authentication)
        .endpoint(new StatusesSampleEndpoint())
        .processor(new StringDelimitedProcessor(msgQueue));

    final Client client = builder.build();

    shutdownRegistry.addAction(new Command() {
      @Override public void execute() {
        client.stop();
      }
    });

    return client;
  }

  private static String checkProperty(Properties properties, String propertyName) {
    return Preconditions.checkNotNull(
        properties.getProperty(propertyName), "Property not set: " + propertyName);
  }
}
