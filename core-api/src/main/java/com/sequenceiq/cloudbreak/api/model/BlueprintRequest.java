package com.sequenceiq.cloudbreak.api.model;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class BlueprintRequest extends BlueprintBase {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @NotNull
    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100 and should not contain semicolon "
            + "and percentage character.")
    @Pattern(regexp = "^[^;%]*$")
    private String name;

    @ApiModelProperty(BlueprintModelDescription.URL)
    private String url;

    @ApiModelProperty(BlueprintModelDescription.BLUEPRINT_PROPERTIES)
    private List<Map<String, Map<String, String>>> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Map<String, Map<String, String>>> getProperties() {
        return properties;
    }

    public void setProperties(List<Map<String, Map<String, String>>> properties) {
        this.properties = properties;
    }
}
