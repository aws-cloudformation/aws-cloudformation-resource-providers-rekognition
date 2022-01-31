package software.amazon.rekognition.collection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


/**
 * Create Cloudformation handler for Rekognition::Collection Resource.
 * Flow -
 *  1. Call CreateCollection.
 *  2. Call ReadHandler to return created collection (DescribeCollection + ListTagsForResource)
 */
public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    private static final int CREATE_STABILIZATION_DELAY = 65;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger
    ) {

        this.logger = logger;

        logger.log(String.format("Cfn Request: %s", request));

        if (callbackContext.isCreated()) {
            return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
        }

        callbackContext.setCreated(true);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-Rekognition-Collection::CreateCollection", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToCreateRequest(model, request))
                    .makeServiceCall(this::createCollection)
                    .progress(CREATE_STABILIZATION_DELAY)
            );
    }

    private CreateCollectionResponse createCollection(
        final CreateCollectionRequest request,
        final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            CreateCollectionResponse response = client.injectCredentialsAndInvokeV2(request, client.client()::createCollection);
            logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
            return response;

        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }
}
