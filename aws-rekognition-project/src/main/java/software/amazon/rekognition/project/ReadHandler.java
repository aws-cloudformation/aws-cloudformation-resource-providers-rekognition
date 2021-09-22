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

import java.util.Optional;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        DescribeProjectsResponse describeProjectsResponse = null;
        DescribeProjectsRequest describeProjectsRequest = null;
        Optional<ProjectDescription> projectToRead;
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

            projectToRead = Utils.findProjectByNameInResponse(describeProjectsResponse, model.getProjectName());
            nextToken = describeProjectsResponse.nextToken();
        } while (projectToRead.isPresent() == false && nextToken != null);

        if (projectToRead.isPresent() == false)
        {
            final ResourceNotFoundException resourceNotFoundException =
                    new ResourceNotFoundException(ResourceModel.TYPE_NAME, model.getProjectName());

            logger.log(resourceNotFoundException.getMessage());
            throw resourceNotFoundException;
        }

        ResourceModel responseResourceModel = ResourceModel.builder()
                .projectName(Utils.getProjectNameFromArn(projectToRead.get().projectArn()))
                .arn(projectToRead.get().projectArn())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(responseResourceModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
