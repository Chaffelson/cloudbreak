package com.sequenceiq.cloudbreak.cloud.k8s.client.model.core;

/**
 * The current state of an application
 **/
public enum ApplicationState {
    ACCEPTED, STARTED, READY, STOPPED, FAILED
}