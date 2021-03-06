package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Gateway;

public class GatewayV3Action {

    private GatewayV3Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Gateway gateway = (Gateway) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        gateway.setResponse(client.getCloudbreakClient().connectorV3Endpoint().getGatewaysCredentialId(orgId, gateway.getRequest()));
        logJSON("V3 Connectors Gateways post request: ", gateway.getRequest());
    }
}
