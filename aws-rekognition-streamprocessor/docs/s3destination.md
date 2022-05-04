# AWS::Rekognition::StreamProcessor S3Destination

The S3 location in customer's account where inference output & artifacts are stored, as part of connected home feature.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#bucketname" title="BucketName">BucketName</a>" : <i>String</i>,
    "<a href="#objectkeyprefix" title="ObjectKeyPrefix">ObjectKeyPrefix</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#bucketname" title="BucketName">BucketName</a>: <i>String</i>
<a href="#objectkeyprefix" title="ObjectKeyPrefix">ObjectKeyPrefix</a>: <i>String</i>
</pre>

## Properties

#### BucketName

Name of the S3 bucket.

_Required_: Yes

_Type_: String

_Maximum_: <code>63</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ObjectKeyPrefix

The object key prefix path where the results will be stored. Default is no prefix path

_Required_: No

_Type_: String

_Maximum_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

