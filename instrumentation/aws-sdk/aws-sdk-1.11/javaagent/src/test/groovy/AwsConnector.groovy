/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.CreateStreamRequest
import com.amazonaws.services.kinesis.model.RegisterStreamConsumerRequest
import com.amazonaws.services.kinesis.model.RegisterStreamConsumerResult
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.BucketNotificationConfiguration
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.QueueConfiguration
import com.amazonaws.services.s3.model.S3Event
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.model.SetBucketNotificationConfigurationRequest
import com.amazonaws.services.s3.model.TopicConfiguration
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sns.model.CreateTopicResult
import com.amazonaws.services.sns.model.SetTopicAttributesRequest
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest
import com.amazonaws.services.secretsmanager.model.CreateSecretResult
import com.amazonaws.services.secretsmanager.model.DescribeSecretRequest
import com.amazonaws.services.secretsmanager.model.DescribeSecretResult
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest
import com.amazonaws.services.identitymanagement.model.CreateRoleResult
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClient
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.CreateFunctionRequest
import com.amazonaws.services.lambda.model.CreateFunctionResult
import com.amazonaws.services.lambda.model.CreateEventSourceMappingRequest
import com.amazonaws.services.lambda.model.CreateEventSourceMappingResult
import com.amazonaws.services.lambda.model.GetFunctionRequest
import com.amazonaws.services.lambda.model.GetFunctionResult
import com.amazonaws.services.lambda.model.GetEventSourceMappingRequest
import com.amazonaws.services.lambda.model.GetEventSourceMappingResult
import com.amazonaws.services.lambda.model.FunctionCode
import com.amazonaws.services.lambda.model.Runtime

