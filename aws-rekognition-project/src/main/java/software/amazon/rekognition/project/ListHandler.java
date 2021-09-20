package software.amazon.rekognition.project;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ProjectDescription;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsResponse;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        List<ResourceModel> models = new ArrayList<>();
        DescribeProjectsResponse describeProjectsResponse = null;
        DescribeProjectsRequest describeProjectsRequest = null;
        String nextToken = null;
        
        RekognitionClient rekognitionClient = RekognitionClient.create();

        do {
            describeProjectsRequest = DescribeProjectsRequest.builder()
                    .nextToken(nextToken)
                    .build();

            describeProjectsResponse = proxy.injectCredentialsAndInvokeV2(
                    describeProjectsRequest,
                    rekognitionClient::describeProjects);

            for (ProjectDescription p : describeProjectsResponse.projectDescriptions()) {
                models.add(ResourceModel.builder()
                        .arn(p.projectArn())
                        .projectName(Utils.getProjectNameFromArn(p.projectArn()))
                        .build());
            }
            nextToken = describeProjectsResponse.nextToken();
        } while (nextToken != null);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
