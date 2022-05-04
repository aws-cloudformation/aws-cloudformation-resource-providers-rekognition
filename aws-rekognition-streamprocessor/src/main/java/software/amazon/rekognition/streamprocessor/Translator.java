package software.amazon.rekognition.streamprocessor;

import com.amazonaws.util.CollectionUtils;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsRequest;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsResponse;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rekognition.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.rekognition.model.RegionOfInterest;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorDataSharingPreference;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorInput;
import software.amazon.awssdk.services.rekognition.model.KinesisVideoStream;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorNotificationChannel;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorOutput;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorParameterToDelete;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorSettings;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorSettingsForUpdate;
import software.amazon.awssdk.services.rekognition.model.TagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.UntagResourceRequest;
import software.amazon.awssdk.services.rekognition.model.UpdateStreamProcessorRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {
  private static final Integer MAX_LIST_RESULTS = 50;

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateStreamProcessorRequest translateToCreateRequest(final ResourceModel model,
                                                               final ResourceHandlerRequest<ResourceModel> request) {
    // Construct input
    final StreamProcessorInput input = StreamProcessorInput.builder()
            .kinesisVideoStream(
                    KinesisVideoStream.builder()
                    .arn(model.getKinesisVideoStream().getArn())
                    .build())
            .build();

    // Construct settings
    final StreamProcessorSettings settings = StreamProcessorSettings.builder()
            .faceSearch(CfnToRekModelsMapper.cfnFaceSearchToRekFaceSearch(model.getFaceSearchSettings()))
            .connectedHome(CfnToRekModelsMapper.cfnConnectedHomeToRekConnectedHome(model.getConnectedHomeSettings()))
            .build();

    // Construct output
    StreamProcessorOutput output = StreamProcessorOutput.builder()
            .s3Destination(CfnToRekModelsMapper.cfnS3DestinationToRekS3Destination(model.getS3Destination()))
            .kinesisDataStream(CfnToRekModelsMapper.cfnKdsToRekKds(model.getKinesisDataStream()))
            .build();

    // Construct SNS channel
    StreamProcessorNotificationChannel notificationChannel =
            CfnToRekModelsMapper.cfnNotificationChannelToRekNotificationChannel(model.getNotificationChannel());

    // ROI
    List<RegionOfInterest> regionsOfInterest = getROI(model);

    // Data sharing preference
    StreamProcessorDataSharingPreference dsp = CfnToRekModelsMapper.cfnDspToRekDsp(model.getDataSharingPreference());

    return CreateStreamProcessorRequest.builder()
            // Name
            .name(model.getName())
            // Role ARN
            .roleArn(model.getRoleArn())
            //KMS key
            .kmsKeyId(model.getKmsKeyId())
            // Input
            .input(input)
            // Settings
            .settings(settings)
            // output
            .output(output)
            // SNS channel
            .notificationChannel(notificationChannel)
            // Regions of Interest
            .regionsOfInterest(regionsOfInterest)
            // Data sharing preference
            .dataSharingPreference(dsp)
            // Tags
            .tags(TagHelper.generateTagsForCreate(model, request))
            .build();
  }

  /**
   * Request to read a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeStreamProcessorRequest translateToDescribeRequest(final ResourceModel model) {
    return DescribeStreamProcessorRequest.builder()
            .name(model.getName())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromDescribeResponse(
          final DescribeStreamProcessorResponse awsResponse,
          final ResourceModel previousModel
  ) {
    // Input
    software.amazon.rekognition.streamprocessor.KinesisVideoStream kvsObject =
            software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                    .arn(awsResponse.input().kinesisVideoStream().arn())
                    .build();

    // Settings
    software.amazon.rekognition.streamprocessor.FaceSearchSettings faceSearchObject =
            RekToCfnModelsMapper.rekFaceSearchToCfnFaceSearch(awsResponse.settings().faceSearch());
    software.amazon.rekognition.streamprocessor.ConnectedHomeSettings connectedHomeObject =
            RekToCfnModelsMapper.rekConnectedHomeToCfnConnectedHome(awsResponse.settings().connectedHome());

    // Output
    software.amazon.rekognition.streamprocessor.S3Destination s3dObject =
            RekToCfnModelsMapper.rekS3DestinationToCfnS3Destination(awsResponse.output().s3Destination());
    software.amazon.rekognition.streamprocessor.KinesisDataStream kdsObject =
            RekToCfnModelsMapper.rekKdsToCfnKds(awsResponse.output().kinesisDataStream());

    // SNSTopic
    NotificationChannel snsObject = RekToCfnModelsMapper.rekNotificationChannelToCfnNotificationChannel(
            awsResponse.notificationChannel());

    // RoI
    Set<software.amazon.rekognition.streamprocessor.BoundingBox> boundingBoxSet = new HashSet<>();
    Set<List<software.amazon.rekognition.streamprocessor.Point>> polygonsSet = new HashSet<>();
    RekToCfnModelsMapper.extractBoundingBoxesAndPolygonsFromRekROIs(
            awsResponse.regionsOfInterest(), boundingBoxSet, polygonsSet);

    // Data sharing preference
     DataSharingPreference dspObject = RekToCfnModelsMapper.rekDspToCfnDsp(awsResponse.dataSharingPreference());

    // Populate the resource model and return
    return previousModel.toBuilder()
            .name(awsResponse.name())
            .arn(awsResponse.streamProcessorArn())
            .roleArn(awsResponse.roleArn())
            .kmsKeyId(awsResponse.kmsKeyId())
            .kinesisVideoStream(kvsObject)
            .faceSearchSettings(faceSearchObject)
            .connectedHomeSettings(connectedHomeObject)
            .kinesisDataStream(kdsObject)
            .s3Destination(s3dObject)
            .notificationChannel(snsObject)
            .status(awsResponse.statusAsString())
            .statusMessage(awsResponse.statusMessage())
            .boundingBoxRegionsOfInterest((0 == boundingBoxSet.size()) ? null : boundingBoxSet)
            .polygonRegionsOfInterest((0 == polygonsSet.size()) ? null : polygonsSet)
            .dataSharingPreference(dspObject)
            .build();
  }

  /**
   * Request to delete a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteStreamProcessorRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteStreamProcessorRequest.builder()
            .name(model.getName())
            .build();
  }

  /**
   * Request to list tags on a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to list tags of a resource with teh given ARN
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
  static ListStreamProcessorsRequest translateToListRequest(final String nextToken) {
    return ListStreamProcessorsRequest.builder()
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
  static List<ResourceModel> translateFromListResponse(final ListStreamProcessorsResponse awsResponse) {
    return streamOfOrEmpty(awsResponse.streamProcessors())
            .map(resource -> ResourceModel.builder()
                    .name(resource.name())
                    .status(resource.statusAsString())
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
   * @param addedTags A map of tag keys and values that need to be added to the resource
   *
   * @return The aws request to add tags to the resource
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
   * @param removedTags A set of tag keys that need to be removed from the resource
   *
   * @return The aws request to untag the resource
   */
  static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    return UntagResourceRequest.builder()
            .resourceArn(model.getArn())
            .tagKeys(removedTags)
            .build();
  }

   static List<RegionOfInterest> getROI(final ResourceModel model) {
    // ROI
    List<RegionOfInterest> bbCollection = CfnToRekModelsMapper.cfnBoundingBoxSetToRekRoiList(model.getBoundingBoxRegionsOfInterest());
    List<RegionOfInterest> polygonCollection = CfnToRekModelsMapper.cfnPolygonSetToRekRoiList(model.getPolygonRegionsOfInterest());
    return CollectionUtils.mergeLists(bbCollection, polygonCollection);
  }

}
