package software.amazon.rekognition.project;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ProjectDescription;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsResponse;
import software.amazon.awssdk.services.rekognition.model.DeleteProjectRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteProjectResponse;

import java.util.List;
import java.util.Arrays;
import java.util.Optional;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        DescribeProjectsResponse describeProjectsResponse = null;
        DescribeProjectsRequest describeProjectsRequest = null;
        Optional<ProjectDescription> projectToDelete;
        String nextToken = null;
        
        final ResourceModel model = request.getDesiredResourceState();
        RekognitionClient rekognitionClient = RekognitionClient.create();

        do {
            describeProjectsRequest = DescribeProjectsRequest.builder()
                    .nextToken(nextToken)
                    .build();

            describeProjectsResponse = proxy.injectCredentialsAndInvokeV2(
                    describeProjectsRequest,
                    rekognitionClient::describeProjects);

            projectToDelete = Utils.findProjectByNameInResponse(describeProjectsResponse, model.getProjectName());
            nextToken = describeProjectsResponse.nextToken();
        } while (projectToDelete.isPresent() == false && nextToken != null);

        if (projectToDelete.isPresent() == false)
        {
            final ResourceNotFoundException resourceNotFoundException =
                    new ResourceNotFoundException(ResourceModel.TYPE_NAME, model.getProjectName());

            logger.log(resourceNotFoundException.getMessage());
            throw resourceNotFoundException;
        }
        
        DeleteProjectRequest deleteProjectRequest = DeleteProjectRequest.builder()
                .projectArn(projectToDelete.get().projectArn())
                .build();

        DeleteProjectResponse deleteProjectResponse = proxy.injectCredentialsAndInvokeV2(
                deleteProjectRequest,
                rekognitionClient::deleteProject);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
