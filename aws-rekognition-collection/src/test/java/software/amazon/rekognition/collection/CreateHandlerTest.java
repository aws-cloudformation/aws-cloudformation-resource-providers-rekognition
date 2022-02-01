package software.amazon.rekognition.collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.rekognition.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.rekognition.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.rekognition.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    RekognitionClient sdkClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private CreateHandler handler;

    private DescribeCollectionRequest describeCollectionRequest;

    private ListTagsForResourceRequest listTagsForResourceRequest;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new CreateHandler();

        ResourceModel initial = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();
        ResourceModel intermediate = ResourceModel.builder()
            .arn(TEST_COLLECTION_ARN)
            .build();

        describeCollectionRequest = Translator.translateToReadRequest(initial);
        listTagsForResourceRequest = Translator.translateToListTagsRequest(intermediate);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atMost(3)).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_CreateCollectionWithTags() {

        ResourceModel requestModel = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(TagHelper.convertToSet(TEST_TAGS))
            .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestModel)
            .build();

        CreateCollectionRequest createCollectionRequest = Translator.translateToCreateRequest(requestModel, request);

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

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                createCollectionRequest,
                proxyClient.client()::createCollection
            )).thenReturn(
            CreateCollectionResponse.builder()
                .collectionArn(TEST_COLLECTION_ARN)
                .faceModelVersion(TEST_FACE_MODEL_VERSION)
                .build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> createResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(createResponse).isNotNull();
        assertThat(createResponse.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(createResponse.getCallbackDelaySeconds()).isEqualTo(65);
        assertThat(createResponse.getResourceModel()).isEqualTo(requestModel);
        assertThat(createResponse.getResourceModels()).isNull();
        assertThat(createResponse.getMessage()).isNull();
        assertThat(createResponse.getErrorCode()).isNull();

        final ProgressEvent<ResourceModel, CallbackContext> readResponse = handler.handleRequest(proxy, request, createResponse.getCallbackContext(), proxyClient, logger);

        assertThat(readResponse).isNotNull();
        assertThat(readResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(readResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(readResponse.getResourceModel()).isEqualTo(expectedModel);
        assertThat(readResponse.getResourceModels()).isNull();
        assertThat(readResponse.getMessage()).isNull();
        assertThat(readResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateCollectionWithoutTags() {

        ResourceModel requestModel = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestModel)
            .build();
        CreateCollectionRequest createCollectionRequest = Translator.translateToCreateRequest(requestModel, request);

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

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                createCollectionRequest,
                proxyClient.client()::createCollection
            )).thenReturn(
            CreateCollectionResponse.builder()
                .collectionArn(TEST_COLLECTION_ARN)
                .faceModelVersion(TEST_FACE_MODEL_VERSION)
                .build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> createResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(createResponse).isNotNull();
        assertThat(createResponse.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(createResponse.getCallbackDelaySeconds()).isEqualTo(65);
        assertThat(createResponse.getResourceModel()).isEqualTo(requestModel);
        assertThat(createResponse.getResourceModels()).isNull();
        assertThat(createResponse.getMessage()).isNull();
        assertThat(createResponse.getErrorCode()).isNull();

        final ProgressEvent<ResourceModel, CallbackContext> readResponse = handler.handleRequest(proxy, request, createResponse.getCallbackContext(), proxyClient, logger);

        assertThat(readResponse).isNotNull();
        assertThat(readResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(readResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(readResponse.getResourceModel()).isEqualTo(expectedModel);
        assertThat(readResponse.getResourceModels()).isNull();
        assertThat(readResponse.getMessage()).isNull();
        assertThat(readResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForCreateCollection(
            InternalServerErrorException.builder().build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_CollectionAlreadyExists() {
        stubAndThrowExceptionForCreateCollection(
            ResourceAlreadyExistsException.builder().build(),
            new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, TEST_COLLECTION_NAME)
        );
    }

    @Test
    public void handleRequest_CollectionCreateDenied() {
        stubAndThrowExceptionForCreateCollection(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForCreateCollection(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForCreateCollection(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException() {
        stubAndThrowExceptionForCreateCollection(
            ServiceQuotaExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForCreateCollection(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    /**
     * Method to cover all exceptions in the "CreateCollection" part of create chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForCreateCollection(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        ResourceModel requestModel = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(TagHelper.convertToSet(TEST_TAGS))
            .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestModel)
            .build();
        CreateCollectionRequest createCollectionRequest = Translator.translateToCreateRequest(requestModel, request);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                createCollectionRequest,
                proxyClient.client()::createCollection
            )).thenThrow(rekEx);

        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
