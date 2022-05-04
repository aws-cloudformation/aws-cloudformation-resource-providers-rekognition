package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Read Cloudformation handler for AWS::Rekognition::StreamProcessor Resource.
 * Flow -
 *  1. Call DescribeStreamProcessor.
 *  2. Call ListTagsforResource to return the stream processor's tags
 */
public class ReadHandler extends BaseHandlerStd {
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
                        proxy.initiate(READ_CALL_GRAPH_NAME, proxyClient, progress.getResourceModel(), callbackContext)
                                .translateToServiceRequest(Translator::translateToDescribeRequest)
                                .makeServiceCall(this::describeStreamProcessor)
                                .done(response -> ProgressEvent.progress(Translator.translateFromDescribeResponse(response, progress.getResourceModel()), callbackContext))
                )
                .then(progress ->
                        proxy.initiate(LIST_TAGS_CALL_GRAPH_NAME, proxyClient, progress.getResourceModel(), callbackContext)
                                .translateToServiceRequest(Translator::translateToListTagsRequest)
                                .makeServiceCall(this::listTagsForStreamProcessor)
                                .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromListTagsResponse(response, progress.getResourceModel())))
                );
    }

    private DescribeStreamProcessorResponse describeStreamProcessor(
            final DescribeStreamProcessorRequest request,
            final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            DescribeStreamProcessorResponse response = client.injectCredentialsAndInvokeV2(
                    request, client.client()::describeStreamProcessor);
            logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
            return response;
        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }

    private ListTagsForResourceResponse listTagsForStreamProcessor(
            final ListTagsForResourceRequest request,
            final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            ListTagsForResourceResponse response = client
                    .injectCredentialsAndInvokeV2(request, client.client()::listTagsForResource);
            logger.log(String.format("%s Tags have successfully been read.", ResourceModel.TYPE_NAME));
            return response;
        } catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }
}
