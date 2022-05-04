# AWS::Rekognition::StreamProcessor ConnectedHomeSettings

Connected home settings to use on a streaming video. Note that either ConnectedHomeSettings or FaceSearchSettings should be set. Not both

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#labels" title="Labels">Labels</a>" : <i>[ String, ... ]</i>,
    "<a href="#minconfidence" title="MinConfidence">MinConfidence</a>" : <i>Double</i>
}
</pre>

### YAML

<pre>
<a href="#labels" title="Labels">Labels</a>: <i>
      - String</i>
<a href="#minconfidence" title="MinConfidence">MinConfidence</a>: <i>Double</i>
</pre>

## Properties

#### Labels

List of labels that need to be detected in the video stream. Current supported values are PERSON, PET, PACKAGE, ALL.

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MinConfidence

Minimum object class match confidence score that must be met to return a result for a recognized object.

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

