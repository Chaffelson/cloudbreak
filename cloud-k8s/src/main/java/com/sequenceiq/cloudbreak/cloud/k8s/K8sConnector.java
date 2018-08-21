package com.sequenceiq.cloudbreak.cloud.k8s;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.k8s.auth.K8sAuthenticator;
import com.sequenceiq.cloudbreak.cloud.k8s.auth.K8sCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class K8sConnector implements CloudConnector {
    @Inject
    private K8sAuthenticator authenticator;

    @Inject
    private K8sProvisionSetup provisionSetup;

    @Inject
    private K8sCredentialConnector credentialConnector;

    @Inject
    private K8sResourceConnector resourceConnector;

    @Inject
    private K8sInstanceConnector instanceConnector;

    @Inject
    private K8sMetadataCollector metadataCollector;

    @Inject
    private K8sPlatformParameters platformParameters;

    @Inject
    private K8sPlatformResources platformResources;

    @Inject
    private K8sConstants k8sConstants;

    @Override
    public Authenticator authentication() {
        return authenticator;
    }

    @Override
    public Setup setup() {
        return provisionSetup;
    }

    @Override
    public List<Validator> validators() {
        return Collections.emptyList();
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }

    @Override
    public ResourceConnector resources() {
        return resourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return instanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return metadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return platformParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return platformResources;
    }

    @Override
    public Platform platform() {
        return K8sConstants.K8S_PLATFORM;
    }

    @Override
    public Variant variant() {
        return K8sConstants.K8S_VARIANT;
    }

    @Override
    public CloudConstant cloudConstant() {
        return k8sConstants;
    }
}
