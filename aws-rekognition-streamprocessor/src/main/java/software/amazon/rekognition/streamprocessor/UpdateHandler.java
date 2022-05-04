package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
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

/**
 * Update Cloudformation handler for AWS::Rekognition::StreamProcessor Resource.
 * Flow -
 *  1. Remove tags if applicable (UntagResource)
 *  2. Add or update tags if applicable (TagResource)
 *  3. Call ReadHandler to return the updated stream processor (DescribeStreamProcessor + ListTagsForResource)
 */
public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger) {
        this.logger = logger;
        ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> updateResource(proxy, proxyClient, progress.getResourceModel(), request, callbackContext, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
        return progressEvent;
    }

    private ProgressEvent<software.amazon.rekognition.streamprocessor.ResourceModel, software.amazon.rekognition.streamprocessor.CallbackContext> updateResource(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<RekognitionClient> serviceClient,
            final software.amazon.rekognition.streamprocessor.ResourceModel resourceModel,
            final ResourceHandlerRequest<software.amazon.rekognition.streamprocessor.ResourceModel> handlerRequest,
            final software.amazon.rekognition.streamprocessor.CallbackContext callbackContext,
            final Logger logger
    ) {
        logger.log(String.format("Cfn Request: %s", handlerRequest));
        ProgressEvent progressEvent = ProgressEvent.progress(resourceModel, callbackContext);
        if(resourceModel.getArn() == null || resourceModel.getArn().length() == 0) {
            return progressEvent;
        }
        if (TagHelper.shouldUpdateTags(resourceModel, handlerRequest)) {
            progressEvent =  progressEvent
                    .then(progress -> untagResource(proxy, serviceClient, resourceModel, handlerRequest, callbackContext, logger))
                    .then(progress -> tagResource(proxy, serviceClient, resourceModel, handlerRequest, callbackContext, logger));
        }
        return progressEvent;
    }

    /**
     * untagResource during update
     * <p>
     * Calls the UntagResource API.
     */
    private ProgressEvent<software.amazon.rekognition.streamprocessor.ResourceModel, software.amazon.rekognition.streamprocessor.CallbackContext> untagResource(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<RekognitionClient> serviceClient,
            final software.amazon.rekognition.streamprocessor.ResourceModel resourceModel,
            final ResourceHandlerRequest<software.amazon.rekognition.streamprocessor.ResourceModel> handlerRequest,
            final software.amazon.rekognition.streamprocessor.CallbackContext callbackContext,
            final Logger logger
    ) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for %s with arn: %s",
                software.amazon.rekognition.streamprocessor.ResourceModel.TYPE_NAME, resourceModel.getArn()));
        final Map<String, String> previousTags = TagHelper.getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(resourceModel, handlerRequest);
        final Set<String> tagsToRemove = TagHelper.generateTagsToRemove(previousTags, desiredTags);
        if (tagsToRemove.isEmpty()) {
            logger.log(String.format("No tags to remove for %s.", resourceModel.getArn()));
            return ProgressEvent.progress(resourceModel, callbackContext);
        }
        logger.log(String.format("tags to be updated for %s.", resourceModel.getArn()));
        return proxy.initiate("AWS-Rekognition-StreamProcessor::UnTagResource", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model -> Translator.untagResourceRequest(model, tagsToRemove))
                .makeServiceCall(this::unTagResource)
                .progress();
    }

    /**
     * tagResource during update
     * <p>
     * Calls the TagResource API.
     */
    private ProgressEvent<software.amazon.rekognition.streamprocessor.ResourceModel, software.amazon.rekognition.streamprocessor.CallbackContext> tagResource(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<RekognitionClient> serviceClient,
            final software.amazon.rekognition.streamprocessor.ResourceModel resourceModel,
            final ResourceHandlerRequest<software.amazon.rekognition.streamprocessor.ResourceModel> handlerRequest,
            final software.amazon.rekognition.streamprocessor.CallbackContext callbackContext,
            final Logger logger
    ) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for %s resource: %s with AccountId: %s",
                software.amazon.rekognition.streamprocessor.ResourceModel.TYPE_NAME, resourceModel.getArn(), handlerRequest.getAwsAccountId()));
        final Map<String, String> previousTags = TagHelper.getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(resourceModel, handlerRequest);
        final Map<String, String> tagsToAdd = TagHelper.generateTagsToAdd(previousTags, desiredTags);
        if (tagsToAdd.isEmpty()) {
            logger.log(String.format("No tags to add for %s.", resourceModel.getArn()));
            return ProgressEvent.progress(resourceModel, callbackContext);
        }
        return proxy.initiate("AWS-Rekognition-StreamProcessor::TagResource", serviceClient, resourceModel, callbackContext)
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
            logger.log(String.format("%s successfully removed Tags.", software.amazon.rekognition.streamprocessor.ResourceModel.TYPE_NAME));
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
            logger.log(String.format("%s successfully added Tags.", software.amazon.rekognition.streamprocessor.ResourceModel.TYPE_NAME));
            return response;

        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }

}
