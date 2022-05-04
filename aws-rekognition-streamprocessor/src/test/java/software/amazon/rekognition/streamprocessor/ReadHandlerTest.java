package software.amazon.rekognition.streamprocessor;

import java.time.Duration;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.rekognition.streamprocessor.TestUtils.MOCK_CREDENTIALS;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_ARN;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_NAME;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_TAGS;
import static software.amazon.rekognition.streamprocessor.TestUtils.logger;

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

    private DescribeStreamProcessorRequest describeStreamProcessorRequest;

    private DescribeStreamProcessorResponse describeResponseForConnectedHome;
    private DescribeStreamProcessorResponse describeResponseForFaceSearch;

    private ListTagsForResourceRequest listTagsForResourceRequest;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new ReadHandler();

        ResourceModel initial = ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .build();
        ResourceModel intermediate = ResourceModel.builder()
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(initial)
                .build();
        describeStreamProcessorRequest = Translator.translateToDescribeRequest(initial);
        listTagsForResourceRequest = Translator.translateToListTagsRequest(intermediate);

        describeResponseForConnectedHome = TestUtils.getConnectedHomeDescribeResponseForReadHandler();
        describeResponseForFaceSearch = TestUtils.getFaceSearchDescribeResponseForReadHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_ResourceExistsWithoutTagsForConnectedHome() {

        final ResourceModel expectedModel = TestUtils.getExpectedModelForConnectedHomeReadHandler();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(describeResponseForConnectedHome);

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
    public void handleRequest_ResourceExistsWithoutTagsForFaceSearch() {

        final ResourceModel expectedModel = TestUtils.getExpectedModelForFaceSearchReadHandler();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(describeResponseForFaceSearch);

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
    public void handleRequest_ResourceExistsWithTagsForConnectedHome() {

        final ResourceModel expectedModel = TestUtils.getExpectedModelForConnectedHomeReadHandler();
        expectedModel.setTags(TagHelper.convertToSet(TEST_TAGS));

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(describeResponseForConnectedHome);

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
    public void handleRequest_ResourceExistsWithTagsForFaceSearch() {

        final ResourceModel expectedModel = TestUtils.getExpectedModelForFaceSearchReadHandler();
        expectedModel.setTags(TagHelper.convertToSet(TEST_TAGS));

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(describeResponseForFaceSearch);

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
        stubAndThrowExceptionForDescribeStreamProcessor(
                AccessDeniedException.builder().message("RekognitionException").build(),
                new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_NotExist() {
        stubAndThrowExceptionForDescribeStreamProcessor(
                ResourceNotFoundException.builder().message("RekognitionException").build(),
                new CfnNotFoundException("StreamProcessor", TEST_STREAM_PROCESSOR_NAME)
        );
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForDescribeStreamProcessor(
                InternalServerErrorException.builder().message("RekognitionException").build(),
                new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameter() {
        stubAndThrowExceptionForDescribeStreamProcessor(
                InvalidParameterException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ThroughputExceeded() {
        stubAndThrowExceptionForDescribeStreamProcessor(
                ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ResourceThrottled() {
        stubAndThrowExceptionForDescribeStreamProcessor(
                ThrottlingException.builder().message("RekognitionException").build(),
                new CfnThrottlingException("CfnException")
        );
    }

    @Test
    public void handleRequest_TagAccessDenied() {
        stubAndThrowExceptionForListTagsForResource(
                AccessDeniedException.builder().message("RekognitionException").build(),
                new CfnAccessDeniedException("CfnException"),
                describeResponseForConnectedHome
        );
    }

    @Test
    public void handleRequest_TagNotExist() {
        stubAndThrowExceptionForListTagsForResource(
                ResourceNotFoundException.builder().message("RekognitionException").build(),
                new CfnNotFoundException("Collection", TEST_STREAM_PROCESSOR_NAME),
                describeResponseForFaceSearch
        );
    }

    @Test
    public void handleRequest_TagInternalServerError() {
        stubAndThrowExceptionForListTagsForResource(
                InternalServerErrorException.builder().message("RekognitionException").build(),
                new CfnServiceInternalErrorException("CfnException"),
                describeResponseForConnectedHome
        );
    }

    @Test
    public void handleRequest_TagInvalidParameter() {
        stubAndThrowExceptionForListTagsForResource(
                InvalidParameterException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException"),
                describeResponseForFaceSearch
        );
    }

    @Test
    public void handleRequest_TagThroughputExceeded() {
        stubAndThrowExceptionForListTagsForResource(
                ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded"),
                describeResponseForConnectedHome
        );
    }

    @Test
    public void handleRequest_TagThrottled() {
        stubAndThrowExceptionForListTagsForResource(
                ThrottlingException.builder().message("RekognitionException").build(),
                new CfnThrottlingException("CfnException"),
                describeResponseForFaceSearch
        );
    }

    /**
     * Method to cover all exceptions in the "DescribeCollection" part of the Read chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForDescribeStreamProcessor(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        when(proxyClient
                .injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
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
    private void stubAndThrowExceptionForListTagsForResource(final RekognitionException rekEx,
                                                             final BaseHandlerException cfnEx,
                                                             final DescribeStreamProcessorResponse desiredResponse) {
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(desiredResponse);
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
