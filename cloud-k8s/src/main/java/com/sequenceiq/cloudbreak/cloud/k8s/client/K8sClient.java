package com.sequenceiq.cloudbreak.cloud.k8s.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1beta1Api;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.authenticators.GCPAuthenticator;

@Service
public class K8sClient {

    public CoreV1Api coreV1Api() {
        CoreV1Api coreV1Api = new CoreV1Api();
        return coreV1Api;
    }

    public AppsV1beta1Api appsV1beta1Api(CoreV1Api coreV1Api) throws IOException {
        ApiClient apiClient = apiClient();
        AppsV1beta1Api appsV1beta1Api = new AppsV1beta1Api(apiClient);

        return appsV1beta1Api;
    }

    public AppsV1beta2Api appsV1beta2Api() throws IOException {
        ApiClient apiClient = apiClient();
        AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);

        return appsV1beta2Api;
    }

    public ApiClient apiClient() throws IOException {
        KubeConfig.registerAuthenticator(new GCPAuthenticator());
        return Config.defaultClient();
    }

    public void createConfigMap(CoreV1Api coreV1Api, String nameSpace, Map<String, String> labels, String cbConfigName, String configPropsString)
            throws ApiException {
        V1ConfigMap body = new V1ConfigMap();

        V1ObjectMeta configMeta = new V1ObjectMeta();
        body.setMetadata(configMeta);
        configMeta.setLabels(labels);

        configMeta.setName(cbConfigName);

        Map<String, String> configData = new HashMap<>();
        body.setData(configData);

        configData.put("cloudbreak-config.props", configPropsString);

        coreV1Api.createNamespacedConfigMap(nameSpace, body, null);
    }

    public void createConfigMap(CoreV1Api coreV1Api, String nameSpace, String cbConfigName, String configPropsString)
            throws ApiException {
        createConfigMap(coreV1Api, nameSpace, Maps.newHashMap(), cbConfigName, configPropsString);
    }


}
