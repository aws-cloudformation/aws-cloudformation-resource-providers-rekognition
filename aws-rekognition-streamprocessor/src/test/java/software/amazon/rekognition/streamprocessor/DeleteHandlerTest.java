package software.amazon.rekognition.streamprocessor;

import java.time.Duration;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorResponse;
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
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_NAME;
import static software.amazon.rekognition.streamprocessor.TestUtils.logger;

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

    private DeleteStreamProcessorRequest deleteStreamProcessorRequest;

    @BeforeEach
    public void setup() {
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.getName());
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RekognitionClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new DeleteHandler();

        ResourceModel initial = ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .build();
        deleteStreamProcessorRequest = Translator.translateToDeleteRequest(initial);
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
    public void handleRequest_DeleteStreamProcessor() {
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        deleteStreamProcessorRequest,
                        proxyClient.client()::deleteStreamProcessor
                )).thenReturn(
                DeleteStreamProcessorResponse.builder().build());

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
        stubAndThrowExceptionForDeleteStreamProcessor(
                InternalServerErrorException.builder().build(),
                new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_StreamProcessorDoesNotExist() {
        stubAndThrowExceptionForDeleteStreamProcessor(
                ResourceNotFoundException.builder().build(),
                new CfnNotFoundException(ResourceModel.TYPE_NAME, TEST_STREAM_PROCESSOR_NAME)
        );
    }

    @Test
    public void handleRequest_DeleteStreamProcessorDenied() {
        stubAndThrowExceptionForDeleteStreamProcessor(
                AccessDeniedException.builder().message("RekognitionException").build(),
                new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForDeleteStreamProcessor(
                InvalidParameterException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForDeleteStreamProcessor(
                ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException() {
        stubAndThrowExceptionForDeleteStreamProcessor(
                ServiceQuotaExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForDeleteStreamProcessor(
                ThrottlingException.builder().message("RekognitionException").build(),
                new CfnThrottlingException("CfnException")
        );
    }

    /**
     * Method to cover all exceptions in the "DeleteStreamProcessor" part of delete chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForDeleteStreamProcessor(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        deleteStreamProcessorRequest,
                        proxyClient.client()::deleteStreamProcessor
                )).thenThrow(rekEx);

        assertThrows(
                cfnEx.getClass(),
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
