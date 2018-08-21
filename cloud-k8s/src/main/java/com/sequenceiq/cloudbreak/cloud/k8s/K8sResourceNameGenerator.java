package com.sequenceiq.cloudbreak.cloud.k8s;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Service
public class K8sResourceNameGenerator {

    @Value("${cb.max.k8s.resource.name.length:}")
    private int maxResourceNameLength;

    @Value("${cb.k8s.deafult.prefix.configmap:cb-config}")
    private int configmapPrefix;

    public String getConfigMapName(AuthenticatedContext ac) {
        return splitName(String.format("%s-%s", configmapPrefix, createApplicationName(ac)));
    }

    public String getInstanceContainerName(AuthenticatedContext ac, Group group, int index) {
        return splitName(String.format("%s-%s-%s", createApplicationName(ac), group.getName(), index));
    }

    public String getGroupName(Group group) {
        return splitName(String.format("%s", group.getName()));
    }

    public String getClusterName(AuthenticatedContext ac) {
        return splitName(createApplicationName(ac));
    }

    private String createApplicationName(AuthenticatedContext ac) {
        return String.format("%s-%s", ac.getCloudContext().getName(), ac.getCloudContext().getId());
    }

    private String splitName(String name) {
        return Splitter.fixedLength(maxResourceNameLength).splitToList(name).get(0);
    }
}
