package software.amazon.rekognition.collection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.TagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.TagResourceResponse;
import software.amazon.awssdk.services.rekognition.model.UntagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.UntagResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;
import java.util.Set;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        if (TagHelper.shouldUpdateTags(request.getDesiredResourceState(), request)) {
            return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> checkIfResourceExists(proxy, proxyClient, progress.getResourceModel(), request, callbackContext, logger))
                .then(progress -> untagResource(proxy, proxyClient, progress.getResourceModel(), request, callbackContext, logger))
                .then(progress -> tagResource(proxy, proxyClient, progress.getResourceModel(), request, callbackContext, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
        }

        return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
    }

    /**
     * Initial check to ensure resource exists
     * <p>
     * Calls the DescribeCollection API.
     */
    private ProgressEvent<ResourceModel, CallbackContext> checkIfResourceExists(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<RekognitionClient> serviceClient,
        final ResourceModel resourceModel,
        final ResourceHandlerRequest<ResourceModel> handlerRequest,
        final CallbackContext callbackContext, final Logger logger
    ) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Checking if resource: %s with AccountId: %s",
            resourceModel.getCollectionId(), handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-Rekognition-Collection::DescribeCollection", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall(this::describeCollection)
            .done(response -> ProgressEvent.progress(Translator.translateFromDescribeResponse(response, resourceModel), callbackContext));
    }

    private DescribeCollectionResponse describeCollection(
        final DescribeCollectionRequest request,
        final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            DescribeCollectionResponse response = client
                .injectCredentialsAndInvokeV2(request, client.client()::describeCollection);
            logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
            return response;
        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }

    /**
     * untagResource during update
     * <p>
     * Calls the UntagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext> untagResource(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<RekognitionClient> serviceClient,
        final ResourceModel resourceModel,
        final ResourceHandlerRequest<ResourceModel> handlerRequest,
        final CallbackContext callbackContext,
        final Logger logger
    ) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for %s resource: %s with AccountId: %s",
            ResourceModel.TYPE_NAME, resourceModel.getArn(), handlerRequest.getAwsAccountId()));

        final Map<String, String> previousTags = TagHelper.getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(resourceModel, handlerRequest);
        final Set<String> tagsToRemove = TagHelper.generateTagsToRemove(previousTags, desiredTags);
        if (tagsToRemove.isEmpty()) {
            logger.log(String.format("No tags to remove for %s.", resourceModel.getArn()));
            return ProgressEvent.progress(resourceModel, callbackContext);
        }

        return proxy.initiate("AWS-Rekognition-Collection::UnTagResource", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model -> Translator.untagResourceRequest(model, tagsToRemove))
            .makeServiceCall(this::unTagResource)
            .progress();
    }

    /**
     * tagResource during update
     * <p>
     * Calls the TagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext> tagResource(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<RekognitionClient> serviceClient,
        final ResourceModel resourceModel,
        final ResourceHandlerRequest<ResourceModel> handlerRequest,
        final CallbackContext callbackContext,
        final Logger logger
    ) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for %s resource: %s with AccountId: %s",
            ResourceModel.TYPE_NAME, resourceModel.getArn(), handlerRequest.getAwsAccountId()));

        final Map<String, String> previousTags = TagHelper.getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(resourceModel, handlerRequest);
        final Map<String, String> tagsToAdd = TagHelper.generateTagsToAdd(previousTags, desiredTags);
        if (tagsToAdd.isEmpty()) {
            logger.log(String.format("No tags to add for %s.", resourceModel.getArn()));
            return ProgressEvent.progress(resourceModel, callbackContext);
        }

        return proxy.initiate("AWS-Rekognition-Collection::TagResource", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model -> Translator.tagResourceRequest(model, tagsToAdd))
            .makeServiceCall(this::tagResource)
            .progress();
    }

    private UntagResourceResponse unTagResource(
        final UntagResourceRequest request,
        final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            UntagResourceResponse response = client.injectCredentialsAndInvokeV2(request, client.client()::untagResource);
            logger.log(String.format("%s successfully removed Tags.", ResourceModel.TYPE_NAME));
            return response;

        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }

    private TagResourceResponse tagResource(
        final TagResourceRequest request,
        final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            TagResourceResponse response = client.injectCredentialsAndInvokeV2(request, client.client()::tagResource);
            logger.log(String.format("%s successfully added Tags.", ResourceModel.TYPE_NAME));
            return response;

        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }
}