import org.slf4j.LoggerFactory
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AwsConnector {

  private LocalStackContainer localstack

  private AmazonSQSAsyncClient sqsClient
  private AmazonS3Client s3Client
  private AmazonSNSAsyncClient snsClient
  private AmazonKinesisClient kinesisClient
  private AWSSecretsManagerClient secretsManagerClient
  private AWSStepFunctionsClient stepFunctionsClient
  private AmazonIdentityManagementClient iamClient
  private AWSLambdaClient lambdaClient

  static localstack() {
    AwsConnector awsConnector = new AwsConnector()

    awsConnector.localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.0.2"))
      .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.SNS, LocalStackContainer.Service.S3)
      .withEnv("DEBUG", "1")
      .withEnv("SQS_PROVIDER", "elasticmq")
      .withStartupTimeout(Duration.ofMinutes(2))
    awsConnector.localstack.start()
    awsConnector.localstack.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("test")))

    AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsConnector.localstack .getAccessKey(), awsConnector.localstack.getSecretKey()))

    awsConnector.sqsClient = AmazonSQSAsyncClient.asyncBuilder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.SQS))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.s3Client = AmazonS3Client.builder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.S3))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.snsClient = AmazonSNSAsyncClient.asyncBuilder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.SNS))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.kinesisClient = AmazonKinesisClient.builder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.KINESIS))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.secretsManagerClient = AWSSecretsManagerClient.builder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.SECRETSMANAGER))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.stepFunctionsClient = AWSStepFunctionsClient.builder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.STEPFUNCTIONS))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.iamClient = AmazonIdentityManagementClient.builder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.IAM))
      .withCredentials(credentialsProvider)
      .build()

    awsConnector.lambdaClient = AWSLambdaClient.builder()
      .withEndpointConfiguration(getEndpointConfiguration(awsConnector.localstack, LocalStackContainer.Service.LAMBDA))
      .withCredentials(credentialsProvider)
      .build()

    return awsConnector
  }

  static AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(LocalStackContainer localstack, LocalStackContainer.Service service) {
    return new AwsClientBuilder.EndpointConfiguration(localstack.getEndpointOverride(service).toString(), localstack.getRegion())
  }

  static liveAws() {
    AwsConnector awsConnector = new AwsConnector()

    awsConnector.sqsClient = AmazonSQSAsyncClient.asyncBuilder()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.s3Client = AmazonS3Client.builder()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.snsClient = AmazonSNSAsyncClient.asyncBuilder()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.kinesisClient = AmazonKinesisClient.builder()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.secretsManagerClient = AWSSecretsManagerClient.standard()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.stepFunctionsClient = AWSStepFunctionsClient.standard()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.iamClient = AmazonIdentityManagementClient.standard()
      .withRegion(Regions.US_EAST_1)
      .build()

    awsConnector.lambdaClient = AWSLambdaClient.standard()
      .withRegion(Regions.US_EAST_1)
      .build()

    return awsConnector
  }

  def createQueue(String queueName) {
    println "Create queue ${queueName}"
    return sqsClient.createQueue(queueName).getQueueUrl()
  }

  def getQueueArn(String queueUrl) {
    println "Get ARN for queue ${queueUrl}"
    return sqsClient.getQueueAttributes(
      new GetQueueAttributesRequest(queueUrl)
        .withAttributeNames("QueueArn")).getAttributes()
      .get("QueueArn")
  }

  def setTopicPublishingPolicy(String topicArn) {
    println "Set policy for topic ${topicArn}"
    snsClient.setTopicAttributes(new SetTopicAttributesRequest(topicArn, "Policy", String.format(SNS_POLICY, topicArn)))
  }

  private static final String SNS_POLICY = "{" +
    "  \"Statement\": [" +
    "    {" +
    "      \"Effect\": \"Allow\"," +
    "      \"Principal\": \"*\"," +
    "      \"Action\": \"sns:Publish\"," +
    "      \"Resource\": \"%s\"" +
    "    }]" +
    "}"

  def setQueuePublishingPolicy(String queueUrl, String queueArn) {
    println "Set policy for queue ${queueArn}"
    sqsClient.setQueueAttributes(queueUrl, Collections.singletonMap("Policy", String.format(SQS_POLICY, queueArn)))
  }

  private static final String SQS_POLICY = "{" +
    "  \"Statement\": [" +
    "    {" +
    "      \"Effect\": \"Allow\"," +
    "      \"Principal\": \"*\"," +
    "      \"Action\": \"sqs:SendMessage\"," +
    "      \"Resource\": \"%s\"" +
    "    }]" +
    "}"

  def createBucket(String bucketName) {
    println "Create bucket ${bucketName}"
    s3Client.createBucket(bucketName)
  }

  def deleteBucket(String bucketName) {
    println "Delete bucket ${bucketName}"
    ObjectListing objectListing = s3Client.listObjects(bucketName)
    Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator()
    while (objIter.hasNext()) {
      s3Client.deleteObject(bucketName, objIter.next().getKey())
    }
    s3Client.deleteBucket(bucketName)
  }

  def enableS3ToSqsNotifications(String bucketName, String sqsQueueArn) {
    println "Enable notification for bucket ${bucketName} to queue ${sqsQueueArn}"
    BucketNotificationConfiguration notificationConfiguration = new BucketNotificationConfiguration()
    notificationConfiguration.addConfiguration("sqsQueueConfig",
      new QueueConfiguration(sqsQueueArn, EnumSet.of(S3Event.ObjectCreatedByPut)))
    s3Client.setBucketNotificationConfiguration(new SetBucketNotificationConfigurationRequest(
      bucketName, notificationConfiguration))
  }

  def enableS3ToSnsNotifications(String bucketName, String snsTopicArn) {
    println "Enable notification for bucket ${bucketName} to topic ${snsTopicArn}"
    BucketNotificationConfiguration notificationConfiguration = new BucketNotificationConfiguration()
    notificationConfiguration.addConfiguration("snsTopicConfig",
      new TopicConfiguration(snsTopicArn, EnumSet.of(S3Event.ObjectCreatedByPut)))
    s3Client.setBucketNotificationConfiguration(new SetBucketNotificationConfigurationRequest(
      bucketName, notificationConfiguration))
  }

  def createTopicAndSubscribeQueue(String topicName, String queueArn) {
    println "Create topic ${topicName} and subscribe to queue ${queueArn}"
    CreateTopicResult ctr = snsClient.createTopic(topicName)
    snsClient.subscribe(ctr.getTopicArn(), "sqs", queueArn)
    return ctr.getTopicArn()
  }

  def receiveMessage(String queueUrl) {
    println "Receive message from queue ${queueUrl}"
    sqsClient.receiveMessage(new ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(20))
  }

  def purgeQueue(String queueUrl) {
    println "Purge queue ${queueUrl}"
    sqsClient.purgeQueue(new PurgeQueueRequest(queueUrl))
  }

  def putSampleData(String bucketName) {
    println "Put sample data to bucket ${bucketName}"
    s3Client.putObject(bucketName, "otelTestKey", "otelTestData")
  }

  def publishSampleNotification(String topicArn) {
    snsClient.publish(topicArn, "Hello There")
  }

  def createStream(String streamName, Integer shardCount) {
    println "Create stream ${streamName} with ${shardCount} shards"
    CreateStreamRequest createStreamRequest = new CreateStreamRequest()
      .withStreamName(streamName)
      .withShardCount(shardCount)
    kinesisClient.createStream(createStreamRequest)
    return kinesisClient.describeStream(streamName).getStreamDescription().getStreamARN()
  }

  def registerStreamConsumer(String streamARN, String consumerName) {
    println "Register consumer ${consumerName} for stream ${streamARN}"
    RegisterStreamConsumerRequest registerStreamConsumerRequest = new RegisterStreamConsumerRequest()
      .withStreamARN(streamARN)
      .withConsumerName(consumerName)
    RegisterStreamConsumerResult registerStreamConsumerResult = kinesisClient.registerStreamConsumer(registerStreamConsumerRequest)
    return registerStreamConsumerResult.getConsumer().getConsumerARN()
  }

  def createSecret(String secretName, String secretValue) {
    println "Create secret ${secretName}"
    CreateSecretRequest createSecretRequest = new CreateSecretRequest()
      .withName(secretName)
      .withSecretString(secretValue)
    CreateSecretResult createSecretResult = secretsManagerClient.createSecret(createSecretRequest)
    return createSecretResult.getARN()
  }

  def describeSecret(String secretName) {
    println "Describe secret ${secretName}"
    DescribeSecretRequest describeSecretRequest = new DescribeSecretRequest()
      .withSecretId(secretName)
    DescribeSecretResult describeSecretResult = secretsManagerClient.describeSecret(describeSecretRequest)
    return describeSecretResult
  }

  def createRole(String roleName, String policy) {
    println "Create role ${roleName}"
    CreateRoleRequest createRoleRequest = new CreateRoleRequest()
      .withRoleName(roleName)
      .withAssumeRolePolicyDocument(policy)
    CreateRoleResult createRoleResult = iamClient.createRole(createRoleRequest)
    return createRoleResult.getRole().getArn()
  }

  def attachRolePolicy(String roleName, String policyArn) {
    println "Attach policy ${policyArn} to role ${roleName}"
    AttachRolePolicyRequest attachRolePolicyRequest = new AttachRolePolicyRequest()
      .withRoleName(roleName)
      .withPolicyArn(policyArn)
    iamClient.attachRolePolicy(attachRolePolicyRequest)
  }

  def putRolePolicy(String roleName, String policyName, String policyDocument) {
    println "Put policy ${policyName} for role ${roleName}"
    PutRolePolicyRequest putRolePolicyRequest = new PutRolePolicyRequest()
      .withRoleName(roleName)
      .withPolicyName(policyName)
      .withPolicyDocument(policyDocument)
    iamClient.putRolePolicy(putRolePolicyRequest)
  }

  def createStateMachine(String stateMachineName, String stateMachineDefinition, String stateMachineRoleArn) {
    println "Create state machine ${stateMachineName}"
    CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest()
      .withName(stateMachineName)
      .withDefinition(stateMachineDefinition)
      .withRoleArn(stateMachineRoleArn)
    CreateStateMachineResult createStateMachineResult = stepFunctionsClient.createStateMachine(createStateMachineRequest)
    return createStateMachineResult.getStateMachineArn()
  }

  def describeStateMachine(String stateMachineArn) {
    println "Describe state machine ${stateMachineArn}"
    DescribeStateMachineRequest describeStateMachineRequest = new DescribeStateMachineRequest()
      .withStateMachineArn(stateMachineArn)
    DescribeStateMachineResult describeStateMachineResult = stepFunctionsClient.describeStateMachine(describeStateMachineRequest)
    return describeStateMachineResult
  }

  def createFunction(String functionName, String roleArn, String handler) {
    println "Create function ${functionName}"
    String lambdaFunctionCode = """
            def lambda_handler(event, context):
                print("Received event: " + str(event))
                return {
                    'statusCode': 200,
                    'body': 'Hello from Lambda!'
                }
            """


    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ZipOutputStream zos = new ZipOutputStream(baos)
    ZipEntry entry = new ZipEntry("lambda_function.py")
    zos.putNextEntry(entry)
    zos.write(lambdaFunctionCode.getBytes())
    zos.closeEntry()
    zos.close()
    byte[] zipFileBytes = baos.toByteArray()

    CreateFunctionRequest createFunctionRequest = new CreateFunctionRequest()
      .withFunctionName(functionName)
      .withRuntime(Runtime.Python38)
      .withRole(roleArn)
      .withHandler(handler)
      .withCode(new FunctionCode().withZipFile(ByteBuffer.wrap(zipFileBytes)))
    CreateFunctionResult createFunctionResult = lambdaClient.createFunction(createFunctionRequest)
    return createFunctionResult.getFunctionArn()
  }

  def createEventSourceMapping(String functionName, String eventSourceArn) {
    println "Create event source mapping for function ${functionName}"
    CreateEventSourceMappingRequest createEventSourceMappingRequest = new CreateEventSourceMappingRequest()
      .withFunctionName(functionName)
      .withEventSourceArn(eventSourceArn)
    CreateEventSourceMappingResult createEventSourceMappingResult = lambdaClient.createEventSourceMapping(createEventSourceMappingRequest)
    return createEventSourceMappingResult.getUUID()
  }

  def getFunction(String functionName) {
    println "Get function ${functionName}"
    GetFunctionRequest getFunctionRequest = new GetFunctionRequest()
      .withFunctionName(functionName)
    GetFunctionResult getFunctionResult = lambdaClient.getFunction(getFunctionRequest)
    return getFunctionResult.getConfiguration()
  }

  def getEventSourceMapping(String uuid) {
    println "Get event source mapping ${uuid}"
    GetEventSourceMappingRequest getEventSourceMappingRequest = new GetEventSourceMappingRequest()
      .withUUID(uuid)
    GetEventSourceMappingResult getEventSourceMappingResult = lambdaClient.getEventSourceMapping(getEventSourceMappingRequest)
    return getEventSourceMappingResult
  }

  def disconnect() {
    if (localstack != null) {
      localstack.stop()
    }
  }
}
