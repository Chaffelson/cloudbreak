package com.sequenceiq.cloudbreak.cloud.k8s.client.api;

import java.net.MalformedURLException;
import java.net.URL;

public class K8sEndpoint {

    private String apiEndpoint;

    private String path;

    public K8sEndpoint(String apiEndpoint, String path) {
        this.apiEndpoint = apiEndpoint;
        this.path = path;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getContextRoot() {
        return K8sResourceConstants.CONTEXT_ROOT;
    }

    public String getVersion() {
        return K8sResourceConstants.API_VERSION;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URL getFullEndpointUrl() throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        return new URL(sb.toString());
    }
}
