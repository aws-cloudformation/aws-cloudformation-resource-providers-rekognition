package software.amazon.rekognition.collection;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.InvalidPaginationTokenException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.rekognition.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.rekognition.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import lombok.experimental.Accessors;
import lombok.Getter;

@Accessors(fluent = true)
public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    @Getter
    private final RekognitionClient rekognitionClient;

    protected BaseHandlerStd() {
        this.rekognitionClient = ClientBuilder.getClient();
    }

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        return handleRequest(
            proxy,
            request,
            callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(this::rekognitionClient),
            logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RekognitionClient> proxyClient,
        final Logger logger);

    protected BaseHandlerException handlerError(final Exception exception, final Logger logger) {
        if (exception instanceof AccessDeniedException) {
            logger.log("We can't process the request because you are not authorized to perform the action.");
            return new CfnAccessDeniedException(exception);

        } else if (exception instanceof InvalidParameterException) {
            logger.log("Input parameter violated a constraint. Validate your parameter before calling the API operation again.");
            return new CfnInvalidRequestException(exception);

        } else if (exception instanceof ProvisionedThroughputExceededException) {
            logger.log("The number of requests exceeded your throughput limit. If you want to increase this limit, contact Amazon Rekognition.");
            return new CfnServiceLimitExceededException(exception);

        } else if (exception instanceof ResourceNotFoundException) {
            logger.log("The resource specified in the request cannot be found.");
            return new CfnNotFoundException(exception);

        } else if (exception instanceof ThrottlingException) {
            logger.log("Amazon Rekognition is temporarily unable to process the request. Try your call again.");
            return new CfnThrottlingException(exception);

        } else if (exception instanceof ResourceAlreadyExistsException) {
            logger.log("There is already a resource with this name. Try again with a different name.");
            return new CfnAlreadyExistsException(exception);

        } else if (exception instanceof ServiceQuotaExceededException) {
            logger.log("The size of the resource exceeds the allowed limit. For more information, see Guidelines and quotas in Amazon Rekognition.");
            return new CfnServiceLimitExceededException(exception);

        } else if (exception instanceof InvalidPaginationTokenException) {
            logger.log("Pagination token in the request is not valid.");
            return new CfnInvalidRequestException(exception);

        }
        return new CfnServiceInternalErrorException(exception);
    }
}
