package software.amazon.rekognition.collection;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.rekognition.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.TagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.TagResourceResponse;
import software.amazon.awssdk.services.rekognition.model.ThrottlingException;
import software.amazon.awssdk.services.rekognition.model.UntagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.UntagResourceResponse;
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
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.rekognition.collection.TagHelper.convertToMap;
import static software.amazon.rekognition.collection.TagHelper.convertToSet;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    RekognitionClient sdkClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new UpdateHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_ResourceExistsButNoUpdateRequired() {

        // Desired collection has tags
        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // Previous collection has same tags
        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // Expected response should have no change
        final ResourceModel expectedResponse = desired.toBuilder()
            .arn(TEST_COLLECTION_ARN)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // pass initial check to ensure resource exists
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        stubReadHandler(expectedResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceExistsAndAddTagRequired() {

        // Desired collection has tags
        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // Previous collection has no tags
        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();

        // Expected response should have new tags
        final ResourceModel expectedResponse = desired.toBuilder()
            .arn(TEST_COLLECTION_ARN)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // pass initial check to ensure resource exists
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tags(TEST_TAGS)
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                tagResourceRequest,
                proxyClient.client()::tagResource
            )).thenReturn(TagResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        stubReadHandler(expectedResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    @Test
    public void handleRequest_ResourceExistsAndModifyTagRequired() {

        final Map<String, String> expectedTags = ImmutableMap.of(
            "TEST_TAG_1", "TEST_VALUE_3",
            "TEST_TAG_2", "TEST_VALUE_4"
        );
        // Desired collection has some tags
        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(expectedTags))
            .build();

        // Previous collection has different tags
        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // Expected response should have new tags
        final ResourceModel expectedResponse = desired.toBuilder()
            .arn(TEST_COLLECTION_ARN)
            .tags(convertToSet(expectedTags))
            .build();

        // pass initial check to ensure resource exists
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tags(expectedTags)
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                tagResourceRequest,
                proxyClient.client()::tagResource
            )).thenReturn(TagResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        stubReadHandler(expectedResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceExistsAndRemoveTagRequired() {

        // Desired collection should have no tags
        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(Collections.emptySet())
            .build();

        // Previous collection has tags
        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // Expected response should have no tags
        final ResourceModel expectedResponse = desired.toBuilder()
            .arn(TEST_COLLECTION_ARN)
            .tags(Collections.emptySet())
            .build();

        // pass initial check to ensure resource exists
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tagKeys(
                TagHelper
                    .generateTagsToRemove(
                        convertToMap(previous.getTags()),
                        convertToMap(desired.getTags())))
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                untagResourceRequest,
                proxyClient.client()::untagResource
            )).thenReturn(UntagResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        stubReadHandler(expectedResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceExistsAndBothAddRemoveTagRequired() {

        final Map<String, String> DESIRED_TAGS = ImmutableMap.of(
            "TEST_TAG_1", "TEST_VALUE_1",
            "TEST_TAG_3", "TEST_VALUE_3"
        );

        // Desired collection should have "TEST_TAG_1" and "TEST_TAG_3"
        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(DESIRED_TAGS))
            .build();

        // Previous collection should have "TEST_TAG_1" and "TEST_TAG_2"
        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        // Expected response should remove "TEST_TAG_2" and add "TEST_TAG_3"
        final ResourceModel expectedResponse = desired.toBuilder()
            .arn(TEST_COLLECTION_ARN)
            .tags(convertToSet(DESIRED_TAGS))
            .build();

        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        // Should only contain "TEST_TAG_2"
        UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tagKeys(
                TagHelper
                    .generateTagsToRemove(
                        convertToMap(previous.getTags()),
                        convertToMap(desired.getTags())))
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                untagResourceRequest,
                proxyClient.client()::untagResource
            )).thenReturn(UntagResourceResponse.builder().build());

        // Should only contain "TEST_TAG_3"
        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tags(
                TagHelper
                    .generateTagsToAdd(
                        convertToMap(previous.getTags()),
                        convertToMap(desired.getTags())))
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                tagResourceRequest,
                proxyClient.client()::tagResource
            )).thenReturn(TagResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        stubReadHandler(expectedResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResponse);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceDoesNotExist() {
        stubAndThrowExceptionForDescribeCollection(
            ResourceNotFoundException.builder().build(),
            new CfnNotFoundException(ResourceModel.TYPE_NAME, TEST_COLLECTION_ARN)
        );
    }

    @Test
    public void handleRequest_ResourceAccessDenied() {
        stubAndThrowExceptionForDescribeCollection(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
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
    public void handleRequest_RemoveTagAccessDenied() {
        stubAndThrowExceptionForUnTagResource(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_RemoveTagInternalServerError() {
        stubAndThrowExceptionForUnTagResource(
            InternalServerErrorException.builder().message("RekognitionException").build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_RemoveTagInvalidParameter() {
        stubAndThrowExceptionForUnTagResource(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_RemoveTagThroughputExceeded() {
        stubAndThrowExceptionForUnTagResource(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_RemoveTagResourceThrottled() {
        stubAndThrowExceptionForUnTagResource(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    @Test
    public void handleRequest_AddTagAccessDenied() {
        stubAndThrowExceptionForTagResource(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_AddTagInternalServerError() {
        stubAndThrowExceptionForTagResource(
            InternalServerErrorException.builder().message("RekognitionException").build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_AddTagInvalidParameter() {
        stubAndThrowExceptionForTagResource(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_AddTagThroughputExceeded() {
        stubAndThrowExceptionForTagResource(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_AddTagResourceThrottled() {
        stubAndThrowExceptionForTagResource(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    private void stubReadHandler(final ResourceModel expected) {
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(expected);
        final ListTagsForResourceRequest listTagsForResourceRequest = Translator.translateToListTagsRequest(expected);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listTagsForResourceRequest,
                proxyClient.client()::listTagsForResource
            )).thenReturn(
            ListTagsForResourceResponse.builder()
                .tags(convertToMap(expected.getTags()))
                .build()
        );
    }

    /**
     * Method to cover all exceptions in the "checkIfResourceExists" part of the update chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForDescribeCollection(
        final RekognitionException rekEx,
        final BaseHandlerException cfnEx
    ) {

        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(Collections.emptySet())
            .build();

        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenThrow(rekEx);
        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }

    /**
     * Method to cover all exceptions in the "unTagResource" part of the update chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForUnTagResource(
        final RekognitionException rekEx,
        final BaseHandlerException cfnEx
    ) {

        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(Collections.emptySet())
            .build();

        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        final ResourceModel expectedResponse = desired.toBuilder()
            .tags(Collections.emptySet())
            .build();

        // pass initial check to ensure resource exists
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tagKeys(
                TagHelper
                    .generateTagsToRemove(
                        convertToMap(previous.getTags()),
                        convertToMap(desired.getTags())))
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                untagResourceRequest,
                proxyClient.client()::untagResource
            )).thenThrow(rekEx);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }

    /**
     * Method to cover all exceptions in the "TagResource" part of the update chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForTagResource(
        final RekognitionException rekEx,
        final BaseHandlerException cfnEx
    ) {

        final ResourceModel desired = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .tags(convertToSet(TEST_TAGS))
            .build();

        final ResourceModel previous = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();

        final ResourceModel expectedResponse = desired.toBuilder()
            .tags(convertToSet(TEST_TAGS))
            .build();

        // pass initial check to ensure resource exists
        final DescribeCollectionRequest describeCollectionRequest = Translator.translateToReadRequest(previous);
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                describeCollectionRequest,
                proxyClient.client()::describeCollection
            )).thenReturn(DEFAULT_DESCRIBE_RESPONSE);

        TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
            .resourceArn(TEST_COLLECTION_ARN)
            .tags(TEST_TAGS)
            .build();
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                tagResourceRequest,
                proxyClient.client()::tagResource
            )).thenThrow(rekEx);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(previous)
            .desiredResourceState(desired)
            .build();

        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
