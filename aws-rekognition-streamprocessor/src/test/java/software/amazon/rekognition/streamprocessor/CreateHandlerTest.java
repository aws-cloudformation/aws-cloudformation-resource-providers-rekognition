package software.amazon.rekognition.streamprocessor;

import java.time.Duration;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorRequest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.rekognition.streamprocessor.TestUtils.MOCK_CREDENTIALS;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_ARN;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_NAME;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_TAGS;
import static software.amazon.rekognition.streamprocessor.TestUtils.logger;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {
    @Mock
    RekognitionClient sdkClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private CreateHandler handler;

    private DescribeStreamProcessorRequest describeStreamProcessorRequest;

    private ListTagsForResourceRequest listTagsForResourceRequest;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new CreateHandler();

        ResourceModel initial = ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .build();
        ResourceModel intermediate = ResourceModel.builder()
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .build();

        describeStreamProcessorRequest = Translator.translateToDescribeRequest(initial);
        listTagsForResourceRequest = Translator.translateToListTagsRequest(intermediate);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atMost(3)).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_CreateStreamProcessorWithTagsForConnectedHome() {
        ResourceModel requestModel = TestUtils.getInputModelForConnectedHomeCreateHandler();
        requestModel.setTags(TagHelper.convertToSet(TEST_TAGS));
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(TestUtils.getConnectedHomeDescribeResponseForCreateHandler());
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .tags(TEST_TAGS)
                        .build()
        );

        CreateStreamProcessorRequest createStreamProcessorRequest = Translator.translateToCreateRequest(requestModel, request);
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        createStreamProcessorRequest,
                        proxyClient.client()::createStreamProcessor
                )).thenReturn(
                CreateStreamProcessorResponse.builder()
                        .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                        .build()
        );

        final ResourceModel expectedModel = TestUtils.getExpectedModelForConnectedHomeCreateHandler();
        expectedModel.setTags(TagHelper.convertToSet(TEST_TAGS));

        final ProgressEvent<ResourceModel, CallbackContext> createResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(createResponse).isNotNull();
        assertThat(createResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(createResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(createResponse.getResourceModel()).isEqualTo(expectedModel);
        assertThat(createResponse.getResourceModels()).isNull();
        assertThat(createResponse.getMessage()).isNull();
        assertThat(createResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStreamProcessorWithTagsForFaceSearch() {
        ResourceModel requestModel = TestUtils.getInputModelForFaceSearchCreateHandler();
        requestModel.setTags(TagHelper.convertToSet(TEST_TAGS));
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(TestUtils.getFaceSearchDescribeResponseForCreateHandler());
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .tags(TEST_TAGS)
                        .build()
        );

        CreateStreamProcessorRequest createStreamProcessorRequest = Translator.translateToCreateRequest(requestModel, request);
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        createStreamProcessorRequest,
                        proxyClient.client()::createStreamProcessor
                )).thenReturn(
                CreateStreamProcessorResponse.builder()
                        .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                        .build()
        );

        final ResourceModel expectedModel = TestUtils.getExpectedModelForFaceSearchCreateHandler();
        expectedModel.setTags(TagHelper.convertToSet(TEST_TAGS));

        final ProgressEvent<ResourceModel, CallbackContext> createResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(createResponse).isNotNull();
        assertThat(createResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(createResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(createResponse.getResourceModel()).isEqualTo(expectedModel);
        assertThat(createResponse.getResourceModels()).isNull();
        assertThat(createResponse.getMessage()).isNull();
        assertThat(createResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStreamProcessorWithoutTagsForConnectedHome() {
        ResourceModel requestModel = TestUtils.getInputModelForConnectedHomeCreateHandler();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(TestUtils.getConnectedHomeDescribeResponseForCreateHandler());
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .build()
        );

        CreateStreamProcessorRequest createStreamProcessorRequest = Translator.translateToCreateRequest(requestModel, request);
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        createStreamProcessorRequest,
                        proxyClient.client()::createStreamProcessor
                )).thenReturn(
                CreateStreamProcessorResponse.builder()
                        .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                        .build()
        );

        final ResourceModel expectedModel = TestUtils.getExpectedModelForConnectedHomeCreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> createResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(createResponse).isNotNull();
        assertThat(createResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(createResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(createResponse.getResourceModel()).isEqualTo(expectedModel);
        assertThat(createResponse.getResourceModels()).isNull();
        assertThat(createResponse.getMessage()).isNull();
        assertThat(createResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStreamProcessorWithoutTagsForFaceSearch() {
        ResourceModel requestModel = TestUtils.getInputModelForFaceSearchCreateHandler();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(TestUtils.getFaceSearchDescribeResponseForCreateHandler());
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .build()
        );

        CreateStreamProcessorRequest createStreamProcessorRequest = Translator.translateToCreateRequest(requestModel, request);
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        createStreamProcessorRequest,
                        proxyClient.client()::createStreamProcessor
                )).thenReturn(
                CreateStreamProcessorResponse.builder()
                        .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                        .build()
        );

        final ResourceModel expectedModel = TestUtils.getExpectedModelForFaceSearchCreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> createResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(createResponse).isNotNull();
        assertThat(createResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(createResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(createResponse.getResourceModel()).isEqualTo(expectedModel);
        assertThat(createResponse.getResourceModels()).isNull();
        assertThat(createResponse.getMessage()).isNull();
        assertThat(createResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForCreateStreamProcessor(
                InternalServerErrorException.builder().build(),
                new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_StreamProcessorAlreadyExists() {
        stubAndThrowExceptionForCreateStreamProcessor(
                ResourceAlreadyExistsException.builder().build(),
                new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, TEST_STREAM_PROCESSOR_NAME)
        );
    }

    @Test
    public void handleRequest_StreamProcessorCreateDenied() {
        stubAndThrowExceptionForCreateStreamProcessor(
                AccessDeniedException.builder().message("RekognitionException").build(),
                new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForCreateStreamProcessor(
                InvalidParameterException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForCreateStreamProcessor(
                ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException() {
        stubAndThrowExceptionForCreateStreamProcessor(
                ServiceQuotaExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForCreateStreamProcessor(
                ThrottlingException.builder().message("RekognitionException").build(),
                new CfnThrottlingException("CfnException")
        );
    }

    @Test
    public void testSetNameIfMissing() {
        ResourceModel requestModel = ResourceModel.builder().build();
        assertNull(requestModel.getName());
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .stackId("arn:aws:cloudformation:us-east-2:123456789012:stack/mystack-mynestedstack-sggfrhxhum7w/f449b250-b969-11e0-a185-5081d0136786")
                .logicalResourceIdentifier("TestResource")
                .clientRequestToken("7f59c3cf-00d2-40c7-b2ff-e75db0987002")
                .build();
        handler.setNameIfMissing(request);
        assertNotNull(requestModel.getName());
    }

    private void stubAndThrowExceptionForCreateStreamProcessor(final RekognitionException rekEx,
                                                               final BaseHandlerException cfnEx) {
        ResourceModel requestModel = TestUtils.getInputModelForConnectedHomeCreateHandler();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();
        CreateStreamProcessorRequest createStreamProcessorRequest = Translator.translateToCreateRequest(requestModel, request);
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        createStreamProcessorRequest,
                        proxyClient.client()::createStreamProcessor
                )).thenThrow(rekEx);

        assertThrows(
                cfnEx.getClass(),
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
