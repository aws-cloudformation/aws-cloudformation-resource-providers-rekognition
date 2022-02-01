package software.amazon.rekognition.collection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
                proxy.initiate("AWS-Rekognition-Collection::DescribeCollection", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(Translator::translateToReadRequest)
                    .makeServiceCall(this::describeCollection)
                    .done(response -> ProgressEvent.progress(Translator.translateFromDescribeResponse(response, progress.getResourceModel()), callbackContext))
            )
            .then(progress ->
                proxy.initiate("AWS-Rekognition-Collection::ListTagsForResource", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(Translator::translateToListTagsRequest)
                    .makeServiceCall(this::listTagsForCollection)
                    .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromListTagsResponse(response, progress.getResourceModel())))
            );
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

    private ListTagsForResourceResponse listTagsForCollection(
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
