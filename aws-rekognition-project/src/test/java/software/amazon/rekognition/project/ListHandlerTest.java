package software.amazon.rekognition.project;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ProjectDescription;
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
import java.util.Arrays;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    private ListHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private RekognitionClient rekognitionClient;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = mock(AmazonWebServicesClientProxy.class);
        rekognitionClient = mock(RekognitionClient.class);
        logger = mock(Logger.class);
    }

    @Test
    public void test_ListHandler_ShouldNotFaild_WhenNoProjectAvailable() {
        // Arrange
        List<ProjectDescription> projects = new ArrayList<>();
        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
            .projectDescriptions(projects)
            .build();

        try (MockedStatic<RekognitionClient> mocked = mockStatic(RekognitionClient.class)) {
            mocked.when(RekognitionClient::create).thenReturn(rekognitionClient);

            doReturn(describeProjectsResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

            // Act
            final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isNull();
            assertThat(response.getResourceModels().size()).isEqualTo(0);
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        }
    }

    @Test
    public void test_ListHandler_ShouldSucceed_WhenMoreThanOneProjectsAvailable() {
        // Arrange
        List<ProjectDescription> projects = new ArrayList<>();
        String arn1 = "arn:aws:rekognition:us-east-1:000000000000:project/Project1/1111111111111";
        String arn2 = "arn:aws:rekognition:us-east-1:000000000000:project/Project2/2222222222222";

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
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

            final ResourceModel model1 = ResourceModel.builder()
                .projectName("Project1")
                .arn(arn1)
                .build();

            final ResourceModel model2 = ResourceModel.builder()
                .projectName("Project2")
                .arn(arn2)
                .build();

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

            // Act
            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isNull();
            assertThat(response.getResourceModels()).containsAll(Arrays.asList(model1, model2));
            assertThat(response.getResourceModels().size()).isEqualTo(projects.size());
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        }
    }
}
