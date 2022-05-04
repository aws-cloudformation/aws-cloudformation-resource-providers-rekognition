package software.amazon.rekognition.streamprocessor;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.InvalidPaginationTokenException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsRequest;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsResponse;
import software.amazon.awssdk.services.rekognition.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.StreamProcessor;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static software.amazon.rekognition.streamprocessor.AbstractTestBase.MOCK_PROXY;
import static software.amazon.rekognition.streamprocessor.TestUtils.MOCK_CREDENTIALS;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_NAME;
import static software.amazon.rekognition.streamprocessor.TestUtils.TEST_STREAM_PROCESSOR_STATUS;
import static software.amazon.rekognition.streamprocessor.TestUtils.logger;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    RekognitionClient sdkClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RekognitionClient> proxyClient;

    private ListHandler handler;

    private ResourceModel inputModel;

    private List<String> streamProcessorNames;
    private List<StreamProcessor> streamProcessors;

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
                .name(TEST_STREAM_PROCESSOR_NAME)
                .build();
        streamProcessorNames = Lists.newArrayList("Name0", "Name1", "Name2");
        streamProcessors = streamProcessorNames.stream()
                .map(name -> StreamProcessor.builder().name(name).status(TEST_STREAM_PROCESSOR_STATUS).build())
                .collect(Collectors.toList());
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
        System.clearProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY);
    }

    @Test
    public void handleRequest_ListStreamProcessorsWithoutNextToken() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(inputModel)
                .build();
        List<ResourceModel> models = streamProcessorNames.stream()
                .map(name -> ResourceModel.builder().name(name).status(TEST_STREAM_PROCESSOR_STATUS).build())
                .collect(Collectors.toList());
        ListStreamProcessorsRequest listStreamProcessorsRequest = Translator.translateToListRequest(null);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listStreamProcessorsRequest,
                        proxyClient.client()::listStreamProcessors
                )).thenReturn(
                ListStreamProcessorsResponse.builder()
                        .streamProcessors(streamProcessors)
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
    public void handleRequest_ListStreamProcessorsWithNextTokenReturned() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(inputModel)
                .build();

        List<ResourceModel> models = streamProcessorNames.stream()
                .map(name -> ResourceModel.builder().name(name).status(TEST_STREAM_PROCESSOR_STATUS).build())
                .collect(Collectors.toList());
        ListStreamProcessorsRequest listStreamProcessorsRequest = Translator.translateToListRequest(null);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listStreamProcessorsRequest,
                        proxyClient.client()::listStreamProcessors
                )).thenReturn(
                ListStreamProcessorsResponse.builder()
                        .streamProcessors(streamProcessors)
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
    public void handleRequest_ListStreamProcessorsWithNextTokenInInput() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(inputModel)
                .nextToken(NEXT_TOKEN_1)
                .build();

        List<ResourceModel> models = streamProcessorNames.stream()
                .map(name -> ResourceModel.builder().name(name).status(TEST_STREAM_PROCESSOR_STATUS).build())
                .collect(Collectors.toList());
        ListStreamProcessorsRequest listStreamProcessorsRequest = Translator.translateToListRequest(NEXT_TOKEN_1);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listStreamProcessorsRequest,
                        proxyClient.client()::listStreamProcessors
                )).thenReturn(
                ListStreamProcessorsResponse.builder()
                        .streamProcessors(streamProcessors)
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
    public void handleRequest_ListStreamProcessorsWithNextTokenReturnedAndInInput() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(inputModel)
                .nextToken(NEXT_TOKEN_1)
                .build();

        List<ResourceModel> models = streamProcessorNames.stream()
                .map(name -> ResourceModel.builder().name(name).status(TEST_STREAM_PROCESSOR_STATUS).build())
                .collect(Collectors.toList());
        ListStreamProcessorsRequest listStreamProcessorsRequest = Translator.translateToListRequest(NEXT_TOKEN_1);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listStreamProcessorsRequest,
                        proxyClient.client()::listStreamProcessors
                )).thenReturn(
                ListStreamProcessorsResponse.builder()
                        .streamProcessors(streamProcessors)
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
        stubAndThrowExceptionForListStreamProcessors(
                InvalidPaginationTokenException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ListStreamProcessorsDenied() {
        stubAndThrowExceptionForListStreamProcessors(
                AccessDeniedException.builder().message("RekognitionException").build(),
                new CfnAccessDeniedException("CfnException")
        );
    }

    @Test
    public void handleRequest_InternalServerError() {
        stubAndThrowExceptionForListStreamProcessors(
                InternalServerErrorException.builder().build(),
                new CfnServiceInternalErrorException("CfnException")
        );
    }

    @Test
    public void handleRequest_InvalidParameters() {
        stubAndThrowExceptionForListStreamProcessors(
                InvalidParameterException.builder().message("RekognitionException").build(),
                new CfnInvalidRequestException("CfnException")
        );
    }

    @Test
    public void handleRequest_ProvisionedLimitExceeded() {
        stubAndThrowExceptionForListStreamProcessors(
                ProvisionedThroughputExceededException.builder().message("RekognitionException").build(),
                new CfnServiceLimitExceededException(TEST_STREAM_PROCESSOR_NAME, "exceeded")
        );
    }

    @Test
    public void handleRequest_StreamProcessorDoesNotExist() {
        stubAndThrowExceptionForListStreamProcessors(
                ResourceNotFoundException.builder().build(),
                new CfnNotFoundException(ResourceModel.TYPE_NAME, TEST_STREAM_PROCESSOR_NAME)
        );
    }

    @Test
    public void handleRequest_ThrottlingException() {
        stubAndThrowExceptionForListStreamProcessors(
                ThrottlingException.builder().message("RekognitionException").build(),
                new CfnThrottlingException("CfnException")
        );
    }

    /**
     * Method to cover all exceptions in the "ListStreamProcessors" part of the List chain
     *
     * @param rekEx Rekognition Service Exception
     * @param cfnEx Corresponding CfnException (Check {@link BaseHandlerStd#handlerError} for complete mapping)
     */
    private void stubAndThrowExceptionForListStreamProcessors(final RekognitionException rekEx, final BaseHandlerException cfnEx) {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(inputModel)
                .build();
        ListStreamProcessorsRequest listStreamProcessorsRequest = Translator.translateToListRequest(null);

        when(
                proxyClient.injectCredentialsAndInvokeV2(
                        listStreamProcessorsRequest,
                        proxyClient.client()::listStreamProcessors
                )).thenThrow(rekEx);
        assertThrows(
                cfnEx.getClass(),
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
