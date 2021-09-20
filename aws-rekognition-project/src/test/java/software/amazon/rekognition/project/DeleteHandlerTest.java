package software.amazon.rekognition.project;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ProjectDescription;
import software.amazon.awssdk.services.rekognition.model.DeleteProjectResponse;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsResponse;

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

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    private DeleteHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private RekognitionClient rekognitionClient;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = mock(AmazonWebServicesClientProxy.class);
        rekognitionClient = mock(RekognitionClient.class);
        logger = mock(Logger.class);
    }

    @Test
    public void test_DeleteHandler_ShouldSucceed_WhenProjectExists() {
        // Arrange
        final String projectName = "projectName";
        final DeleteProjectResponse deleteProjectResponse = DeleteProjectResponse.builder().build();

        List<ProjectDescription> projects = new ArrayList<>();
        projects.add(ProjectDescription.builder()
            .projectArn("arn:aws:rekognition:us-east-1:000000000000:project/" + projectName + "/1111111111111")
            .build());
        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
            .projectDescriptions(projects)
            .build();

        try (MockedStatic<RekognitionClient> mocked = mockStatic(RekognitionClient.class)) {
            mocked.when(RekognitionClient::create).thenReturn(rekognitionClient);

            doReturn(describeProjectsResponse).doReturn(deleteProjectResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceModel model = ResourceModel.builder().projectName(projectName).build();

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

            // Act
            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        }
    }

    @Test
    public void test_DeleteHandler_ShouldFail_WhenProjectDoesNotExist_And_NoProjectInAccount() {
        // Arrange
        final String projectName = "projectName";
        final DeleteProjectResponse deleteProjectResponse = DeleteProjectResponse.builder().build();

        List<ProjectDescription> projects = new ArrayList<>();
        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
            .projectDescriptions(projects)
            .build();

        try (MockedStatic<RekognitionClient> mocked = mockStatic(RekognitionClient.class)) {
            mocked.when(RekognitionClient::create).thenReturn(rekognitionClient);

            doReturn(describeProjectsResponse).doThrow(ResourceNotFoundException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceModel model = ResourceModel.builder().projectName(projectName).build();

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
        }
    }

    @Test
    public void test_DeleteHandler_ShouldFail_WhenProjectDoesNotExist_And_ThereExistsProjectsInAccount() {
        // Arrange
        final String projectName = "projectName";
        String arn1 = "arn:aws:rekognition:us-east-1:000000000000:project/Project1/1111111111111";
        String arn2 = "arn:aws:rekognition:us-east-1:000000000000:project/Project2/2222222222222";

        final DeleteProjectResponse deleteProjectResponse = DeleteProjectResponse.builder().build();

        List<ProjectDescription> projects = new ArrayList<>();
        projects.add(ProjectDescription.builder()
                .projectArn(arn1)
                .build());
        projects.add(ProjectDescription.builder()
                .projectArn(arn2)
                .build());
        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
                .projectDescriptions(projects)
                .build();

        try (MockedStatic<RekognitionClient> mocked = mockStatic(RekognitionClient.class)) {
            mocked.when(RekognitionClient::create).thenReturn(rekognitionClient);

            doReturn(describeProjectsResponse)
                .doThrow(ResourceNotFoundException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceModel model = ResourceModel.builder().projectName(projectName).build();

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
        }
    }
}
