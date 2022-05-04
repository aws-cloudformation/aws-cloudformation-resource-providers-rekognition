package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.services.rekognition.model.RegionOfInterest;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorDataSharingPreference;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorNotificationChannel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper class that maps CFN resource schema models to Rekognition models
 */
public class CfnToRekModelsMapper {
    static software.amazon.awssdk.services.rekognition.model.FaceSearchSettings cfnFaceSearchToRekFaceSearch(
            software.amazon.rekognition.streamprocessor.FaceSearchSettings faceSearch) {
        return (null == faceSearch) ? null
                : software.amazon.awssdk.services.rekognition.model.FaceSearchSettings.builder()
                .collectionId(faceSearch.getCollectionId())
                .faceMatchThreshold((null != faceSearch.getFaceMatchThreshold())
                        ? new Float(faceSearch.getFaceMatchThreshold())
                        : null)
                .build();
    }

    static software.amazon.awssdk.services.rekognition.model.ConnectedHomeSettings cfnConnectedHomeToRekConnectedHome(
            software.amazon.rekognition.streamprocessor.ConnectedHomeSettings connectedHomeSettings) {
        return (null == connectedHomeSettings) ? null
                : software.amazon.awssdk.services.rekognition.model.ConnectedHomeSettings.builder()
                .labels(connectedHomeSettings.getLabels())
                .minConfidence((null != connectedHomeSettings.getMinConfidence())
                        ? new Float(connectedHomeSettings.getMinConfidence())
                        : null)
                .build();
    }

    static software.amazon.awssdk.services.rekognition.model.S3Destination cfnS3DestinationToRekS3Destination(
            software.amazon.rekognition.streamprocessor.S3Destination s3d) {
        return (null == s3d) ? null
                : software.amazon.awssdk.services.rekognition.model.S3Destination.builder()
                .bucket(s3d.getBucketName())
                .keyPrefix(s3d.getObjectKeyPrefix())
                .build();
    }

    static software.amazon.awssdk.services.rekognition.model.KinesisDataStream cfnKdsToRekKds(
            software.amazon.rekognition.streamprocessor.KinesisDataStream kds) {
        return (null == kds) ? null
                : software.amazon.awssdk.services.rekognition.model.KinesisDataStream.builder()
                .arn(kds.getArn())
                .build();
    }

    static StreamProcessorNotificationChannel cfnNotificationChannelToRekNotificationChannel(
            NotificationChannel notificationChannel) {
        return (null == notificationChannel) ? null
                : StreamProcessorNotificationChannel.builder()
                .snsTopicArn(notificationChannel.getArn())
                .build();
    }

    static List<RegionOfInterest> cfnBoundingBoxSetToRekRoiList(
            Set<BoundingBox> bboxSet) {
        return (null == bboxSet) ? null
                : bboxSet.stream()
                .map(boundingBox ->
                        RegionOfInterest.builder()
                                .boundingBox(
                                        software.amazon.awssdk.services.rekognition.model.BoundingBox.builder()
                                                .height(new Float(boundingBox.getHeight()))
                                                .left(new Float(boundingBox.getLeft()))
                                                .top(new Float(boundingBox.getTop()))
                                                .width(new Float(boundingBox.getWidth()))
                                                .build())
                                .build())
                .collect(Collectors.toList());
    }

    static List<RegionOfInterest> cfnPolygonSetToRekRoiList(
            Set<List<software.amazon.rekognition.streamprocessor.Point>> polygonSet) {
        return (null == polygonSet) ? null
                : polygonSet.stream()
                .map(p ->
                        RegionOfInterest.builder()
                                .polygon(p.stream()
                                        .map(point ->
                                                software.amazon.awssdk.services.rekognition.model.Point.builder()
                                                        .x(new Float(point.getX()))
                                                        .y(new Float(point.getY()))
                                                        .build())
                                        .collect(Collectors.toList()))
                                .build())
                .collect(Collectors.toList());
    }

    static StreamProcessorDataSharingPreference cfnDspToRekDsp(DataSharingPreference dsp) {
        return (null == dsp) ? null
                : StreamProcessorDataSharingPreference.builder()
                .optIn(dsp.getOptIn())
                .build();
    }
}
