package software.amazon.rekognition.collection;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.InvalidPaginationTokenException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ListCollectionsRequest;
import software.amazon.awssdk.services.rekognition.model.ListCollectionsResponse;
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
import java.util.List;
import java.util.stream.Collectors;

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
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    RekognitionClient sdkClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private ListHandler handler;

    private ResourceModel inputModel;

    private List<String> collectionIds;

    private final String NEXT_TOKEN_1 = "nextToken1";
    private final String NEXT_TOKEN_2 = "nextToken2";

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new ListHandler();
        inputModel = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();
        collectionIds = Lists.newArrayList("Id1", "Id2", "Id3");
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_ListCollectionsWithoutNextToken() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(inputModel)
            .build();
        List<ResourceModel> models = collectionIds.stream()
            .map(id -> ResourceModel.builder().collectionId(id).build())
            .collect(Collectors.toList());
        ListCollectionsRequest listCollectionsRequest = Translator.translateToListRequest(null);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listCollectionsRequest,
                proxyClient.client()::listCollections
            )).thenReturn(
                ListCollectionsResponse.builder()
                    .collectionIds(collectionIds)
                    .faceModelVersions("5.0", "5.0", "4.0")
                    .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(models);
        assertThat(response.getNextToken()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListCollectionsWithNextTokenReturned() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(inputModel)
            .build();

        List<ResourceModel> models = collectionIds.stream()
            .map(id -> ResourceModel.builder().collectionId(id).build())
            .collect(Collectors.toList());
        ListCollectionsRequest listCollectionsRequest = Translator.translateToListRequest(null);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listCollectionsRequest,
                proxyClient.client()::listCollections
            )).thenReturn(
                ListCollectionsResponse.builder()
                    .collectionIds(collectionIds)
                    .faceModelVersions("5.0", "5.0", "4.0")
                    .nextToken(NEXT_TOKEN_1)
                    .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(models);
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN_1);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListCollectionsWithNextTokenInInput() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(inputModel)
            .nextToken(NEXT_TOKEN_1)
            .build();

        List<ResourceModel> models = collectionIds.stream()
            .map(id -> ResourceModel.builder().collectionId(id).build())
            .collect(Collectors.toList());
        ListCollectionsRequest listCollectionsRequest = Translator.translateToListRequest(NEXT_TOKEN_1);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listCollectionsRequest,
                proxyClient.client()::listCollections
            )).thenReturn(
                ListCollectionsResponse.builder()
                    .collectionIds(collectionIds)
                    .faceModelVersions("5.0", "5.0", "4.0")
                    .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(models);
        assertThat(response.getNextToken()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListCollectionsWithNextTokenReturnedAndInInput() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(inputModel)
            .nextToken(NEXT_TOKEN_1)
            .build();

        List<ResourceModel> models = collectionIds.stream()
            .map(id -> ResourceModel.builder().collectionId(id).build())
            .collect(Collectors.toList());
        ListCollectionsRequest listCollectionsRequest = Translator.translateToListRequest(NEXT_TOKEN_1);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listCollectionsRequest,
                proxyClient.client()::listCollections
            )).thenReturn(
                ListCollectionsResponse.builder()
                    .collectionIds(collectionIds)
                    .faceModelVersions("5.0", "5.0", "4.0")
                    .nextToken(NEXT_TOKEN_2)
                    .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(models);
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN_2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InvalidPaginatedToken() {
        stubAndThrowExceptionForListCollections(
            InvalidPaginationTokenException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ListCollectionsDenied() {
        stubAndThrowExceptionForListCollections(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForListCollections(
            InternalServerErrorException.builder().build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForListCollections(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForListCollections(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_CollectionDoesNotExist() {
        stubAndThrowExceptionForListCollections(
            ResourceNotFoundException.builder().build(),
            new CfnNotFoundException(ResourceModel.TYPE_NAME, TEST_COLLECTION_NAME)
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForListCollections(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    /**
     * Method to cover all exceptions in the "ListCollection" part of the List chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForListCollections(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(inputModel)
            .build();
        ListCollectionsRequest listCollectionsRequest = Translator.translateToListRequest(null);

        when(
            proxyClient.injectCredentialsAndInvokeV2(
                listCollectionsRequest,
                proxyClient.client()::listCollections
            )).thenThrow(rekEx);
        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
