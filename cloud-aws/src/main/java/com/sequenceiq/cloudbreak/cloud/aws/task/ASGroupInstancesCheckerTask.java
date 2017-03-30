package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(ASGroupInstancesCheckerTask.NAME)
@Scope(value = "prototype")
public class ASGroupInstancesCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "aSGroupInstancesCheckerTask";

    private static final int MAX_INSTANCE_ID_SIZE = 100;

    private static final int INSTANCE_RUNNING = 16;

    private static final int COMPLETED = 100;

    private static final String CANCELLED = "Cancelled";

    private static final String WAIT_FOR_SPOT_INSTANCES_STATUS_CODE = "WaitingForSpotInstanceId";

    private static final String SPOT_ID_PATTERN = "sir-[a-z0-9]{8}";

    private static final String LOW_SPOT_PRICE_STATUS_CODE = "price-too-low";

    private static final Logger LOGGER = LoggerFactory.getLogger(ASGroupInstancesCheckerTask.class);

    private String groupName;

    private Integer requiredInstances;

    private AwsClient awsClient;

    private CloudFormationStackUtil cloudFormationStackUtil;

    public ASGroupInstancesCheckerTask(AuthenticatedContext authenticatedContext, String groupName, Integer requiredInstances, AwsClient awsClient,
            CloudFormationStackUtil cloudFormationStackUtil) {
        super(authenticatedContext, true);
        this.groupName = groupName;
        this.requiredInstances = requiredInstances;
        this.awsClient = awsClient;
        this.cloudFormationStackUtil = cloudFormationStackUtil;
    }

    @Override
    public Boolean call() {
        LOGGER.info("Checking status of instances in group '{}'", groupName);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getLocation().getRegion().value());
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(getAuthenticatedContext(), amazonEC2Client, groupName);
/*        if (instanceIds.size() < requiredInstances) {
            LOGGER.debug("Instances in AS group: {}, needed: {}", instanceIds.size(), requiredInstances);
            List<Activity> activities = getAutoScalingActivities();
            if (latestActivity.isPresent()) {
                checkForSpotRequest(latestActivity.get(), amazonEC2Client);
                activities = activities.stream().filter(activity -> activity.getStartTime().after(latestActivity.get().getStartTime()))
                        .collect(Collectors.toList());
            }
            for (Activity activity : activities) {
                if (activity.getProgress().equals(COMPLETED) && CANCELLED.equals(activity.getStatusCode())) {
                    throw new CloudConnectorException(activity.getStatusMessage());
                }
            }
            return false;
        }*/
        List<DescribeInstanceStatusResult> describeInstanceStatusResultList = new ArrayList<>();

        List<List<String>> partitionedInstanceIdsList = Lists.partition(instanceIds, MAX_INSTANCE_ID_SIZE);

        for (List<String> partitionedInstanceIds : partitionedInstanceIdsList) {
            DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest().withInstanceIds(partitionedInstanceIds);
            DescribeInstanceStatusResult describeResult = amazonEC2Client.describeInstanceStatus(describeInstanceStatusRequest);
            describeInstanceStatusResultList.add(describeResult);
        }

        List<InstanceStatus> instanceStatusList = describeInstanceStatusResultList.stream()
                .flatMap(describeInstanceStatusResult -> describeInstanceStatusResult.getInstanceStatuses().stream())
                .collect(Collectors.toList());

        if (instanceStatusList.size() < requiredInstances) {
            LOGGER.debug("Instances up: {}, needed: {}", instanceStatusList.size(), requiredInstances);
            return false;
        }

        for (InstanceStatus status : instanceStatusList) {
            if (INSTANCE_RUNNING != status.getInstanceState().getCode()) {
                LOGGER.debug("Instances are up but not all of them are in running state.");
                return false;
            }
        }
        return true;
    }

    private void checkForSpotRequest(Activity activity, AmazonEC2Client amazonEC2Client) {
        if (WAIT_FOR_SPOT_INSTANCES_STATUS_CODE.equals(activity.getStatusCode())) {
            Pattern pattern = Pattern.compile(SPOT_ID_PATTERN);
            Matcher matcher = pattern.matcher(activity.getStatusMessage());
            if (matcher.find()) {
                String spotId = matcher.group(0);
                DescribeSpotInstanceRequestsResult spotResult = amazonEC2Client.describeSpotInstanceRequests(
                        new DescribeSpotInstanceRequestsRequest().withSpotInstanceRequestIds(spotId));
                Optional<SpotInstanceRequest> request = spotResult.getSpotInstanceRequests().stream().findFirst();
                if (request.isPresent()) {
                    if (LOW_SPOT_PRICE_STATUS_CODE.equals(request.get().getStatus().getCode())) {
                        throw new CloudConnectorException(request.get().getStatus().getMessage());
                    }
                }

            }
        }
    }
}
