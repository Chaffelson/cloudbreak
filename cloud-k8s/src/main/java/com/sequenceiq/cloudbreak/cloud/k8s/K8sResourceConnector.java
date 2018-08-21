package com.sequenceiq.cloudbreak.cloud.k8s;

import static com.sequenceiq.cloudbreak.common.type.ResourceType.K8S_APPLICATION;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.k8s.auth.K8sClientUtil;
import com.sequenceiq.cloudbreak.cloud.k8s.auth.K8sCredentialView;
import com.sequenceiq.cloudbreak.cloud.k8s.client.model.core.K8sComponent;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1beta1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.AppsV1beta1Deployment;
import io.kubernetes.client.models.AppsV1beta1DeploymentSpec;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1SecurityContext;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceSpec;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.authenticators.GCPAuthenticator;

@Service
public class K8sResourceConnector implements ResourceConnector<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sResourceConnector.class);

    private static final String ARTIFACT_TYPE_DOCKER = "DOCKER";

    private static final String CB_CLUSTER_NAME = "cb-cluster-name";

    private static final String CB_CLUSTER_GROUP_NAME = "cb-cluster-name";

    private static final String APP_NAME = "app";

    private static final int SSH_PORT = 22;

    private static final int NGNIX_PORT = 9443;

    private static final int AMBARI_PORT = 443;

    private static final int DEFAULT_MODE = 0744;

    @Inject
    private K8sClientUtil k8sClientUtil;

    @Inject
    private K8sResourceNameGenerator k8sResourceNameGenerator;

    @Value("${cb.k8s.defaultNameSpace}")
    private String defaultNameSpace;

    @Value("${cb.k8s.defaultLifeTime:}")
    private int defaultLifeTime;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        K8sCredentialView k8sClient = k8sClientUtil.createK8sClient(authenticatedContext);
        String clusterName = k8sResourceNameGenerator.getClusterName(authenticatedContext);

        if (!checkApplicationAlreadyCreated(k8sClient, clusterName)) {
            createApplication(authenticatedContext, k8sClient, stack);
        }

        CloudResource k8sApplication = new Builder().type(K8S_APPLICATION).name(clusterName).build();
        persistenceNotifier.notifyAllocation(k8sApplication, authenticatedContext.getCloudContext());
        return check(authenticatedContext, Collections.singletonList(k8sApplication));
    }

    public ApiClient apiClient() throws IOException {
        KubeConfig.registerAuthenticator(new GCPAuthenticator());
        return Config.defaultClient();
    }

    public CoreV1Api coreV1Api() throws IOException {
        return new CoreV1Api();
    }

    private void createApplication(AuthenticatedContext ac, K8sCredentialView k8sCredential, CloudStack stack) throws IOException, ApiException {
        createApiClient();
        createConfigMap(ac, stack);
        for (Group group : stack.getGroups()) {
            int i = 0;
            for (CloudInstance cloudInstance : group.getInstances()) {
                createInstance(ac, stack, group, i);
                i++;
            }
        }
    }

    public Map<String, String> getLabels(String clusterName, Optional<String> groupName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(APP_NAME, clusterName);
        labels.put(CB_CLUSTER_NAME, clusterName);
        if (groupName.isPresent()) {
            labels.put(CB_CLUSTER_GROUP_NAME, groupName.orElse(null));
        }
        return labels;
    }

    private void createInstance(AuthenticatedContext ac, CloudStack stack, Group group, int i) throws IOException, ApiException {
        String instanceName = k8sResourceNameGenerator.getInstanceContainerName(ac, group, i);
        String groupName = k8sResourceNameGenerator.getGroupName(group);
        String clusterName = k8sResourceNameGenerator.getClusterName(ac);
        appsV1beta1Api().createNamespacedDeployment(defaultNameSpace, appsV1beta1Deployment(stack, group, instanceName, clusterName), null);
        coreV1Api().createNamespacedService(defaultNameSpace, v1Service(clusterName), null);
    }

    private V1Service v1Service(String clusterName) {
        V1Service serviceBody = new V1Service();

        serviceBody.setMetadata(v1ObjectMeta(clusterName));
        serviceBody.setSpec(v1ServiceSpec());
        return serviceBody;
    }

    private V1ServiceSpec v1ServiceSpec(String clusterName) {
        V1ServiceSpec v1ServiceSpec = new V1ServiceSpec();
        v1ServiceSpec.setType("LoadBalancer");
        v1ServiceSpec.setSelector(selectorMap(clusterName));
        return v1ServiceSpec;
    }

    private AppsV1beta1Api appsV1beta1Api() throws IOException {
        return new AppsV1beta1Api(apiClient());
    }

    private AppsV1beta1Deployment appsV1beta1Deployment(CloudStack stack, Group group, String instanceName, String clusterName) {
        AppsV1beta1Deployment deployment = new AppsV1beta1Deployment();

        deployment.setKind("Deployment");
        deployment.setApiVersion("apps/v1beta1");
        deployment.setMetadata(v1ObjectMeta(clusterName));
        deployment.setSpec(appsV1beta1DeploymentSpec());
        deployment.getSpec().setTemplate(v1PodTemplateSpec(instanceName, stack.getImage().getImageName(), group));
        return deployment;
    }

    private V1Container v1Container(String instanceName, String imageName, Group group) {
        V1Container v1Container = new V1Container();
        v1Container.setName(instanceName);
        v1Container.setImage(imageName);
        v1Container.setCommand(cmd());
        V1SecurityContext secContext = v1SecurityContext();
        v1Container.setSecurityContext(secContext);
        v1Container.setVolumeMounts(new ArrayList<>());
        v1Container.getVolumeMounts().add(configVolume());
        v1Container.setPorts(new ArrayList<>());
        v1Container.getPorts().add(sshPort());
        v1Container.getPorts().add(nginxPort());
        v1Container.getPorts().add(ambariPort());
        v1Container.setResources(v1ResourceRequirements(group));
        return v1Container;
    }

    private V1SecurityContext v1SecurityContext() {
        V1SecurityContext secContext = new V1SecurityContext();
        secContext.setPrivileged(true);
        return secContext;
    }

    private V1PodTemplateSpec v1PodTemplateSpec(String instanceName, String imageName, Group group) {
        V1PodTemplateSpec v1PodTemplateSpec = new V1PodTemplateSpec();
        v1PodTemplateSpec.setMetadata(v1ObjectMeta());
        v1PodTemplateSpec.setSpec(v1ProdSpec(instanceName, imageName, group));
        return v1PodTemplateSpec;
    }

    private V1ObjectMeta v1ObjectMeta(String clusterName, Optional<String> groupName) {
        V1ObjectMeta podMeta = new V1ObjectMeta();
        podMeta.setLabels(getLabels(clusterName, groupName));
        return podMeta;
    }

    private V1PodSpec v1ProdSpec(String instanceName, String imageName, Group group) {
        V1PodSpec v1PodSpec = new V1PodSpec();
        v1PodSpec.setVolumes(new ArrayList<>());
        v1PodSpec.getVolumes().add(configMapVolume());
        v1PodSpec.setContainers(new ArrayList<>());
        v1PodSpec.getContainers().add(v1Container(instanceName, imageName, group));
        return v1PodSpec;
    }

    private List<String> cmd() {
        List<String> cmd = new ArrayList<>();
        cmd.add("/sbin/init");
        return cmd;
    }

    private V1Volume configMapVolume() {
        V1Volume configVolume = new V1Volume();
        configVolume.setName("config-volume");
        configVolume.setConfigMap(configMap());
        return configVolume;
    }

    private V1ConfigMapVolumeSource configMap() {
        V1ConfigMapVolumeSource configMap = new V1ConfigMapVolumeSource();
        configMap.setDefaultMode(DEFAULT_MODE);
        configMap.setName(configName);
        return configMap;
    }

    private V1ResourceRequirements v1ResourceRequirements(Group group) {
        V1ResourceRequirements resources = new V1ResourceRequirements();
        resources.putLimitsItem("memory", Quantity.fromString(group.getReferenceInstanceConfiguration().getTemplate() + "Mi"));
        resources.putLimitsItem("cpu", Quantity.fromString(group.getReferenceInstanceConfiguration().getTemplate()..getCpus() + ""));
        return resources;
    }

    private V1ContainerPort ambariPort() {
        V1ContainerPort saltPort = new V1ContainerPort();
        saltPort.setName("ambari");
        saltPort.setContainerPort(AMBARI_PORT);
        return saltPort;
    }

    private V1ContainerPort nginxPort() {
        V1ContainerPort ngnixPort = new V1ContainerPort();
        ngnixPort.setName("ngnix");
        ngnixPort.setContainerPort(NGNIX_PORT);
        return ngnixPort;
    }

    private V1ContainerPort sshPort() {
        V1ContainerPort sshPort = new V1ContainerPort();
        sshPort.setContainerPort(SSH_PORT);
        sshPort.setName("ssh");
        return sshPort;
    }

    private V1VolumeMount configVolume() {
        V1VolumeMount configVolumeMount = new V1VolumeMount();
        configVolumeMount.setName("config-volume");
        configVolumeMount.setMountPath("/configmap");
        return configVolumeMount;
    }

    private AppsV1beta1DeploymentSpec appsV1beta1DeploymentSpec(String clusterName) {
        AppsV1beta1DeploymentSpec appsV1beta1DeploymentSpec = new AppsV1beta1DeploymentSpec();
        appsV1beta1DeploymentSpec.setReplicas(1);
        appsV1beta1DeploymentSpec.setSelector(v1LabelSelector(clusterName));
        return appsV1beta1DeploymentSpec;
    }

    private V1ObjectMeta v1ObjectMeta(String clusterName) {
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(clusterName);
        meta.setLabels(getLabels(clusterName, Optional.empty()));
        return meta;
    }

    private void createApiClient() throws IOException {
        ApiClient client = apiClient();
        Configuration.setDefaultApiClient(client);
    }

    private V1LabelSelector v1LabelSelector(String clusterName) {
        V1LabelSelector selector = new V1LabelSelector();
        selector.matchLabels(selectorMap(clusterName));
        return selector;
    }

    private Map<String, String> selectorMap(String clusterName) {
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put(APP_NAME, clusterName);
        matchLabels.put(CB_CLUSTER_NAME, clusterName);
        return matchLabels;
    }


    private void createConfigMap(AuthenticatedContext ac, CloudStack stack) throws IOException, ApiException {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> e : stack.getParameters().entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
        }
        String configPropsString = sb.toString();

        V1ConfigMap body = new V1ConfigMap();

        V1ObjectMeta configMeta = new V1ObjectMeta();
        body.setMetadata(configMeta);
        Map<String, String> labels = new HashMap<>();
        configMeta.setLabels(labels);
        labels.put(CB_CLUSTER_NAME, k8sResourceNameGenerator.getClusterName(ac));


        String configMapName = k8sResourceNameGenerator.getConfigMapName(ac);

        configMeta.setName(configMapName);
        Map<String, String> configData = new HashMap<>();
        body.setData(configData);

        configData.put("cloudbreak-config.props", configPropsString);

        coreV1Api().createNamespacedConfigMap(defaultNameSpace, body, null);
        LOGGER.info("Created ConfigMap " + configMapName);
    }

    private boolean checkApplicationAlreadyCreated(K8sCredentialView k8sCredential, String applicationName) throws MalformedURLException {
        return true;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case K8S_APPLICATION:
                    result.add(new CloudResourceStatus(resource, ResourceStatus.CREATED));
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext,
            CloudStack stack, List<CloudResource> cloudResources) throws Exception {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : cloudResources) {
            switch (resource.getType()) {
                case K8S_APPLICATION:
                    K8sApiUtils.deleteK8sApp(resource.getName());
                    result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED));
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        throw new CloudOperationNotSupportedException("Upscale stack operation is not supported on K8S");
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms, Object resourcesToRemove) {
        throw new CloudOperationNotSupportedException("Downscale stack operation is not supported on K8S");
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Downscale resources collection operation is not supported on K8S");
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        throw new TemplatingDoesNotSupportedException();
    }

}
