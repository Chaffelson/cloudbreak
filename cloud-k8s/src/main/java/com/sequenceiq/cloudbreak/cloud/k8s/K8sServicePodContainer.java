package com.sequenceiq.cloudbreak.cloud.k8s;

/**
 * The unnatural K8s artifact that represents a fat container connected up via a pod to a service
 */
public class K8sServicePodContainer {
    private final String privateIp;

    private final String publicIp;

    public K8sServicePodContainer(String privateIp, String publicIp) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }
}
