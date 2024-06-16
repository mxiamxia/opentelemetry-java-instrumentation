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
import com.amazonaws.services.kinesis.model.DescribeStreamConsumerRequest
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
import org.slf4j.LoggerFactory
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

import java.time.Duration

class AwsConnector {

  private LocalStackContainer localstack

  private AmazonSQSAsyncClient sqsClient
  private AmazonS3Client s3Client
  private AmazonSNSAsyncClient snsClient
  private AmazonKinesisClient kinesisClient

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

  def describeStreamConsumer(String consumerARN) {
    println "Describe consumer ${consumerARN}"
    DescribeStreamConsumerRequest describeStreamConsumerRequest = new DescribeStreamConsumerRequest()
      .withConsumerARN(consumerARN)
    return kinesisClient.describeStreamConsumer(describeStreamConsumerRequest).getConsumerDescription()
  }

  def disconnect() {
    if (localstack != null) {
      localstack.stop()
    }
  }
}
