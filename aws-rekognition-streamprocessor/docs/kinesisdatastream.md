# AWS::Rekognition::StreamProcessor KinesisDataStream

The Amazon Kinesis Data Stream stream to which the Amazon Rekognition stream processor streams the analysis results, as part of face search feature.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#arn" title="Arn">Arn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#arn" title="Arn">Arn</a>: <i>String</i>
</pre>

## Properties

#### Arn

ARN of the Kinesis Data Stream stream.

_Required_: Yes

_Type_: String

_Maximum_: <code>2048</code>

_Pattern_: <code>(^arn:([a-z\d-]+):kinesis:([a-z\d-]+):\d{12}:.+$)</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
