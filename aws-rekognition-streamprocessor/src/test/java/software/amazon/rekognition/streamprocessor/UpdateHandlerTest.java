package software.amazon.rekognition.streamprocessor;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
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
import software.amazon.awssdk.services.rekognition.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.rekognition.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.rekognition.model.TagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.TagResourceResponse;
import software.amazon.awssdk.services.rekognition.model.ThrottlingException;
import software.amazon.awssdk.services.rekognition.model.UntagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.UntagResourceResponse;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static software.amazon.rekognition.streamprocessor.TestUtils.MOCK_CREDENTIALS;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_NAME;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_TAGS;
import static software.amazon.rekognition.streamprocessor.TestUtils.logger;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock private AmazonWebServicesClientProxy proxy;
    @Mock private ProxyClient<RekognitionClient> proxyClient;
    @Mock RekognitionClient sdkClient;
    private UpdateHandler handler;
    private ResourceModel resourceModel;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        this.proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        this.sdkClient = mock(RekognitionClient.class);
        this.proxyClient = MOCK_PROXY(proxy, sdkClient);
        this.handler = new UpdateHandler();
        this.resourceModel = TestUtils.getInputModelForConnectedHomeUpdateHandler();
    }

    @AfterEach
    public void tear_down() {
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_updateStreamProcessorWithTagsForConnectedHome() {
        final Map<String, String> expectedTags = ImmutableMap.of(
                "TEST_TAG_1", "TEST_VALUE_3",
                "TEST_TAG_2", "TEST_VALUE_4"
        );

        // Desired streamprocessor with new tags
        ResourceModel desiredModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();

        // Previous streamprocessor with different tags
        final ResourceModel previousModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(TEST_TAGS))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                .resourceArn(desiredModel.getArn())
                .tags(TagHelper.convertToMap(desiredModel.getTags()))
                .build();

        final DescribeStreamProcessorRequest expectedDescribeStreamProcessorRequest = Translator.translateToDescribeRequest(previousModel);
        final DescribeStreamProcessorResponse expectedDescribeStreamProcessorResponse = TestUtils.getConnectedHomeDescribeResponseForUpdateHandler();
        final ResourceModel transformedResourceModel =  Translator.translateFromDescribeResponse(expectedDescribeStreamProcessorResponse, previousModel);
        final ListTagsForResourceRequest expectedListTagsForResourceRequest = Translator.translateToListTagsRequest(transformedResourceModel);
        // Expected response should have new tags
        final ResourceModel expectedResponse = TestUtils.getExpectedModelForConnectedHomeUpdateHandler().toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        expectedDescribeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(expectedDescribeStreamProcessorResponse);
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        tagResourceRequest,
                        proxyClient.client()::tagResource
                )).thenReturn(TagResourceResponse.builder().build());

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        expectedListTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .tags(TagHelper.convertToMap(expectedResponse.getTags()))
                        .build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> updateResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(updateResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(updateResponse.getResourceModels()).isNull();
        assertThat(updateResponse.getMessage()).isNull();
        assertThat(updateResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_updateStreamProcessorWithUntagAndTagForConnectedHome() {
        final Map<String, String> expectedTags = ImmutableMap.of(
                "TEST_TAG_3", "TEST_VALUE_3",
                "TEST_TAG_4", "TEST_VALUE_4"
        );

        // Desired streamprocessor with new tags
        ResourceModel requestModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();

        // Previous streamprocessor with different tags
        final ResourceModel previousModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(TEST_TAGS))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(requestModel)
                .build();

        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                .resourceArn(requestModel.getArn())
                .tags(TagHelper.convertToMap(requestModel.getTags()))
                .build();

        // Expected response should have new tags
        final ResourceModel expectedResponse = TestUtils.getExpectedModelForConnectedHomeUpdateHandler().toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();
        final DescribeStreamProcessorRequest expectedDescribeStreamProcessorRequest = Translator.translateToDescribeRequest(requestModel);
        final DescribeStreamProcessorResponse expectedDescribeStreamProcessorResponse = TestUtils.getConnectedHomeDescribeResponseForUpdateHandler();
        final ResourceModel transformedResourceModel =  Translator.translateFromDescribeResponse(expectedDescribeStreamProcessorResponse, previousModel);
        final ListTagsForResourceRequest expectedListTagsForResourceRequest = Translator.translateToListTagsRequest(transformedResourceModel);
        final DescribeStreamProcessorRequest expectedTransformedDescribeStreamProcessorRequest = Translator.translateToDescribeRequest(transformedResourceModel);
        final UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                .resourceArn(requestModel.getArn())
                .tagKeys(
                        TagHelper
                                .generateTagsToRemove(
                                        TagHelper.convertToMap(previousModel.getTags()),
                                        TagHelper.convertToMap(requestModel.getTags())))
                .build();
        this.stubDefaultDescribeRequest(expectedDescribeStreamProcessorRequest,
                expectedDescribeStreamProcessorResponse);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        untagResourceRequest,
                        proxyClient.client()::untagResource
                )).thenReturn(UntagResourceResponse.builder().build());

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        expectedTransformedDescribeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(expectedDescribeStreamProcessorResponse);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        tagResourceRequest,
                        proxyClient.client()::tagResource
                )).thenReturn(TagResourceResponse.builder().build());
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        expectedListTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .tags(TagHelper.convertToMap(expectedResponse.getTags()))
                        .build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> updateResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(updateResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(updateResponse.getResourceModels()).isNull();
        assertThat(updateResponse.getMessage()).isNull();
        assertThat(updateResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_updateStreamProcessorWithUntagForConnectedHome() {
        final Map<String, String> expectedTags = Collections.emptyMap();

        // Desired streamprocessor with new tags
        ResourceModel requestModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();

        // Previous streamprocessor with different tags
        final ResourceModel previousModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(TEST_TAGS))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(requestModel)
                .build();

        // Expected response should have new tags
        final ResourceModel expectedResponse = TestUtils.getExpectedModelForConnectedHomeUpdateHandler().toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();
        final DescribeStreamProcessorRequest expectedDescribeStreamProcessorRequest = Translator.translateToDescribeRequest(requestModel);
        final DescribeStreamProcessorResponse expectedDescribeStreamProcessorResponse = TestUtils.getConnectedHomeDescribeResponseForUpdateHandler();
        final ResourceModel transformedResourceModel =  Translator.translateFromDescribeResponse(expectedDescribeStreamProcessorResponse, previousModel);
        final ListTagsForResourceRequest expectedListTagsForResourceRequest = Translator.translateToListTagsRequest(transformedResourceModel);
        this.stubDefaultDescribeRequest(expectedDescribeStreamProcessorRequest,
                expectedDescribeStreamProcessorResponse);
        UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                .resourceArn(requestModel.getArn())
                .tagKeys(
                        TagHelper
                                .generateTagsToRemove(
                                        TagHelper.convertToMap(previousModel.getTags()),
                                        TagHelper.convertToMap(requestModel.getTags())))
                .build();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        untagResourceRequest,
                        proxyClient.client()::untagResource
                )).thenReturn(UntagResourceResponse.builder().build());

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        expectedListTagsForResourceRequest,
                        proxyClient.client()::listTagsForResource
                )).thenReturn(
                ListTagsForResourceResponse.builder()
                        .tags(TagHelper.convertToMap(expectedResponse.getTags()))
                        .build()
        );
        final ProgressEvent<ResourceModel, CallbackContext> updateResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(updateResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(updateResponse.getResourceModels()).isNull();
        assertThat(updateResponse.getMessage()).isNull();
        assertThat(updateResponse.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                InternalServerErrorException.builder().build(),
                new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_StreamprocessorAlreadyExists() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                ResourceAlreadyExistsException.builder().build(),
                new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, TEST_STREAM_PROCESSOR_NAME)
        );
    }

    @Test
    public void handleRequest_StreamprocessorUpdateDenied() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                AccessDeniedException.builder().message("RekognitionException").build(),
                new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                InvalidParameterException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                ServiceQuotaExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForUpdateStreamProcessor(
                ThrottlingException.builder().message("RekognitionException").build(),
                new CfnThrottlingException("CfnException")
        );
    }

    private void stubAndThrowExceptionForUpdateStreamProcessor(final RekognitionException rekEx,
                                                               final BaseHandlerException cfnEx) {

        final Map<String, String> expectedTags = ImmutableMap.of(
                "TEST_TAG_1", "TEST_VALUE_3",
                "TEST_TAG_2", "TEST_VALUE_4"
        );

        ResourceModel desiredModel = resourceModel.toBuilder()
                .tags(TagHelper.convertToSet(expectedTags))
                .build();

        ResourceHandlerRequest<ResourceModel> resourceHandlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(resourceModel)
                .build();

        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                .resourceArn(desiredModel.getArn())
                .tags(TagHelper.convertToMap(desiredModel.getTags()))
                .build();

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        tagResourceRequest,
                        proxyClient.client()::tagResource
                )).thenThrow(rekEx);

        assertThrows(
                cfnEx.getClass(),
                () -> handler.handleRequest(proxy, resourceHandlerRequest, new CallbackContext(), proxyClient, logger)
        );
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    private void stubDefaultDescribeRequest(DescribeStreamProcessorRequest describeStreamProcessorRequest,
                                            DescribeStreamProcessorResponse describeStreamProcessorResponse) {
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        describeStreamProcessorRequest,
                        proxyClient.client()::describeStreamProcessor
                )).thenReturn(describeStreamProcessorResponse);
    }

}
