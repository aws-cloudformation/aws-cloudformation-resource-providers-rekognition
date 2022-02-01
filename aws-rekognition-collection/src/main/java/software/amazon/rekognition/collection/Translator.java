package software.amazon.rekognition.collection;

import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.ListCollectionsRequest;
import software.amazon.awssdk.services.rekognition.model.ListCollectionsResponse;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.rekognition.model.TagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.UntagResourceRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

    private static final Integer MAX_LIST_RESULTS = 50;
    private static final TagHelper tagHelper = new TagHelper();

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static CreateCollectionRequest translateToCreateRequest(final ResourceModel model, ResourceHandlerRequest<ResourceModel> request) {
        return CreateCollectionRequest.builder()
            .collectionId(model.getCollectionId())
            .tags(tagHelper.generateTagsForCreate(model, request))
            .build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static DescribeCollectionRequest translateToReadRequest(final ResourceModel model) {
        return DescribeCollectionRequest.builder()
            .collectionId(model.getCollectionId())
            .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromDescribeResponse(
        final DescribeCollectionResponse awsResponse,
        final ResourceModel previousModel
    ) {
        return previousModel.toBuilder()
            .arn(awsResponse.collectionARN())
            .build();
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteCollectionRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteCollectionRequest.builder()
            .collectionId(model.getCollectionId())
            .build();
    }

    /**
     * Request to list tags on a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListTagsForResourceRequest translateToListTagsRequest(final ResourceModel model) {
        return ListTagsForResourceRequest.builder()
            .resourceArn(model.getArn())
            .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromListTagsResponse(
        final ListTagsForResourceResponse awsResponse,
        final ResourceModel previousResourceModel) {
        return previousResourceModel.toBuilder()
            .tags(TagHelper.convertToSet(awsResponse.tags()))
            .build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListCollectionsRequest translateToListRequest(final String nextToken) {
        return ListCollectionsRequest.builder()
            .maxResults(MAX_LIST_RESULTS)
            .nextToken(nextToken)
            .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(final ListCollectionsResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.collectionIds())
            .map(resource -> ResourceModel.builder()
                .collectionId(resource)
                .build())
            .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static TagResourceRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
        return TagResourceRequest.builder()
            .resourceArn(model.getArn())
            .tags(addedTags)
            .build();
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
        return UntagResourceRequest.builder()
            .resourceArn(model.getArn())
            .tagKeys(removedTags)
            .build();
    }
}
