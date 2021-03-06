package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.ProxyConfigEntity;

public class ProxyConfigV3Action {
    private ProxyConfigV3Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        proxyconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .proxyConfigV3Endpoint()
                        .createInOrganization(orgId, proxyconfigEntity.getRequest()));
        logJSON("Proxy config post request: ", proxyconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        proxyconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .proxyConfigV3Endpoint()
                        .getByNameInOrganization(orgId, proxyconfigEntity.getName()));
        logJSON(" get proxy config response: ", proxyconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        proxyconfigEntity.setResponses(
                client.getCloudbreakClient()
                        .proxyConfigV3Endpoint()
                        .listByOrganization(orgId));
        logJSON(" get all proxy config response: ", proxyconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        client.getCloudbreakClient()
                .proxyConfigV3Endpoint()
                .deleteInOrganization(orgId, proxyconfigEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}