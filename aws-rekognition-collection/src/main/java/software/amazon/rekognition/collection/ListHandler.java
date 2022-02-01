package software.amazon.rekognition.collection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ListCollectionsRequest;
import software.amazon.awssdk.services.rekognition.model.ListCollectionsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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

        return proxy.initiate("AWS-Rekognition-Collection::ListCollections", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(r -> Translator.translateToListRequest(request.getNextToken()))
            .makeServiceCall(this::listCollections)
            .done(response ->
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(response))
                    .status(OperationStatus.SUCCESS)
                    .nextToken(response.nextToken())
                    .build());
    }

    private ListCollectionsResponse listCollections(
        final ListCollectionsRequest request,
        final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            ListCollectionsResponse response = client.injectCredentialsAndInvokeV2(request, client.client()::listCollections);
            logger.log(String.format("%s successfully Listed.", ResourceModel.TYPE_NAME));
            return response;
        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }
}
