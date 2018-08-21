package com.sequenceiq.cloudbreak.cloud.k8s.auth;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class K8sCredentialView {
    private final CloudCredential cloudCredential;

    public K8sCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }
}
