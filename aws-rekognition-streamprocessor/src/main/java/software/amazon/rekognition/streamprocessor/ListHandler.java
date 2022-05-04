package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsRequest;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * List Cloudformation handler for AWS::Rekognition::StreamProcessor Resource.
 * Flow -
 *  Call ListStreamProcessors with the token as the input if provided to receive paginated list results.
 */
public class ListHandler extends BaseHandlerStd {
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Cfn Request: %s", request));

        return proxy.initiate(LIST_CALL_GRAPH_NAME, proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(r -> Translator.translateToListRequest(request.getNextToken()))
                .makeServiceCall(this::listStreamProcessors)
                .done(response ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(Translator.translateFromListResponse(response))
                                .status(OperationStatus.SUCCESS)
                                .nextToken(response.nextToken())
                                .build());
    }

    private ListStreamProcessorsResponse listStreamProcessors(
            final ListStreamProcessorsRequest request,
            final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            ListStreamProcessorsResponse response = client.injectCredentialsAndInvokeV2(request,
                    client.client()::listStreamProcessors);
            logger.log(String.format("%s successfully listed.", ResourceModel.TYPE_NAME));
            return response;
        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }
}
