package com.sequenceiq.cloudbreak.cloud.k8s;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.K8S;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class K8sConstants implements CloudConstant {
    public static final Platform K8S_PLATFORM = Platform.platform(K8S);

    public static final Variant K8S_VARIANT = Variant.variant(K8S);

    public static final String K8S_LIFETIME_PARAMETER = "lifeTime";

    private K8sConstants() {
    }

    @Override
    public Platform platform() {
        return K8S_PLATFORM;
    }

    @Override
    public Variant variant() {
        return K8S_VARIANT;
    }
}
