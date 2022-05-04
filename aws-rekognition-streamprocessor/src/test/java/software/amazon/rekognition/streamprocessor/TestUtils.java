package software.amazon.rekognition.streamprocessor;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.FaceSearchSettings;
import software.amazon.awssdk.services.rekognition.model.ConnectedHomeSettings;
import software.amazon.awssdk.services.rekognition.model.RegionOfInterest;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorDataSharingPreference;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorInput;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorNotificationChannel;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorOutput;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorSettings;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.emptySet;

public class TestUtils {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String TEST_STREAM_PROCESSOR_NAME;
    protected static final String TEST_STREAM_PROCESSOR_ARN;
    protected static final String TEST_CUSTOMER_ROLE_ARN;
    protected static final String TEST_CUSTOMER_S3_BUCKET;
    protected static final String TEST_CUSTOMER_S3_OBJECT_KEY_PREFIX;
    protected static final String TEST_CUSTOMER_KVS_ARN;
    protected static final String TEST_CUSTOMER_KDS_ARN;
    protected static final String TEST_CUSTOMER_SNS_ARN;
    protected static final String TEST_STREAM_PROCESSOR_STATUS;
    protected static final String TEST_CUSTOMER_KMS_KEY_ID;
    protected static final String TEST_CUSTOMER_COLLECTION_ID;
    protected static final String TEST_FACE_MODEL_VERSION;
    protected static final Instant TEST_TIMESTAMP;
    protected static final Long TEST_FACE_COUNT;
    protected static final Map<String, String> TEST_TAGS;
    protected static final DescribeStreamProcessorResponse DEFAULT_DESCRIBE_RESPONSE;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        TEST_STREAM_PROCESSOR_NAME = "test";
        TEST_CUSTOMER_ROLE_ARN = "arn:aws:iam::123456789012:role/RoleForRek";
        TEST_CUSTOMER_S3_BUCKET = "test-bucket";
        TEST_CUSTOMER_S3_OBJECT_KEY_PREFIX = "test/key/prefix/";
        TEST_CUSTOMER_KVS_ARN = "arn:aws:kinesisvideo:us-east-1:123456789012:stream/test_stream/0123456789012";
        TEST_CUSTOMER_KDS_ARN = "arn:aws:kinesis:us-east-1:123456789012:test_stream/stream1";
        TEST_CUSTOMER_SNS_ARN = "arn:aws:sns:us-east-2:444455556666:TestTopic";
        TEST_CUSTOMER_KMS_KEY_ID = "1234abcd-12ab-34cd-56ef-1234567890ab";
        TEST_STREAM_PROCESSOR_STATUS = "STOPPED";
        TEST_CUSTOMER_COLLECTION_ID = "TestCollection";
        TEST_STREAM_PROCESSOR_ARN = String.format("arn:aws:rekognition:us-east-1:123456789012:streamprocessor/%s", TEST_STREAM_PROCESSOR_NAME);
        TEST_TIMESTAMP = Instant.parse("2021-01-01T00:00:00.000Z");
        TEST_FACE_MODEL_VERSION = "5";
        TEST_FACE_COUNT = 0L;
        TEST_TAGS = ImmutableMap.of("TEST_TAG_1", "TEST_VALUE_1", "TEST_TAG_2", "TEST_VALUE_2");
        DEFAULT_DESCRIBE_RESPONSE = DescribeStreamProcessorResponse.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                .build();
    }

    // **********Helper methods needed for create handler**********
    protected static ResourceModel getInputModelForConnectedHomeCreateHandler() {
        return ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .connectedHomeSettings(software.amazon.rekognition.streamprocessor.ConnectedHomeSettings.builder()
                        .labels(new HashSet<>(Arrays.asList("PERSON")))
                        .build())
                .s3Destination(software.amazon.rekognition.streamprocessor.S3Destination.builder()
                        .bucketName(TEST_CUSTOMER_S3_BUCKET)
                        .build())
                .notificationChannel(NotificationChannel.builder()
                        .arn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .boundingBoxRegionsOfInterest(new HashSet<>(Arrays.asList(
                        BoundingBox.builder()
                                .height(Double.parseDouble("0.0"))
                                .width(Double.parseDouble("0.2"))
                                .left(Double.parseDouble("0.1"))
                                .top(Double.parseDouble("0.5"))
                                .build()))
                )
                .tags(emptySet())
                .build();
    }

    protected static DescribeStreamProcessorResponse getConnectedHomeDescribeResponseForCreateHandler() {
        return DescribeStreamProcessorResponse.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .input(StreamProcessorInput.builder()
                        .kinesisVideoStream(software.amazon.awssdk.services.rekognition.model.KinesisVideoStream.builder()
                                .arn(TEST_CUSTOMER_KVS_ARN)
                                .build())
                        .build())
                .settings(StreamProcessorSettings.builder()
                        .connectedHome(ConnectedHomeSettings.builder()
                                .labels(Arrays.asList("PERSON"))
                                .build())
                        .build())
                .output(StreamProcessorOutput.builder()
                        .s3Destination(software.amazon.awssdk.services.rekognition.model.S3Destination.builder()
                                .bucket(TEST_CUSTOMER_S3_BUCKET)
                                .build())
                        .build())
                .notificationChannel(StreamProcessorNotificationChannel.builder()
                        .snsTopicArn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .regionsOfInterest(RegionOfInterest.builder()
                        .boundingBox(software.amazon.awssdk.services.rekognition.model.BoundingBox.builder().width(0.2f)
                                .top(0.5f).height(0.0f).left(0.1f).build())
                        .build())
                .creationTimestamp(Instant.now().minusSeconds(3600))
                .lastUpdateTimestamp(Instant.now().minusSeconds(300))
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .build();
    }

    protected static ResourceModel getExpectedModelForConnectedHomeCreateHandler() {
        return ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .connectedHomeSettings(software.amazon.rekognition.streamprocessor.ConnectedHomeSettings.builder()
                        .labels(new HashSet<>(Arrays.asList("PERSON")))
                        .build())
                .s3Destination(software.amazon.rekognition.streamprocessor.S3Destination.builder()
                        .bucketName(TEST_CUSTOMER_S3_BUCKET)
                        .build())
                .notificationChannel(NotificationChannel.builder()
                        .arn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .boundingBoxRegionsOfInterest(new HashSet<>(Arrays.asList(
                        BoundingBox.builder()
                                .height(Double.parseDouble("0.0"))
                                .width(Double.parseDouble("0.2"))
                                .left(Double.parseDouble("0.1"))
                                .top(Double.parseDouble("0.5"))
                                .build()))
                )
                .tags(emptySet())
                .build();
    }

    protected static ResourceModel getInputModelForFaceSearchCreateHandler() {
        return ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .faceSearchSettings(software.amazon.rekognition.streamprocessor.FaceSearchSettings.builder()
                        .collectionId(TEST_CUSTOMER_COLLECTION_ID)
                        .build())
                .kinesisDataStream(software.amazon.rekognition.streamprocessor.KinesisDataStream.builder()
                        .arn(TEST_CUSTOMER_KDS_ARN)
                        .build())
                .tags(emptySet())
                .build();
    }

    protected static DescribeStreamProcessorResponse getFaceSearchDescribeResponseForCreateHandler() {
        return DescribeStreamProcessorResponse.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .input(StreamProcessorInput.builder()
                        .kinesisVideoStream(software.amazon.awssdk.services.rekognition.model.KinesisVideoStream.builder()
                                .arn(TEST_CUSTOMER_KVS_ARN)
                                .build())
                        .build())
                .settings(StreamProcessorSettings.builder()
                        .faceSearch(FaceSearchSettings.builder()
                                .collectionId(TEST_CUSTOMER_COLLECTION_ID)
                                .build())
                        .build())
                .output(StreamProcessorOutput.builder()
                        .kinesisDataStream(software.amazon.awssdk.services.rekognition.model.KinesisDataStream.builder()
                                .arn(TEST_CUSTOMER_KDS_ARN)
                                .build())
                        .build())
                .creationTimestamp(Instant.now().minusSeconds(3600))
                .lastUpdateTimestamp(Instant.now().minusSeconds(300))
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .build();
    }

    protected static ResourceModel getExpectedModelForFaceSearchCreateHandler() {
        return ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .faceSearchSettings(software.amazon.rekognition.streamprocessor.FaceSearchSettings.builder()
                        .collectionId(TEST_CUSTOMER_COLLECTION_ID)
                        .build())
                .kinesisDataStream(software.amazon.rekognition.streamprocessor.KinesisDataStream.builder()
                        .arn(TEST_CUSTOMER_KDS_ARN)
                        .build())
                .tags(emptySet())
                .build();
    }

    // **********Helper methods needed for read handler**********

    protected static ResourceModel getExpectedModelForConnectedHomeReadHandler() {
        return ResourceModel.builder()
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .name(TEST_STREAM_PROCESSOR_NAME)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .kmsKeyId(TEST_CUSTOMER_KMS_KEY_ID)
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .connectedHomeSettings(software.amazon.rekognition.streamprocessor.ConnectedHomeSettings.builder()
                        .labels(new HashSet<>(Arrays.asList("PERSON", "PACKAGE")))
                        .build())
                .s3Destination(software.amazon.rekognition.streamprocessor.S3Destination.builder()
                        .bucketName(TEST_CUSTOMER_S3_BUCKET)
                        .objectKeyPrefix(TEST_CUSTOMER_S3_OBJECT_KEY_PREFIX)
                        .build())
                .notificationChannel(NotificationChannel.builder()
                        .arn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .polygonRegionsOfInterest(new HashSet<>(Arrays.asList(
                        Arrays.asList(
                                software.amazon.rekognition.streamprocessor.Point.builder().x(Double.parseDouble("0.0")).y(Double.parseDouble("0.0")).build(),
                                software.amazon.rekognition.streamprocessor.Point.builder().x(Double.parseDouble("0.4")).y(Double.parseDouble("0.0")).build(),
                                software.amazon.rekognition.streamprocessor.Point.builder().x(Double.parseDouble("0.4")).y(Double.parseDouble("1.0")).build(),
                                software.amazon.rekognition.streamprocessor.Point.builder().x(Double.parseDouble("0.0")).y(Double.parseDouble("1.0")).build()
                        )
                )))
                .dataSharingPreference(DataSharingPreference.builder().optIn(false).build())
                .tags(emptySet())
                .build();
    }

    protected static ResourceModel getExpectedModelForFaceSearchReadHandler() {
        return ResourceModel.builder()
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .name(TEST_STREAM_PROCESSOR_NAME)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .faceSearchSettings(software.amazon.rekognition.streamprocessor.FaceSearchSettings.builder()
                        .collectionId(TEST_CUSTOMER_COLLECTION_ID)
                        .faceMatchThreshold(Double.parseDouble("0.8"))
                        .build())
                .kinesisDataStream(software.amazon.rekognition.streamprocessor.KinesisDataStream.builder()
                        .arn(TEST_CUSTOMER_KDS_ARN)
                        .build())
                .tags(emptySet())
                .build();
    }

    protected static DescribeStreamProcessorResponse getConnectedHomeDescribeResponseForReadHandler() {
        return DescribeStreamProcessorResponse.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .input(StreamProcessorInput.builder()
                        .kinesisVideoStream(software.amazon.awssdk.services.rekognition.model.KinesisVideoStream.builder()
                                .arn(TEST_CUSTOMER_KVS_ARN)
                                .build())
                        .build())
                .settings(StreamProcessorSettings.builder()
                        .connectedHome(ConnectedHomeSettings.builder()
                                .labels(Arrays.asList("PERSON", "PACKAGE"))
                                .build())
                        .build())
                .output(StreamProcessorOutput.builder()
                        .s3Destination(software.amazon.awssdk.services.rekognition.model.S3Destination.builder()
                                .bucket(TEST_CUSTOMER_S3_BUCKET)
                                .keyPrefix(TEST_CUSTOMER_S3_OBJECT_KEY_PREFIX)
                                .build())
                        .build())
                .notificationChannel(StreamProcessorNotificationChannel.builder()
                        .snsTopicArn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .kmsKeyId(TEST_CUSTOMER_KMS_KEY_ID)
                .regionsOfInterest(RegionOfInterest.builder()
                        .polygon(Arrays.asList(
                                software.amazon.awssdk.services.rekognition.model.Point.builder().x(0.0f).y(0.0f).build(),
                                software.amazon.awssdk.services.rekognition.model.Point.builder().x(0.4f).y(0.0f).build(),
                                software.amazon.awssdk.services.rekognition.model.Point.builder().x(0.4f).y(1.0f).build(),
                                software.amazon.awssdk.services.rekognition.model.Point.builder().x(0.0f).y(1.0f).build()
                        ))
                        .build())
                .dataSharingPreference(StreamProcessorDataSharingPreference.builder().optIn(false).build())
                .creationTimestamp(Instant.now().minusSeconds(3600))
                .lastUpdateTimestamp(Instant.now().minusSeconds(300))
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .build();
    }

    protected static DescribeStreamProcessorResponse getFaceSearchDescribeResponseForReadHandler() {
        return DescribeStreamProcessorResponse.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .input(StreamProcessorInput.builder()
                        .kinesisVideoStream(software.amazon.awssdk.services.rekognition.model.KinesisVideoStream.builder()
                                .arn(TEST_CUSTOMER_KVS_ARN)
                                .build())
                        .build())
                .settings(StreamProcessorSettings.builder()
                        .faceSearch(FaceSearchSettings.builder()
                                .collectionId(TEST_CUSTOMER_COLLECTION_ID)
                                .faceMatchThreshold(0.8f)
                                .build())
                        .build())
                .output(StreamProcessorOutput.builder()
                        .kinesisDataStream(software.amazon.awssdk.services.rekognition.model.KinesisDataStream.builder()
                                .arn(TEST_CUSTOMER_KDS_ARN)
                                .build())
                        .build())
                .creationTimestamp(Instant.now().minusSeconds(3600))
                .lastUpdateTimestamp(Instant.now().minusSeconds(300))
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .build();
    }

    // **********Helper methods needed for update handler**********

    protected static DescribeStreamProcessorResponse getConnectedHomeDescribeResponseForUpdateHandler() {
        return DescribeStreamProcessorResponse.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .streamProcessorArn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .input(StreamProcessorInput.builder()
                        .kinesisVideoStream(software.amazon.awssdk.services.rekognition.model.KinesisVideoStream.builder()
                                .arn(TEST_CUSTOMER_KVS_ARN)
                                .build())
                        .build())
                .settings(StreamProcessorSettings.builder()
                        .connectedHome(ConnectedHomeSettings.builder()
                                .labels(Arrays.asList("PERSON"))
                                .minConfidence(50f)
                                .build())
                        .build())
                .output(StreamProcessorOutput.builder()
                        .s3Destination(software.amazon.awssdk.services.rekognition.model.S3Destination.builder()
                                .bucket(TEST_CUSTOMER_S3_BUCKET)
                                .build())
                        .build())
                .notificationChannel(StreamProcessorNotificationChannel.builder()
                        .snsTopicArn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .regionsOfInterest(RegionOfInterest.builder()
                        .boundingBox(software.amazon.awssdk.services.rekognition.model.BoundingBox.builder().width(0.2f)
                                .top(0.5f).height(0.0f).left(0.1f).build())
                        .build())
                .dataSharingPreference(software.amazon.awssdk.services.rekognition.model
                        .StreamProcessorDataSharingPreference.builder().optIn(true)
                        .build())
                .creationTimestamp(Instant.now().minusSeconds(3600))
                .lastUpdateTimestamp(Instant.now().minusSeconds(300))
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .build();
    }

    protected static ResourceModel getInputModelForConnectedHomeUpdateHandler() {
        return ResourceModel.builder()
                .arn(TEST_STREAM_PROCESSOR_ARN) //arn is added to desired resource model by cfn
                .name(TEST_STREAM_PROCESSOR_NAME)
                .connectedHomeSettings(software.amazon.rekognition.streamprocessor.ConnectedHomeSettings.builder()
                        .labels(new HashSet<>(Arrays.asList("PERSON")))
                        .build())
                .s3Destination(software.amazon.rekognition.streamprocessor.S3Destination.builder()
                        .bucketName(TEST_CUSTOMER_S3_BUCKET)
                        .build())
                .notificationChannel(NotificationChannel.builder()
                        .arn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .boundingBoxRegionsOfInterest(new HashSet<>(Arrays.asList(
                        BoundingBox.builder()
                                .height(Double.parseDouble("0.0"))
                                .width(Double.parseDouble("0.2"))
                                .left(Double.parseDouble("0.1"))
                                .top(Double.parseDouble("0.5"))
                                .build())))
                .dataSharingPreference(DataSharingPreference.builder().optIn(true).build())
                .build();
    }

    protected static ResourceModel getExpectedModelForConnectedHomeUpdateHandler() {
        return ResourceModel.builder()
                .name(TEST_STREAM_PROCESSOR_NAME)
                .arn(TEST_STREAM_PROCESSOR_ARN)
                .roleArn(TEST_CUSTOMER_ROLE_ARN)
                .status(TEST_STREAM_PROCESSOR_STATUS)
                .statusMessage("Stream processor stopped")
                .kinesisVideoStream(software.amazon.rekognition.streamprocessor.KinesisVideoStream.builder()
                        .arn(TEST_CUSTOMER_KVS_ARN)
                        .build())
                .connectedHomeSettings(software.amazon.rekognition.streamprocessor.ConnectedHomeSettings.builder()
                        .labels(new HashSet<>(Arrays.asList("PERSON")))
                        .minConfidence(50.0)
                        .build())
                .s3Destination(software.amazon.rekognition.streamprocessor.S3Destination.builder()
                        .bucketName(TEST_CUSTOMER_S3_BUCKET)
                        .build())
                .notificationChannel(NotificationChannel.builder()
                        .arn(TEST_CUSTOMER_SNS_ARN)
                        .build())
                .boundingBoxRegionsOfInterest(new HashSet<>(Arrays.asList(
                        BoundingBox.builder()
                                .height(Double.parseDouble("0.0"))
                                .width(Double.parseDouble("0.2"))
                                .left(Double.parseDouble("0.1"))
                                .top(Double.parseDouble("0.5"))
                                .build()))
                )
                .dataSharingPreference(software.amazon.rekognition.streamprocessor.DataSharingPreference.builder().optIn(true).build())
                .tags(emptySet())
                .build();
    }

}
