package com.sequenceiq.it.cloudbreak.newway.v3;

import static java.lang.Boolean.FALSE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.WaitResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class CloudbreakV3Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakV3Util.class);

    private static final int MAX_RETRY = 360;

    private static long pollingInterval = 10000L;

    private CloudbreakV3Util() {
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Value("${integrationtest.testsuite.pollingInterval:10000}")
    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public static void checkResponse(String operation, Response response) {
        if (Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            String errormsg = "Error happened during " + operation + " rest operation: status: " + response.getStatus() + ", error: "
                    + response.readEntity(String.class);
            LOGGER.error(errormsg);
            throw new RuntimeException(errormsg);
        }
    }

    public static void waitAndCheckStackStatus(CloudbreakClient cloudbreakClient, Long orgId, String stackName, String desiredStatus) {
        waitAndCheckStatuses(cloudbreakClient, orgId, stackName, Collections.singletonMap("status", desiredStatus));
    }

    public static void waitAndCheckClusterStatus(CloudbreakClient cloudbreakClient, Long orgId, String stackName, String desiredStatus) {
        waitAndCheckStatuses(cloudbreakClient, orgId, stackName, Collections.singletonMap("clusterStatus", desiredStatus));
    }

    public static void waitAndExpectClusterFailure(CloudbreakClient cloudbreakClient, Long orgId, String stackName, String desiredStatus, String keyword) {
        waitAndExpectClusterFailure(cloudbreakClient, orgId, stackName, Collections.singletonMap("clusterStatus", desiredStatus), keyword);
    }

    public static WaitResult waitForStackStatus(CloudbreakClient cloudbreakClient, Long orgId, String stackName, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, orgId, stackName, Collections.singletonMap("status", desiredStatus));
    }

    public static WaitResult waitForClusterStatus(CloudbreakClient cloudbreakClient, Long orgId, String stackName, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, orgId, stackName, Collections.singletonMap("clusterStatus", desiredStatus));
    }

    public static String getFailedStatusReason(CloudbreakClient cloudbreakClient, Long orgId, String stackName, Map<String, String> desiredStatuses,
            Collection<WaitResult> desiredWaitResult) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, orgId, stackName, desiredStatuses);
            if (!desiredWaitResult.contains(waitResult)) {
                Assert.fail("Expected status is failed, actual: " + waitResult);
            } else {
                StackV3Endpoint stackV3Endpoint = cloudbreakClient.stackV3Endpoint();
                return stackV3Endpoint.getStatusByNameInOrganization(orgId, stackName).get("statusReason").toString();
            }
        }
        return "";
    }

    public static void waitAndCheckStatuses(CloudbreakClient cloudbreakClient, Long orgId, String stackName, Map<String, String> desiredStatuses) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, orgId, stackName, desiredStatuses);
            if (waitResult == WaitResult.FAILED) {
                Assert.fail("The stack has failed");
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
        }
    }

    public static void waitAndExpectClusterFailure(CloudbreakClient cloudbreakClient, Long orgId, String stackName, Map<String, String> desiredStatuses,
            String keyword) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, orgId, stackName, desiredStatuses);
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
            StackV3Endpoint stackV3Endpoint = cloudbreakClient.stackV3Endpoint();
            Assert.assertTrue(stackV3Endpoint.getStatusByNameInOrganization(orgId, stackName).get("clusterStatusReason").toString().contains(keyword));
        }
    }

    public static WaitResult waitForHostStatusStack(StackV3Endpoint stackV3Endpoint, Long orgId, String stackName, String hostGroup, String desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean found = FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for host status {} in hostgroup {} ...", desiredStatus, hostGroup);
            sleep();
            StackResponse stackResponse = stackV3Endpoint.getByNameInOrganization(orgId, stackName, new HashSet<>());
            Set<HostGroupResponse> hostGroupResponse = stackResponse.getCluster().getHostGroups();
            for (HostGroupResponse hr : hostGroupResponse) {
                if (hr.getName().equals(hostGroup)) {
                    Set<HostMetadataResponse> hostMetadataResponses = hr.getMetadata();
                    for (HostMetadataResponse hmr : hostMetadataResponses) {
                        if (hmr.getState().equals(desiredStatus)) {
                            found = Boolean.TRUE;
                        }
                    }
                }
            }

            retryCount++;

        } while (!found && (retryCount < MAX_RETRY));

        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    public static void checkClusterFailed(StackV3Endpoint stackV3Endpoint, Long orgId, String stackName, CharSequence failMessage) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInOrganization(orgId, stackName, new HashSet<>());
        Assert.assertNotEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE);
        Assert.assertTrue(stackResponse.getCluster().getStatusReason().contains(failMessage));
    }

    public static void checkClusterAvailability(StackV3Endpoint stackV3Endpoint, String port, Long orgId, String stackName, String ambariUser,
            String ambariPassowrd, boolean checkAmbari) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInOrganization(orgId, stackName, new HashSet<>());
        checkClusterAvailability(stackResponse, port, ambariUser, ambariPassowrd, checkAmbari);
    }

    public static void checkClusterAvailabilityThroughGateway(StackV3Endpoint stackV3Endpoint, Long orgId, String stackName, String ambariUser,
            String ambariPassowrd) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInOrganization(orgId, stackName, new HashSet<>());
        checkStackStatusForClusterAvailability(stackResponse);

        String ambariServerUrl = stackResponse.getCluster().getAmbariServerUrl();
        Assert.assertNotNull(ambariServerUrl, "The Ambari URL is not available!");
        Response response = RestClientUtil.get().target(ambariServerUrl).request().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.getStatus(), "Ambari is not available!");
    }

    private static void checkClusterAvailability(StackResponse stackResponse, String port, String ambariUser, String ambariPassowrd,
            boolean checkAmbari) {
        checkStackStatusForClusterAvailability(stackResponse);

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");

        if (checkAmbari) {
            AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassowrd);
            Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");
            Assert.assertEquals(ambariClient.getClusterHosts().size(), getNodeCount(stackResponse),
                    "The number of cluster nodes in the stack differs from the number of nodes registered in ambari");
        }
    }

    private static void checkStackStatusForClusterAvailability(StackResponse stackResponse) {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE, "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
    }

    public static void checkClusterStopped(StackV3Endpoint stackV3Endpoint, String port, Long orgId, String stackName, String ambariUser,
            String ambariPassword) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInOrganization(orgId, stackName, new HashSet<>());
        checkClusterStopped(port, ambariUser, ambariPassword, stackResponse);
    }

    private static void checkClusterStopped(String port, String ambariUser, String ambariPassword, StackResponse stackResponse) {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.STOPPED, "The cluster is not stopped!");
        Assert.assertEquals(stackResponse.getStatus(), Status.STOPPED, "The stack is not stopped!");

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertFalse(isAmbariRunning(ambariClient), "The Ambari server is running in stopped state!");
    }

    public static boolean isAmbariRunning(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
            return "RUNNING".equals(ambariHealth);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getAmbariIp(StackV3Endpoint stackV3Endpoint, Long orgId, String stackName, IntegrationTestContext itContext) {
        String ambariIp = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_IP_ID);
        if (StringUtils.isEmpty(ambariIp)) {
            StackResponse stackResponse = stackV3Endpoint.getByNameInOrganization(orgId, stackName, new HashSet<>());
            ambariIp = stackResponse.getCluster().getAmbariServerIp();
            Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");
            itContext.putContextParam(CloudbreakITContextConstants.AMBARI_IP_ID, ambariIp);
        }
        return ambariIp;
    }

    public static String getAmbariIp(StackResponse stackResponse, IntegrationTestContext itContext) {
        String ambariIp = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_IP_ID);
        if (StringUtils.isEmpty(ambariIp)) {
            ambariIp = stackResponse.getCluster().getAmbariServerIp();
            Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");
            itContext.putContextParam(CloudbreakITContextConstants.AMBARI_IP_ID, ambariIp);
        }
        return ambariIp;
    }

    private static WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, Long orgId, String stackName, Map<String, String> desiredStatuses) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, String> currentStatuses = new HashMap<>();

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatuses, stackName, currentStatuses);

            sleep();
            StackV3Endpoint stackV3Endpoint = cloudbreakClient.stackV3Endpoint();
            try {
                Map<String, Object> statusResult = stackV3Endpoint.getStatusByNameInOrganization(orgId, stackName);
                for (String statusPath : desiredStatuses.keySet()) {
                    String currStatus = "DELETE_COMPLETED";
                    if (!CollectionUtils.isEmpty(statusResult)) {
                        currStatus = (String) statusResult.get(statusPath);
                    }
                    currentStatuses.put(statusPath, currStatus);
                }
            } catch (RuntimeException ignore) {
                continue;
            }

            retryCount++;
        }
        while (!checkStatuses(currentStatuses, desiredStatuses) && !checkFailedStatuses(currentStatuses) && retryCount < MAX_RETRY);

        LOGGER.info("Status(es) {} for {} are in desired status(es) {}", desiredStatuses.keySet(), stackName, currentStatuses.values());
        if (currentStatuses.values().stream().anyMatch(cs -> cs.contains("FAILED")) || checkNotExpectedDelete(currentStatuses, desiredStatuses)) {
            waitResult = WaitResult.FAILED;
        }
        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    public static WaitResult waitForEvent(CloudbreakClient cloudbreakClient, Long orgId, String stackName, String eventType,
            String eventMessage, long sinceTimeStamp) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean exitCriteria = FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for event type {} and event message contains {} ...", eventType, eventMessage);
            sleep();
            EventV3Endpoint eventEndpoint = cloudbreakClient.eventV3Endpoint();
            List<CloudbreakEventsJson> list = eventEndpoint.getCloudbreakEventsSince(orgId, sinceTimeStamp);
            for (CloudbreakEventsJson event : list) {
                if (event.getStackName().equals(stackName) && event.getEventMessage().contains(eventMessage) && event.getEventType().equals(eventType)) {
                    exitCriteria = Boolean.TRUE;
                    break;
                }
            }
            retryCount++;
        } while (!exitCriteria && retryCount < MAX_RETRY);

        LOGGER.info("Event {} for {} happened and event message contains {}", eventType, stackName, eventMessage);

        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private static boolean checkStatuses(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = true;
        for (Entry<String, String> desiredStatus : desiredStatuses.entrySet()) {
            if (!desiredStatus.getValue().equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = false;
                break;
            }
        }
        return result;
    }

    private static boolean checkFailedStatuses(Map<String, String> currentStatuses) {
        boolean result = false;
        List<String> failedStatuses = Arrays.asList("FAILED", "DELETE_COMPLETED");
        for (Entry<String, String> desiredStatus : currentStatuses.entrySet()) {
            if (failedStatuses.stream().anyMatch(fs -> desiredStatus.getValue().contains(fs))) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean checkNotExpectedDelete(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = false;
        for (Entry<String, String> desiredStatus : desiredStatuses.entrySet()) {
            if (!"DELETE_COMPLETED".equals(desiredStatus.getValue()) && "DELETE_COMPLETED".equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static void sleep() {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait", e);
        }
    }

    private static int getNodeCount(StackResponse stackResponse) {
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getGroup())) {
                nodeCount += instanceGroup.getNodeCount();
            }
        }
        return nodeCount;
    }
}
