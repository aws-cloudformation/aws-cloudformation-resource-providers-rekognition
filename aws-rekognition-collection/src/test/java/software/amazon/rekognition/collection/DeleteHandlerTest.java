package software.amazon.rekognition.collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.ServiceQuotaExceededException;
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
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    RekognitionClient sdkClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private DeleteHandler handler;

    private ResourceHandlerRequest<ResourceModel> request;

    private DeleteCollectionRequest deleteCollectionRequest;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new DeleteHandler();

        ResourceModel initial = ResourceModel.builder()
            .collectionId(TEST_COLLECTION_NAME)
            .build();
        deleteCollectionRequest = Translator.translateToDeleteRequest(initial);
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(initial)
            .build();
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_DeleteCollection() {
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                deleteCollectionRequest,
                proxyClient.client()::deleteCollection
            )).thenReturn(
            DeleteCollectionResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForDeleteCollection(
            InternalServerErrorException.builder().build(),
            new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_CollectionDoesNotExist() {
        stubAndThrowExceptionForDeleteCollection(
            ResourceNotFoundException.builder().build(),
            new CfnNotFoundException(ResourceModel.TYPE_NAME, TEST_COLLECTION_NAME)
        );
    }

    @Test
    public void handleRequest_DeleteCollectionDenied() {
        stubAndThrowExceptionForDeleteCollection(
            AccessDeniedException.builder().message("RekognitionException").build(),
            new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForDeleteCollection(
            InvalidParameterException.builder().message("RekognitionException").build(),
            new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForDeleteCollection(
            ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException() {
        stubAndThrowExceptionForDeleteCollection(
            ServiceQuotaExceededException.builder().message("RekognitionException").build(),
            new CfnServiceLimitExceededException(TEST_COLLECTION_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForDeleteCollection(
            ThrottlingException.builder().message("RekognitionException").build(),
            new CfnThrottlingException("CfnException")
        );
    }

    /**
     * Method to cover all exceptions in the "DeleteCollection" part of delete chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForDeleteCollection(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        when(
            proxyClient.injectCredentialsAndInvokeV2(
                deleteCollectionRequest,
                proxyClient.client()::deleteCollection
            )).thenThrow(rekEx);

        assertThrows(
            cfnEx.getClass(),
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
