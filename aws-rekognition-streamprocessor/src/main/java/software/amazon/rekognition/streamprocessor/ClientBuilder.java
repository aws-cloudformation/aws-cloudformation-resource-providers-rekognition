package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.time.Duration;

public class ClientBuilder {
  private static final Integer CLIENT_TIMEOUT_SECONDS = 30;
  private static final Integer CLIENT_NUM_RETRIES = 3;

  private static final BackoffStrategy BACKOFF_THROTTLING_STRATEGY =
          EqualJitterBackoffStrategy.builder()
                  .baseDelay(Duration.ofSeconds(1))
                  .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF) // default is 20s
                  .build();
  private static final RetryPolicy RETRY_POLICY =
          RetryPolicy.builder()
                  .numRetries(CLIENT_NUM_RETRIES)
                  .retryCondition(RetryCondition.defaultRetryCondition())
                  .throttlingBackoffStrategy(BACKOFF_THROTTLING_STRATEGY)
                  .build();

  public static RekognitionClient getClient() {
    return RekognitionClient.builder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofSeconds(CLIENT_TIMEOUT_SECONDS))
                    .retryPolicy(RETRY_POLICY)
                    .build())
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
