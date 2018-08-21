package com.sequenceiq.cloudbreak.cloud.k8s.auth;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.k8s.K8sConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class K8sAuthenticator implements Authenticator {
    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    @Override
    public Platform platform() {
        return K8sConstants.K8S_PLATFORM;
    }

    @Override
    public Variant variant() {
        return K8sConstants.K8S_VARIANT;
    }
}
