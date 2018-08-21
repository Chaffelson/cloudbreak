package com.sequenceiq.cloudbreak.cloud.k8s;

import static com.sequenceiq.cloudbreak.cloud.k8s.client.K8sClient.appsV1beta1Api;
import static com.sequenceiq.cloudbreak.cloud.k8s.client.K8sClient.appsV1beta2Api;
import static com.sequenceiq.cloudbreak.cloud.k8s.client.K8sClient.coreV1Api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.k8s.client.K8sClient;
import com.sequenceiq.cloudbreak.cloud.k8s.client.model.core.K8sComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1beta1Api;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.AppsV1beta1Deployment;
import io.kubernetes.client.models.AppsV1beta1DeploymentList;
import io.kubernetes.client.models.AppsV1beta1DeploymentSpec;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapList;
import io.kubernetes.client.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1SecurityContext;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1ServiceSpec;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.models.V1beta2ReplicaSet;
import io.kubernetes.client.models.V1beta2ReplicaSetList;

public class K8sApiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(K8sApiUtils.class);

    private static final int SSH_PORT = 22;

    private static final int NGNIX_PORT = 9443;

    private static final String CB_CONFIG_NAME = "cb-config";

    private static final String DEFAULT_NAMESPACE = "default";

    private static final String HWX_DPS_CLUSTER_NAME = "hwx-dps-cluster-name";

    private static final String HWX_DPS_CLUSTER_GROUP = "hwx-dps-cluster-group";

    private static final int AMBARI_PORT = 443;

    private static final int DEFAULT_MODE = 0744;

    private static final int API_RETRY_INTERVAL = 2000;

    private static final int MAX_RETRY_ATTEMPTS = 60;

    private K8sApiUtils() {
    }

    public static Map<String, Map<String, K8sServicePodContainer>> collectContainersByGroup(String hdpClusterName)
            throws ApiException, InterruptedException, CloudbreakException {
        Map<String, Map<String, K8sServicePodContainer>> result = new HashMap<>();

        V1ServiceList services = coreV1Api().listServiceForAllNamespaces(null, null, true,
                getClusterLabelSelector(hdpClusterName),
                0, null, null, 0, false);

        int numAttempts = MAX_RETRY_ATTEMPTS;
        final int totalNumberOfServices = services.getItems().size();
        boolean done = false;
        while (!done && numAttempts > 0) {
            for (V1Service item : services.getItems()) {
                if (hasValidPublicIp(item)) {
                    String publicIp = item.getStatus().getLoadBalancer().getIngress().get(0).getIp();
                    if (publicIp != null && !"".equals(publicIp)) {
                        String group = item.getMetadata().getLabels().get(HWX_DPS_CLUSTER_GROUP);
                        Map<String, K8sServicePodContainer> containersForGroup = result.get(group);
                        if (containersForGroup == null) {
                            containersForGroup = new HashMap<>();
                            result.put(group, containersForGroup);
                        }
                        populateContainerMetadata(api, item, publicIp, group, containersForGroup);
                    }
                }
            }
            int totalSoFar = getInstanceSize(result);
            done = totalNumberOfServices == totalSoFar;
            Thread.sleep(API_RETRY_INTERVAL);
            numAttempts--;
            services = coreV1Api().listServiceForAllNamespaces(null, null, true,
                    getClusterLabelSelector(hdpClusterName),
                    0, null, null, 0, false);
        }
        if (getInstanceSize(result) != totalNumberOfServices) {
            throw new CloudbreakException("Could not gather metadata");
        }
        return result;
    }

    private static void populateContainerMetadata(CoreV1Api api, V1Service item, String publicIp,
            String group, Map<String, K8sServicePodContainer> containersForGroup) throws ApiException {
        if (!containersForGroup.containsKey(publicIp)) {
            String appName = item.getSpec().getSelector().get("app");
            V1PodList podList = api.listNamespacedPod(DEFAULT_NAMESPACE, null, null, null, true,
                    "app=" + appName, 0, null, 0, false);
            String privateIp = podList.getItems().get(0).getStatus().getPodIP();
            if (privateIp != null && !"".equals(privateIp)) {
                LOGGER.info("Adding entry for " + group + " IP " + publicIp);
                containersForGroup.put(publicIp, new K8sServicePodContainer(privateIp, publicIp));
            }
        }
    }

    private static boolean hasValidPublicIp(V1Service item) {
        return item.getStatus() != null
                && item.getStatus().getLoadBalancer() != null
                && item.getStatus().getLoadBalancer().getIngress() != null
                && item.getStatus().getLoadBalancer().getIngress().size() > 0
                && item.getStatus().getLoadBalancer().getIngress().get(0) != null
                && item.getStatus().getLoadBalancer().getIngress().get(0).getIp() != null;
    }

    private static int getInstanceSize(Map<String, Map<String, K8sServicePodContainer>> result) {
        int totalSoFar = 0;
        for (Map.Entry<String, Map<String, K8sServicePodContainer>> e : result.entrySet()) {
            totalSoFar += e.getValue().size();
        }
        return totalSoFar;
    }

    public static void createK8sApp(String hdpClusterName, K8sComponent component)
            throws Exception {
        String configPropsString = writeParameters(component.getConfiguration().getProperties());
        createK8sArtifactsForGroup(coreV1Api(), appsV1beta1Api(), configPropsString, hdpClusterName, component.getName(), component);
    }

    public static void deleteK8sApp(String hdpClusterName) throws Exception {
        deleteK8sArtifacts(coreV1Api(), appsV1beta1Api(), appsV1beta2Api(), hdpClusterName);
    }

    private static String writeParameters(Map<String, String> parameters) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> e : parameters.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static void createK8sArtifactsForGroup(CoreV1Api api, AppsV1beta1Api appsV1beta1Api, String configPropsString,
            String hdpClusterName, String group, K8sComponent component) throws ApiException {
        String configName = CB_CONFIG_NAME + "-" + hdpClusterName + "-" + group;
        createConfigMap(api, hdpClusterName, configName, configPropsString);
        for (int i = 0; i < component.getNumberOfContainers(); i++) {
            String appName = hdpClusterName + "-" + group + "-" + i;
            createDeployment(appsV1beta1Api, hdpClusterName, group, configName, appName, IMAGE_NAME, component);
            createService(api, hdpClusterName, group, appName);
        }
    }

    private static void deleteK8sArtifacts(CoreV1Api api, AppsV1beta1Api appsV1beta1Api,
            AppsV1beta2Api appsV1beta2Api, String hdpClusterName) throws ApiException {

        //the API does not do a "cascaded" delete, so delete all the artifacts explicitly
        //first the services, then the deployments, then the replica sets, then the pods
        V1ServiceList services = api.listServiceForAllNamespaces(null, null, true,
                getClusterLabelSelector(hdpClusterName),
                0, null, null, 0, false);

        for (V1Service item : services.getItems()) {
            LOGGER.info("Deleting Service " + item.getMetadata().getName());
            try {
                api.deleteNamespacedService(item.getMetadata().getName(), DEFAULT_NAMESPACE, null);
            } catch (Exception ex) {
                //known issue with the k8s java api apparently
            }
        }

        AppsV1beta1DeploymentList deployments = appsV1beta1Api.listDeploymentForAllNamespaces(null, null,
                true, getClusterLabelSelector(hdpClusterName), 0, null, null, 0, false);

        for (AppsV1beta1Deployment item : deployments.getItems()) {
            LOGGER.info("Deleting Deployment " + item.getMetadata().getName());
            try {
                V1DeleteOptions body = new V1DeleteOptions();
                body.setGracePeriodSeconds(0L);
                appsV1beta1Api.deleteNamespacedDeployment(item.getMetadata().getName(), DEFAULT_NAMESPACE,
                        body, null, 0, false, null);
            } catch (Exception ex) {
                //known issue with the k8s java api apparently, they have issues parsing the result json
                //but the object has already been deleted
            }
        }

        V1beta2ReplicaSetList v1beta2ReplicaSetList = appsV1beta2Api.listNamespacedReplicaSet(DEFAULT_NAMESPACE, null,
                null, null, true,
                getClusterLabelSelector(hdpClusterName), 0, null, 0, false);
        for (V1beta2ReplicaSet rs : v1beta2ReplicaSetList.getItems()) {
            try {
                appsV1beta2Api.deleteNamespacedReplicaSet(
                        rs.getMetadata().getName(), DEFAULT_NAMESPACE, new V1DeleteOptions(), null,
                        0, false, null);

            } catch (Exception ex) {
                //known issue with the k8s java api apparently, they have issues parsing the result json
                //but the object has already been deleted            }
            }
        }

        V1PodList pods = api.listNamespacedPod(DEFAULT_NAMESPACE, null, null, null, true,
                getClusterLabelSelector(hdpClusterName), 0, null, 0, false);
        for (V1Pod pod : pods.getItems()) {
            try {
                api.deleteNamespacedPod(pod.getMetadata().getName(), DEFAULT_NAMESPACE, new V1DeleteOptions(),
                        null, 0, false, null);
            } catch (Exception ex) {
                //known issue with the k8s java api apparently, they have issues parsing the result json
                //but the object has already been deleted            }
            }
        }

        V1ConfigMapList configMapList = api.listNamespacedConfigMap(
                DEFAULT_NAMESPACE, null, null, null, true,
                getClusterLabelSelector(hdpClusterName),
                0, null, 0, false);
        for (V1ConfigMap item : configMapList.getItems()) {
            LOGGER.info("Deleting ConfigMap " + item.getMetadata().getName());
            api.deleteNamespacedConfigMap(item.getMetadata().getName(), DEFAULT_NAMESPACE, new V1DeleteOptions(), null,
                    0, false, null);
        }
    }

    private static String getClusterLabelSelector(String hdpClusterName) {
        return HWX_DPS_CLUSTER_NAME + "=" + hdpClusterName;
    }

    private static void createService(CoreV1Api api, String hdpClusterName, String group, String appName) throws ApiException {

        V1Service serviceBody = new V1Service();

        V1ObjectMeta meta = new V1ObjectMeta();
        serviceBody.setMetadata(meta);

        meta.setName(appName);
        Map<String, String> labels = new HashMap<>();
        labels.put(HWX_DPS_CLUSTER_NAME, hdpClusterName);
        labels.put(HWX_DPS_CLUSTER_GROUP, group);
        meta.setLabels(labels);

        V1ServiceSpec serviceSpec = new V1ServiceSpec();
        serviceSpec.setType("LoadBalancer");
        Map<String, String> selector = new HashMap<>();
        serviceSpec.setSelector(selector);

        selector.put("app", appName);

        List<V1ServicePort> ports = new ArrayList<>();
        serviceSpec.setPorts(ports);

        V1ServicePort sshPort = new V1ServicePort();
        ports.add(sshPort);

        sshPort.setPort(SSH_PORT);
        sshPort.setName("ssh");
        sshPort.setTargetPort(new IntOrString(SSH_PORT));
        sshPort.setProtocol("TCP");

        V1ServicePort ngnixPort = new V1ServicePort();
        ports.add(ngnixPort);

        ngnixPort.setName("ngnix");
        ngnixPort.setPort(NGNIX_PORT);
        ngnixPort.setTargetPort(new IntOrString(NGNIX_PORT));
        ngnixPort.setProtocol("TCP");

        V1ServicePort saltPort = new V1ServicePort();
        ports.add(saltPort);

        saltPort.setName("ambari");
        saltPort.setPort(AMBARI_PORT);
        saltPort.setTargetPort(new IntOrString(AMBARI_PORT));
        saltPort.setProtocol("TCP");

        serviceBody.setSpec(serviceSpec);

        api.createNamespacedService(DEFAULT_NAMESPACE, serviceBody, null);

        LOGGER.info("Created Service " + appName);
    }

    private static void createConfigMap(CoreV1Api api, String hdpClusterName, String cbConfigName, String configPropsString) throws ApiException {
        K8sClient.createConfigMap(coreV1Api(), "default", cbConfigName, configPropsString);
    }

    private static void createDeployment(AppsV1beta1Api appsV1beta1Api,
            String hdpClusterName, String group, String configName, String appName,
            String imageName, K8sComponent component) throws ApiException {
        AppsV1beta1Deployment deployment = new AppsV1beta1Deployment();

        deployment.setKind("Deployment");
        deployment.setApiVersion("apps/v1beta1");
        V1ObjectMeta meta = new V1ObjectMeta();
        deployment.setMetadata(meta);

        meta.setName(appName);
        Map<String, String> labels = new HashMap<>();
        labels.put("app", appName);
        labels.put(HWX_DPS_CLUSTER_NAME, hdpClusterName);
        labels.put(HWX_DPS_CLUSTER_GROUP, group);
        meta.setLabels(labels);

        AppsV1beta1DeploymentSpec spec = new AppsV1beta1DeploymentSpec();
        deployment.setSpec(spec);

        spec.setReplicas(1);

        V1LabelSelector selector = new V1LabelSelector();
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", appName);
        matchLabels.put(HWX_DPS_CLUSTER_NAME, hdpClusterName);
        selector.matchLabels(matchLabels);
        spec.setSelector(selector);

        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();
        spec.setTemplate(podTemplateSpec);

        V1ObjectMeta podMeta = new V1ObjectMeta();
        podMeta.setLabels(matchLabels);
        podTemplateSpec.setMetadata(podMeta);

        V1PodSpec podSpec = new V1PodSpec();
        podTemplateSpec.setSpec(podSpec);

        List<V1Container> containers = new ArrayList<>();
        podSpec.setContainers(containers);

        V1Container container = new V1Container();
        containers.add(container);

        container.setName("hdp-container");
        container.setImage(imageName);

        List<String> cmd = new ArrayList<>();
        cmd.add("/sbin/init");
        container.setCommand(cmd);

        V1SecurityContext secContext = new V1SecurityContext();
        secContext.setPrivileged(true);
        container.setSecurityContext(secContext);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        container.setVolumeMounts(volumeMounts);

        V1VolumeMount configVolumeMount = new V1VolumeMount();
        configVolumeMount.setName("config-volume");
        configVolumeMount.setMountPath("/configmap");
        volumeMounts.add(configVolumeMount);

        List<V1ContainerPort> ports = new ArrayList<>();
        container.setPorts(ports);

        V1ContainerPort sshPort = new V1ContainerPort();
        sshPort.setContainerPort(SSH_PORT);
        sshPort.setName("ssh");
        ports.add(sshPort);

        V1ContainerPort ngnixPort = new V1ContainerPort();
        ngnixPort.setName("ngnix");
        ngnixPort.setContainerPort(NGNIX_PORT);
        ports.add(ngnixPort);

        V1ContainerPort saltPort = new V1ContainerPort();
        saltPort.setName("ambari");
        saltPort.setContainerPort(AMBARI_PORT);
        ports.add(saltPort);

        V1ResourceRequirements resources = new V1ResourceRequirements();
        resources.putLimitsItem("memory", Quantity.fromString(component.getResource().getMemory() + "Mi"));
        resources.putLimitsItem("cpu", Quantity.fromString(component.getResource().getCpus() + ""));
        container.setResources(resources);
        List<V1Volume> volumes = new ArrayList<>();
        podSpec.setVolumes(volumes);

        V1Volume configVolume = new V1Volume();
        configVolume.setName("config-volume");
        V1ConfigMapVolumeSource configMap = new V1ConfigMapVolumeSource();
        configMap.setDefaultMode(DEFAULT_MODE);
        configMap.setName(configName);

        configVolume.setConfigMap(configMap);
        volumes.add(configVolume);

        appsV1beta1Api.createNamespacedDeployment(DEFAULT_NAMESPACE, deployment, null);

        LOGGER.info("Created Deployment " + appName);
    }
}
