package software.amazon.rekognition.collection;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String TEST_COLLECTION_NAME;
    protected static final String TEST_COLLECTION_ARN;
    protected static final String TEST_FACE_MODEL_VERSION;
    protected static final Instant TEST_TIMESTAMP;
    protected static final Long TEST_FACE_COUNT;
    protected static final Map<String, String> TEST_TAGS;
    protected static final DescribeCollectionResponse DEFAULT_DESCRIBE_RESPONSE;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        TEST_COLLECTION_NAME = "test";
        TEST_COLLECTION_ARN = String.format("arn:aws:rekognition:us-east-1:545579126031:collection/%s", TEST_COLLECTION_NAME);
        TEST_TIMESTAMP = Instant.parse("2021-01-01T00:00:00.000Z");
        TEST_FACE_MODEL_VERSION = "5";
        TEST_FACE_COUNT = 0L;
        TEST_TAGS = ImmutableMap.of("TEST_TAG_1", "TEST_VALUE_1", "TEST_TAG_2", "TEST_VALUE_2");
        DEFAULT_DESCRIBE_RESPONSE = DescribeCollectionResponse.builder()
                .collectionARN(TEST_COLLECTION_ARN)
                .faceModelVersion(TEST_FACE_MODEL_VERSION)
                .faceCount(TEST_FACE_COUNT)
                .creationTimestamp(TEST_TIMESTAMP)
                .build();
    }

    static ProxyClient<RekognitionClient> MOCK_PROXY(
        final AmazonWebServicesClientProxy proxy,
        final RekognitionClient sdkClient) {
        return new ProxyClient<RekognitionClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
            IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RekognitionClient client() {
                return sdkClient;
            }
        };
    }
}
