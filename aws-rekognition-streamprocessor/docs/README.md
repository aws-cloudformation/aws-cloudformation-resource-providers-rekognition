# AWS::Rekognition::StreamProcessor

The AWS::Rekognition::StreamProcessor type is used to create an Amazon Rekognition StreamProcessor that you can use to analyze streaming videos.



## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::Rekognition::StreamProcessor",
    "Properties" : {
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>" : <i>String</i>,
        "<a href="#rolearn" title="RoleArn">RoleArn</a>" : <i>String</i>,
        "<a href="#kinesisvideostream" title="KinesisVideoStream">KinesisVideoStream</a>" : <i><a href="kinesisvideostream.md">KinesisVideoStream</a></i>,
        "<a href="#facesearchsettings" title="FaceSearchSettings">FaceSearchSettings</a>" : <i><a href="facesearchsettings.md">FaceSearchSettings</a></i>,
        "<a href="#connectedhomesettings" title="ConnectedHomeSettings">ConnectedHomeSettings</a>" : <i><a href="connectedhomesettings.md">ConnectedHomeSettings</a></i>,
        "<a href="#kinesisdatastream" title="KinesisDataStream">KinesisDataStream</a>" : <i><a href="kinesisdatastream.md">KinesisDataStream</a></i>,
        "<a href="#s3destination" title="S3Destination">S3Destination</a>" : <i><a href="s3destination.md">S3Destination</a></i>,
        "<a href="#notificationchannel" title="NotificationChannel">NotificationChannel</a>" : <i><a href="notificationchannel.md">NotificationChannel</a></i>,
        "<a href="#datasharingpreference" title="DataSharingPreference">DataSharingPreference</a>" : <i><a href="datasharingpreference.md">DataSharingPreference</a></i>,
        "<a href="#polygonregionsofinterest" title="PolygonRegionsOfInterest">PolygonRegionsOfInterest</a>" : <i>[ [ <a href="point.md">Point</a>, ... ], ... ]</i>,
        "<a href="#boundingboxregionsofinterest" title="BoundingBoxRegionsOfInterest">BoundingBoxRegionsOfInterest</a>" : <i>[ <a href="boundingbox.md">BoundingBox</a>, ... ]</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::Rekognition::StreamProcessor
Properties:
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>: <i>String</i>
    <a href="#rolearn" title="RoleArn">RoleArn</a>: <i>String</i>
    <a href="#kinesisvideostream" title="KinesisVideoStream">KinesisVideoStream</a>: <i><a href="kinesisvideostream.md">KinesisVideoStream</a></i>
    <a href="#facesearchsettings" title="FaceSearchSettings">FaceSearchSettings</a>: <i><a href="facesearchsettings.md">FaceSearchSettings</a></i>
    <a href="#connectedhomesettings" title="ConnectedHomeSettings">ConnectedHomeSettings</a>: <i><a href="connectedhomesettings.md">ConnectedHomeSettings</a></i>
    <a href="#kinesisdatastream" title="KinesisDataStream">KinesisDataStream</a>: <i><a href="kinesisdatastream.md">KinesisDataStream</a></i>
    <a href="#s3destination" title="S3Destination">S3Destination</a>: <i><a href="s3destination.md">S3Destination</a></i>
    <a href="#notificationchannel" title="NotificationChannel">NotificationChannel</a>: <i><a href="notificationchannel.md">NotificationChannel</a></i>
    <a href="#datasharingpreference" title="DataSharingPreference">DataSharingPreference</a>: <i><a href="datasharingpreference.md">DataSharingPreference</a></i>
    <a href="#polygonregionsofinterest" title="PolygonRegionsOfInterest">PolygonRegionsOfInterest</a>: <i>
      -
      - <a href="point.md">Point</a></i>
    <a href="#boundingboxregionsofinterest" title="BoundingBoxRegionsOfInterest">BoundingBoxRegionsOfInterest</a>: <i>
      - <a href="boundingbox.md">BoundingBox</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### Name

Name of the stream processor. It's an identifier you assign to the stream processor. You can use it to manage the stream processor.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>128</code>

_Pattern_: <code>[a-zA-Z0-9_.\-]+</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### KmsKeyId

The KMS key that is used by Rekognition to encrypt any intermediate customer metadata and store in the customer's S3 bucket.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### RoleArn

ARN of the IAM role that allows access to the stream processor, and provides Rekognition read permissions for KVS stream and write permissions to S3 bucket and SNS topic.

_Required_: Yes

_Type_: String

_Maximum_: <code>2048</code>

_Pattern_: <code>arn:aws(-[\w]+)*:iam::[0-9]{12}:role/.*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### KinesisVideoStream

The Kinesis Video Stream that streams the source video.

_Required_: Yes

_Type_: <a href="kinesisvideostream.md">KinesisVideoStream</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### FaceSearchSettings

Face search settings to use on a streaming video. Note that either FaceSearchSettings or ConnectedHomeSettings should be set. Not both

_Required_: No

_Type_: <a href="facesearchsettings.md">FaceSearchSettings</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### ConnectedHomeSettings

Connected home settings to use on a streaming video. Note that either ConnectedHomeSettings or FaceSearchSettings should be set. Not both

_Required_: No

_Type_: <a href="connectedhomesettings.md">ConnectedHomeSettings</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### KinesisDataStream

The Amazon Kinesis Data Stream stream to which the Amazon Rekognition stream processor streams the analysis results, as part of face search feature.

_Required_: No

_Type_: <a href="kinesisdatastream.md">KinesisDataStream</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### S3Destination

The S3 location in customer's account where inference output & artifacts are stored, as part of connected home feature.

_Required_: No

_Type_: <a href="s3destination.md">S3Destination</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### NotificationChannel

The ARN of the SNS notification channel where events of interests are published, as part of connected home feature.

_Required_: No

_Type_: <a href="notificationchannel.md">NotificationChannel</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### DataSharingPreference

Indicates whether Rekognition is allowed to store the video stream data for model-training.

_Required_: No

_Type_: <a href="datasharingpreference.md">DataSharingPreference</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### PolygonRegionsOfInterest

The PolygonRegionsOfInterest specifies a set of polygon areas of interest in the video frames to analyze, as part of connected home feature. Each polygon is in turn, an ordered list of Point

_Required_: No

_Type_: List of List of <a href="point.md">Point</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### BoundingBoxRegionsOfInterest

The BoundingBoxRegionsOfInterest specifies an array of bounding boxes of interest in the video frames to analyze, as part of connected home feature. If an object is partially in a region of interest, Rekognition will tag it as detected if the overlap of the object with the region-of-interest is greater than 20%.

_Required_: No

_Type_: List of <a href="boundingbox.md">BoundingBox</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

An array of key-value pairs to apply to this resource.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Name.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The ARN of the stream processor

#### Status

Current status of the stream processor.

#### StatusMessage

Detailed status message about the stream processor.
