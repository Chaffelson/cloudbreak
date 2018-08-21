package com.sequenceiq.cloudbreak.cloud.k8s.client.exception;

public class K8sClientException extends Exception {

    public K8sClientException(String message) {
        super(message);
    }

    public K8sClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public K8sClientException(Throwable cause) {
        super(cause);
    }

    public K8sClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
