package software.amazon.rekognition.project;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateProjectRequest;
import software.amazon.awssdk.services.rekognition.model.CreateProjectResponse;
import software.amazon.awssdk.services.rekognition.model.ResourceInUseException;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // Make sure the user is not trying to assign values to readOnly properties (e.g. ARN)
        if (hasReadOnlyProperties(model)) {
            throw new CfnInvalidRequestException("Attempting to set a ReadOnly Property.");
        }

        RekognitionClient rekognitionClient = RekognitionClient.create();
        CreateProjectRequest createProjectRequest = CreateProjectRequest.builder()
                .projectName(model.getProjectName())
                .build();

        CreateProjectResponse createProjectResponse = null;

        try {
            createProjectResponse = proxy.injectCredentialsAndInvokeV2(
                    createProjectRequest,
                    rekognitionClient::createProject);
        } catch (ResourceInUseException e) {
            final ResourceAlreadyExistsException resourceAlreadyExistsException =
                    new ResourceAlreadyExistsException(ResourceModel.TYPE_NAME, model.getProjectName(), e);

            logger.log(resourceAlreadyExistsException.getMessage());
            throw resourceAlreadyExistsException;
        }

        logger.log(String.format("Project: %s successfully created.", model.getProjectName()));
        ResourceModel responseResourceModel = ResourceModel.builder()
                .projectName(model.getProjectName())
                .arn(createProjectResponse.projectArn())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(responseResourceModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    /**
     * This function checks that the model provided by CloudFormation does not contain any readOnly properties (i.e Arn).
     *
     * @param model the ResourceModel for the given CreateHandler invocation
     * @return a boolean indicating if the ResourceModel contains readOnly properties
     */
    private boolean hasReadOnlyProperties(final ResourceModel model) {
        return model.getArn() != null;
    }
}
