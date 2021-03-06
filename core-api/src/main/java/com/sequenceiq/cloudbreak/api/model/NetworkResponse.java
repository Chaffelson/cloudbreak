package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkResponse extends NetworkBase {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(ModelDescriptions.ORGANIZATION_OF_THE_RESOURCE)
    private OrganizationResourceResponse organization;

    public OrganizationResourceResponse getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationResourceResponse organization) {
        this.organization = organization;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
