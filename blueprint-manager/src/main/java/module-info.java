module blueprint.manager {
    requires java.compiler;
    requires javax.inject;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires commons.lang3;
    requires gson;
    requires guava;
    requires handlebars;
    requires jackson.annotations;
    requires json.lib;
    requires logback.classic;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires cloud.common;
    requires core.api;
    requires core;
    requires core.model;
    exports com.sequenceiq.cloudbreak.blueprint;
    exports com.sequenceiq.cloudbreak.blueprint.configuration;
    exports com.sequenceiq.cloudbreak.blueprint.filesystem;
    exports com.sequenceiq.cloudbreak.blueprint.filesystem.query;
    exports com.sequenceiq.cloudbreak.blueprint.kerberos;
    exports com.sequenceiq.cloudbreak.blueprint.nifi;
    exports com.sequenceiq.cloudbreak.blueprint.sharedservice;
    exports com.sequenceiq.cloudbreak.blueprint.smartsense;
    exports com.sequenceiq.cloudbreak.blueprint.template.views;
    exports com.sequenceiq.cloudbreak.blueprint.templates;
    exports com.sequenceiq.cloudbreak.blueprint.utils;
    exports com.sequenceiq.cloudbreak.blueprint.validation;
}