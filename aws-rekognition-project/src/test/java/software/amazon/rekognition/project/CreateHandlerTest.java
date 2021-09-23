package software.amazon.rekognition.project;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateProjectRequest;
import software.amazon.awssdk.services.rekognition.model.CreateProjectResponse;
import software.amazon.awssdk.services.rekognition.model.ResourceInUseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    private CreateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private RekognitionClient rekognitionClient;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = mock(AmazonWebServicesClientProxy.class);
        rekognitionClient = mock(RekognitionClient.class);
        logger = mock(Logger.class);
    }

    @Test
    public void test_CreateHandler_ShouldSucceed_SimpleSuccess() {
        // Arrange
        final String projectName = "projectName";
        final String projectArn = "arn:aws:rekognition:us-east-1:111111111111:project/" + projectName;

        final CreateProjectResponse createProjectResponse = CreateProjectResponse.builder()
            .projectArn(projectArn)
            .build();

        try (MockedStatic<RekognitionClient> mocked = mockStatic(RekognitionClient.class)) {
            mocked.when(RekognitionClient::create).thenReturn(rekognitionClient);

            doReturn(createProjectResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceModel expectedModel = ResourceModel.builder()
                .projectName(projectName)
                .arn(projectArn)
                .build();

            ResourceModel requestModel = ResourceModel.builder()
                .projectName(projectName)
                .build();

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

            // Act
            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualToComparingFieldByField(expectedModel);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        }
    }

    @Test
    public void test_CreateHandler_ShouldFail_WhenProjectWithSameNameExists() {
        // Arrange
        final String projectName = "projectName";
        final ResourceInUseException conflictException = ResourceInUseException.builder().build();

        try (MockedStatic<RekognitionClient> mocked = mockStatic(RekognitionClient.class)) {
            mocked.when(RekognitionClient::create).thenReturn(rekognitionClient);

            doThrow(conflictException)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceModel requestModel = ResourceModel.builder()
                .projectName(projectName)
                .build();

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

            // Act & Assert
            assertThrows(ResourceAlreadyExistsException.class,
                () -> handler.handleRequest(proxy, request, null, logger));
        }
    }

    @Test
    public void test_CreateHandler_ShouldFail_CannotSetReadOnlyPropertyARN() {
        // Arrange
        final String projectName = "projectName";
        final String projectArn = "arn:aws:rekognition:us-east-1:111111111111:project/projectName";

        final ResourceModel requestModel = ResourceModel.builder()
            .projectName(projectName)
            .arn(projectArn)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestModel)
            .build();

        // Act & Assert
        assertThrows(CfnInvalidRequestException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }
}
