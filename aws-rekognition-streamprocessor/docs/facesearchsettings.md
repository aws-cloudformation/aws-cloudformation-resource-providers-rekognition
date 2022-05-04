# AWS::Rekognition::StreamProcessor FaceSearchSettings

Face search settings to use on a streaming video. Note that either FaceSearchSettings or ConnectedHomeSettings should be set. Not both

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#collectionid" title="CollectionId">CollectionId</a>" : <i>String</i>,
    "<a href="#facematchthreshold" title="FaceMatchThreshold">FaceMatchThreshold</a>" : <i>Double</i>
}
</pre>

### YAML

<pre>
<a href="#collectionid" title="CollectionId">CollectionId</a>: <i>String</i>
<a href="#facematchthreshold" title="FaceMatchThreshold">FaceMatchThreshold</a>: <i>Double</i>
</pre>

## Properties

#### CollectionId

The ID of a collection that contains faces that you want to search for.

_Required_: Yes

_Type_: String

_Maximum_: <code>255</code>

_Pattern_: <code>\A[a-zA-Z0-9_\.\-]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### FaceMatchThreshold

Minimum face match confidence score percentage that must be met to return a result for a recognized face. The default is 80. 0 is the lowest confidence. 100 is the highest confidence. Values between 0 and 100 are accepted.

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
