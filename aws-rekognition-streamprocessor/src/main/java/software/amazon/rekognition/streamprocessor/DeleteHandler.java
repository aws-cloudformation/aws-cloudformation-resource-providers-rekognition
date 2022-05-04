package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Delete Cloudformation handler for AWS::Rekognition::StreamProcessor Resource.
 * Flow -
 *  1. Call DeleteStreamProcessor.
 *  2. Use the default success handler to return the progress event.
 */
public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Cfn Request: %s", request));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate(DELETE_CALL_GRAPH_NAME, proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteStreamProcessor)
                                .progress()
                )
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteStreamProcessorResponse deleteStreamProcessor(
            final DeleteStreamProcessorRequest request,
            final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            DeleteStreamProcessorResponse response = client.injectCredentialsAndInvokeV2(request,
                    client.client()::deleteStreamProcessor);
            logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
            return response;

        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }
}
