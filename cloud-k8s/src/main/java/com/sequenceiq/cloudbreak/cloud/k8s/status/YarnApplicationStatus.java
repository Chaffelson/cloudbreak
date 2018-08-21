package com.sequenceiq.cloudbreak.cloud.k8s.status;

import com.sequenceiq.cloudbreak.cloud.k8s.client.model.core.ApplicationState;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class YarnApplicationStatus {
    private YarnApplicationStatus() {
    }

    public static ResourceStatus mapResourceStatus(String status) {
        return mapResourceStatus(getApplicationState(status));
    }

    public static ResourceStatus mapResourceStatus(ApplicationState status) {
        if (status == null) {
            return ResourceStatus.FAILED;
        }

        switch (status) {
            case ACCEPTED:
            case STARTED:
            case STOPPED:
                return ResourceStatus.IN_PROGRESS;
            case READY:
                return ResourceStatus.CREATED;
            default:
                return ResourceStatus.IN_PROGRESS;
        }
    }

    private static ApplicationState getApplicationState(String status) {
        try {
            return ApplicationState.valueOf(status);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
