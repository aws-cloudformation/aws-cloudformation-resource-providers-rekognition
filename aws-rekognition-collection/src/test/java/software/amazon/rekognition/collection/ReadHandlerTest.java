package software.amazon.rekognition.collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.rekognition.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    RekognitionClient sdkClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private ReadHandler handler;

    @Mock
    private ResourceHandlerRequest<ResourceModel> request;

    private DescribeCollectionRequest describeCollectionRequest;

    private ListTagsForResourceRequest listTagsForResourceRequest;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new ReadHandler();

        ResourceModel initial = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();
        ResourceModel intermediate = ResourceModel.builder()
            .arn(TEST_COLLECTION_ARN)
            .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(initial)
            .build();
        describeCollectionRequest = Translator.translateToReadRequest(initial);
        listTagsForResourceRequest = Translator.translateToListTagsRequest(intermediate);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_ResourceExistsWithoutTags() {

        final ResourceModel expectedModel = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .arn(TEST_COLLECTION_ARN)
            .tags(emptySet())
            .build();

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(
            DescribeCollectionResponse.builder()
                .collectionARN(TEST_COLLECTION_ARN)
                .faceModelVersion(TEST_FACE_MODEL_VERSION)
                .faceCount(TEST_FACE_COUNT)
                .creationTimestamp(TEST_TIMESTAMP)
                .build()
        );

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listTagsForResourceRequest,
                proxyClient.client()::listTagsForResource
            )).thenReturn(
            ListTagsForResourceResponse.builder()
                .build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceExistsWithTags() {

        final ResourceModel expectedModel = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .arn(TEST_COLLECTION_ARN)
            .tags(TagHelper.convertToSet(TEST_TAGS))
            .build();

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(
            DescribeCollectionResponse.builder()
                .collectionARN(TEST_COLLECTION_ARN)
                .faceModelVersion(TEST_FACE_MODEL_VERSION)
                .faceCount(TEST_FACE_COUNT)
                .creationTimestamp(TEST_TIMESTAMP)
                .build()
        );

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listTagsForResourceRequest,
                proxyClient.client()::listTagsForResource
            )).thenReturn(
            ListTagsForResourceResponse.builder()
                .tags(TEST_TAGS)
                .build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceAccessDenied() {
        stubAndThrowExceptionForDescribeCollection(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_NotExist() {
        stubAndThrowExceptionForDescribeCollection(
            ResourceNotFoundException.builder().message("RekognitionException").build(),
            new CfnNotFoundException("Collection", TEST_COLLECTION_NAME)
        );
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForDescribeCollection(
            InternalServerErrorException.builder().message("RekognitionException").build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameter() {
        stubAndThrowExceptionForDescribeCollection(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ThroughputExceeded() {
        stubAndThrowExceptionForDescribeCollection(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ResourceThrottled() {
        stubAndThrowExceptionForDescribeCollection(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    @Test
    public void handleRequest_TagAccessDenied() {
        stubAndThrowExceptionForListTagsForResource(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_TagNotExist() {
        stubAndThrowExceptionForListTagsForResource(
            ResourceNotFoundException.builder().message("RekognitionException").build(),
            new CfnNotFoundException("Collection", TEST_COLLECTION_NAME)
        );
    }

    @Test
    public void handleRequest_TagInternalServerError() {
        stubAndThrowExceptionForListTagsForResource(
            InternalServerErrorException.builder().message("RekognitionException").build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_TagInvalidParameter() {
        stubAndThrowExceptionForListTagsForResource(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_TagThroughputExceeded() {
        stubAndThrowExceptionForListTagsForResource(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_TagThrottled() {
        stubAndThrowExceptionForListTagsForResource(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    /**
     * Method to cover all exceptions in the "DescribeCollection" part of the Read chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForDescribeCollection(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        when(proxyClient
            .injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenThrow(rekEx);
        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }

    /**
     * Method to cover all exceptions in the "ListTags" part of the Read chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForListTagsForResource(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(
            DescribeCollectionResponse.builder()
                .collectionARN(TEST_COLLECTION_ARN)
                .faceModelVersion(TEST_FACE_MODEL_VERSION)
                .faceCount(TEST_FACE_COUNT)
                .creationTimestamp(TEST_TIMESTAMP)
                .build()
        );
        when(
            proxyClient
                .injectCredentialsAndInvokeV2(
                    listTagsForResourceRequest,
                    proxyClient.client()::listTagsForResource
                )).thenThrow(rekEx);
        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
