package software.amazon.rekognition.streamprocessor;

import com.amazonaws.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

/**
 * Create Cloudformation handler for AWS::Rekognition::StreamProcessor Resource.
 * Flow -
 *  1. Call CreateStreamProcessor.
 *  2. Call ReadHandler to return the created stream processor (DescribeStreamProcessor + ListTagsForResource)
 */
public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Cfn Request: %s", request));

        final ResourceModel model = request.getDesiredResourceState();

        // If the user hasn't provided a stream processor name, we generate a unique ID for the resource
        setNameIfMissing(request);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate(CREATE_CALL_GRAPH_NAME, proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((resourceModel) -> Translator.translateToCreateRequest(model, request))
                    .makeServiceCall(this::createStreamProcessor)
                    .progress()
            )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateStreamProcessorResponse createStreamProcessor(
            final CreateStreamProcessorRequest request,
            final ProxyClient<RekognitionClient> client
    ) {
        try {
            logger.log(String.format("Service Request: %s", request));
            CreateStreamProcessorResponse response = client.injectCredentialsAndInvokeV2(request, client.client()::createStreamProcessor);
            logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
            return response;
        }
        catch (final AwsServiceException e) {
            throw this.handlerError(e, logger);
        }
    }

    @VisibleForTesting
    protected void setNameIfMissing(final ResourceHandlerRequest<ResourceModel> request) {
        if (StringUtils.isNullOrEmpty(request.getDesiredResourceState().getName())) {
            // Construct a name using the stack ID, logical ID, and client request token.
            final String generatedName = IdentifierUtils.generateResourceIdentifier(
                    request.getStackId(),
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    STREAM_PROCESSOR_NAME_MAX_LENGTH).toLowerCase();
            request.getDesiredResourceState().setName(generatedName);
        }
    }
}
