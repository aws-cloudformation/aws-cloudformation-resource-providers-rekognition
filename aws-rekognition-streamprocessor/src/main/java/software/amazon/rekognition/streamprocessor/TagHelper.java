package software.amazon.rekognition.streamprocessor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class TagHelper {
    /**
     * convertToMap
     *
     * Converts a collection of Tag objects to a tagName - tagValue map.
     *
     * Note: Tag objects with null tag values will not be included in the output
     * map.
     *
     * @param tags Collection of tags to convert
     * @return Converted Map of tags
     */
    public static Map<String, String> convertToMap(final Collection<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyMap();
        }
        return tags.stream()
            .filter(tag -> tag.getValue() != null)
            .collect(Collectors.toMap(
                Tag::getKey,
                Tag::getValue,
                (oldValue, newValue) -> newValue));
    }

    /**
     * convertToSet
     *
     * Converts a tag map to a set of Tag objects.
     *
     * Note: Like convertToMap, convertToSet filters out value-less tag entries.
     *
     * @param tagMap Map of tags to convert
     * @return Set of Tag objects
     */
    public static Set<Tag> convertToSet(final Map<String, String> tagMap) {
        if (MapUtils.isEmpty(tagMap)) {
            return Collections.emptySet();
        }
        return tagMap.entrySet().stream()
            .filter(tag -> tag.getValue() != null)
            .map(tag -> Tag.builder()
                .key(tag.getKey())
                .value(tag.getValue())
                .build())
            .collect(Collectors.toSet());
    }

    /**
     * generateTagsForCreate
     *
     * Generate tags to put into resource creation request.
     * This includes user defined tags and system tags as well.
     *
     * @param resourceModel The resource model
     * @param handlerRequest The resource handler request
     *
     * @return Map of tag keys and values
     */
    public static final Map<String, String> generateTagsForCreate(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> tagMap = new HashMap<>();

        // Stack level tags
        if (handlerRequest.getDesiredResourceTags() != null) {
            tagMap.putAll(handlerRequest.getDesiredResourceTags());
        }

        if (resourceModel.getTags() != null) {
            tagMap.putAll(convertToMap(resourceModel.getTags()));
        }

        return Collections.unmodifiableMap(tagMap);
    }

    /**
     * shouldUpdateTags
     *
     * Determines whether user defined tags have been changed during update.
     *
     * @param resourceModel The resource model
     * @param handlerRequest The resource handler request
     *
     * @return Boolean value indicating if the tags should be updated
     */
    public static final boolean shouldUpdateTags(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = getNewDesiredTags(resourceModel, handlerRequest);
        return ObjectUtils.notEqual(previousTags, desiredTags);
    }

    /**
     * getPreviouslyAttachedTags
     *
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get previous attached user defined tags from both handlerRequest.getPreviousResourceTags (stack tags)
     * and handlerRequest.getPreviousResourceState (resource tags).
     *
     * @param handlerRequest The resource handler request
     *
     * @return Map of previously attached tags
     */
    public static Map<String, String> getPreviouslyAttachedTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        Map<String, String> previousTags = new HashMap<>();
        // Stack level tags
        if(null != handlerRequest.getPreviousResourceTags()) {
            previousTags.putAll(handlerRequest.getPreviousResourceTags());
        }
        // Resource level tags
        previousTags.putAll(convertToMap(handlerRequest.getPreviousResourceState().getTags()));
        return previousTags;
    }

    /**
     * getNewDesiredTags
     *
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get new user defined tags from both resource model and previous stack tags.
     *
     * @param resourceModel The resource model
     * @param handlerRequest The resource handler request
     *
     * @return Map of new desired tag keys and values
     */
    public static Map<String, String> getNewDesiredTags(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        Map<String, String> desiredTags = new HashMap<>();
        // Stack level tags
        if(null != handlerRequest.getDesiredResourceTags()) {
            desiredTags.putAll(handlerRequest.getDesiredResourceTags());
        }
        // Resource level tags
        desiredTags.putAll(convertToMap(resourceModel.getTags()));
        return desiredTags;
    }

    /**
     * generateTagsToAdd
     *
     * Determines the tags the customer desired to define or redefine.
     *
     * @param previousTags The previous tags in the resource
     * @param desiredTags The new desired set of tags in the resource.
     *
     * @return Map of tag keys and values that need to be added, and not currently present in the resource
     */
    public static Map<String, String> generateTagsToAdd(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        return desiredTags.entrySet().stream()
            .filter(e -> !previousTags.containsKey(e.getKey()) || !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    /**
     * getTagsToRemove
     *
     * Determines the tags the customer desired to remove from the function.
     *
     * @param previousTags The previous tags in the resource
     * @param desiredTags The new desired set of tags in the resource.
     *
     * @return Set of keys to be removed
     */
    public static Set<String> generateTagsToRemove(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        final Set<String> desiredTagNames = desiredTags.keySet();

        return previousTags.keySet().stream()
            .filter(tagName -> !desiredTagNames.contains(tagName))
            .collect(Collectors.toSet());
    }
}
