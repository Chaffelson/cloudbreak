package com.sequenceiq.cloudbreak.cloud.k8s.auth;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class K8sClientUtil {

    public K8sCredentialView createK8sClient(AuthenticatedContext authenticatedContext) {
        K8sCredentialView k8sCredentialView = new K8sCredentialView(authenticatedContext.getCloudCredential());
        return k8sCredentialView;
    }
}
