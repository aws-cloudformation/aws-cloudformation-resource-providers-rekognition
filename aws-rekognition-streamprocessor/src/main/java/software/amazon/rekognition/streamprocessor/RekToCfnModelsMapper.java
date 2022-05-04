package software.amazon.rekognition.streamprocessor;

import software.amazon.awssdk.services.rekognition.model.RegionOfInterest;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorDataSharingPreference;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorNotificationChannel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper class that maps Rekognition models to CFN resource schema models
 */
public class RekToCfnModelsMapper {

    static software.amazon.rekognition.streamprocessor.ConnectedHomeSettings rekConnectedHomeToCfnConnectedHome(
            software.amazon.awssdk.services.rekognition.model.ConnectedHomeSettings connectedHome) {
        return (null == connectedHome) ? null
                : software.amazon.rekognition.streamprocessor.ConnectedHomeSettings.builder()
                .labels(new HashSet<>(connectedHome.labels()))
                .minConfidence((null != connectedHome.minConfidence())
                        ? Double.parseDouble(connectedHome.minConfidence().toString())
                        : null)
                .build();
    }

    static software.amazon.rekognition.streamprocessor.FaceSearchSettings rekFaceSearchToCfnFaceSearch(
            software.amazon.awssdk.services.rekognition.model.FaceSearchSettings faceSearchSettings) {
        return (null == faceSearchSettings) ? null
                : software.amazon.rekognition.streamprocessor.FaceSearchSettings.builder()
                .collectionId(faceSearchSettings.collectionId())
                .faceMatchThreshold((null != faceSearchSettings.faceMatchThreshold())
                        ? Double.parseDouble(faceSearchSettings.faceMatchThreshold().toString())
                        : null)
                .build();
    }

    static software.amazon.rekognition.streamprocessor.S3Destination rekS3DestinationToCfnS3Destination(
            software.amazon.awssdk.services.rekognition.model.S3Destination s3Destination) {
        return (null == s3Destination) ? null
                : software.amazon.rekognition.streamprocessor.S3Destination.builder()
                .bucketName(s3Destination.bucket())
                .objectKeyPrefix(s3Destination.keyPrefix())
                .build();
    }

    static software.amazon.rekognition.streamprocessor.KinesisDataStream rekKdsToCfnKds(software.amazon.awssdk.services.rekognition.model.KinesisDataStream kds) {
        return (null == kds) ? null
                : software.amazon.rekognition.streamprocessor.KinesisDataStream.builder()
                .arn(kds.arn())
                .build();
    }

    static software.amazon.rekognition.streamprocessor.NotificationChannel rekNotificationChannelToCfnNotificationChannel(
            StreamProcessorNotificationChannel notificationChannel) {
        return (null == notificationChannel) ? null
                : NotificationChannel.builder()
                .arn(notificationChannel.snsTopicArn())
                .build();
    }

    static void extractBoundingBoxesAndPolygonsFromRekROIs(
            List<RegionOfInterest> rekRois,
            Set<BoundingBox> boundingBoxesOut,
            Set<List<software.amazon.rekognition.streamprocessor.Point>> polygonsOut) {

        for(RegionOfInterest roi : rekRois) {
            // If this ROI obj has a polygon
            if(roi.hasPolygon()) {
                List<software.amazon.rekognition.streamprocessor.Point> polygon = new ArrayList<>();
                for(software.amazon.awssdk.services.rekognition.model.Point point : roi.polygon()) {
                    polygon.add(software.amazon.rekognition.streamprocessor.Point.builder()
                            .x(Double.parseDouble(point.x().toString()))
                            .y(Double.parseDouble(point.y().toString()))
                            .build());
                }
                polygonsOut.add(polygon);
            }
            // If this ROI has a bbox instead
            software.amazon.awssdk.services.rekognition.model.BoundingBox bbox = roi.boundingBox();
            if(null != bbox) {
                software.amazon.rekognition.streamprocessor.BoundingBox bboxObj =
                        software.amazon.rekognition.streamprocessor.BoundingBox.builder()
                                .left(Double.parseDouble(bbox.left().toString()))
                                .top(Double.parseDouble(bbox.top().toString()))
                                .width(Double.parseDouble(bbox.width().toString()))
                                .height(Double.parseDouble(bbox.height().toString()))
                                .build();
                boundingBoxesOut.add(bboxObj);
            }
        }
    }

    static DataSharingPreference rekDspToCfnDsp(StreamProcessorDataSharingPreference dsp) {
        return (null == dsp) ? null
                : DataSharingPreference.builder()
                .optIn(dsp.optIn())
                .build();
    }
}
