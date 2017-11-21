package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariStackDetailsDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariStackDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariStackDetailsJson implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK, required = true)
    private String stack;

    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.VERSION, required = true)
    private String version;

    @ApiModelProperty(AmbariStackDetailsDescription.OS)
    private String os;

    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK_REPO_ID, required = true)
    private String stackRepoId;

    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK_BASE_URL, required = true)
    private String stackBaseURL;

    @ApiModelProperty(value = AmbariStackDetailsDescription.UTILS_REPO_ID, required = true)
    private String utilsRepoId;

    @ApiModelProperty(value = AmbariStackDetailsDescription.UTILS_BASE_URL, required = true)
    private String utilsBaseURL;

    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.VERIFY, required = true)
    private Boolean verify;

    @ApiModelProperty(value = AmbariStackDetailsDescription.REPOSITORY_VERSION, required = true)
    private String repositoryVersion;

    @ApiModelProperty(value = AmbariStackDetailsDescription.VDF_URL, required = true)
    private String versionDefinitionFileUrl;

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getStackRepoId() {
        return stackRepoId;
    }

    public void setStackRepoId(String stackRepoId) {
        this.stackRepoId = stackRepoId;
    }

    public String getStackBaseURL() {
        return stackBaseURL;
    }

    public void setStackBaseURL(String stackBaseURL) {
        this.stackBaseURL = stackBaseURL;
    }

    public void setVerify(Boolean verify) {
        this.verify = verify;
    }

    public Boolean getVerify() {
        return verify;
    }

    public String getUtilsRepoId() {
        return utilsRepoId;
    }

    public void setUtilsRepoId(String utilsRepoId) {
        this.utilsRepoId = utilsRepoId;
    }

    public String getUtilsBaseURL() {
        return utilsBaseURL;
    }

    public void setUtilsBaseURL(String utilsBaseURL) {
        this.utilsBaseURL = utilsBaseURL;
    }

    public String getRepositoryVersion() {
        return repositoryVersion;
    }

    public void setRepositoryVersion(String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
    }

    public String getVersionDefinitionFileUrl() {
        return versionDefinitionFileUrl;
    }

    public void setVersionDefinitionFileUrl(String versionDefinitionFileUrl) {
        this.versionDefinitionFileUrl = versionDefinitionFileUrl;
    }
}
