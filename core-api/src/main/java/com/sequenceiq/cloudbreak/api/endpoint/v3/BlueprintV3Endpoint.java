package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.BlueprintOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/blueprints", description = ControllerDescription.BLUEPRINT_V3_DESCRIPTION, protocols = "http,https")
public interface BlueprintV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.LIST_BY_ORGANIZATION, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "listBlueprintsByOrganization")
    Set<BlueprintResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_NAME_IN_ORG, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintInOrganization")
    BlueprintResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.CREATE_IN_ORG, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "createBlueprintInOrganization")
    BlueprintResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid BlueprintRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_NAME_IN_ORG, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "deleteBlueprintInOrganization")
    BlueprintResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_BLUEPRINT_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintRequestFromName")
    BlueprintRequest getRequestFromName(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @GET
    @Path("{name}/custom-parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CUSTOM_PARAMETERS, produces = ContentType.JSON, nickname = "getBlueprintCustomParameters")
    ParametersQueryResponse getCustomParameters(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}
