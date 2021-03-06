package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RdsConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class RdsConfigV3Controller extends AbstractRdsConfigController implements RdsConfigV3Endpoint {

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<RDSConfigResponse> listByOrganization(Long organizationId) {
        return getRdsConfigService().findAllByOrganizationId(organizationId).stream()
                .map(rdsConfig -> getConversionService().convert(rdsConfig, RDSConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RDSConfigResponse getByNameInOrganization(Long organizationId, String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForOrganizationId(name, organizationId);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse createInOrganization(Long organizationId, RDSConfigRequest request) {
        RDSConfig rdsConfig = getConversionService().convert(request, RDSConfig.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        rdsConfig = getRdsConfigService().create(rdsConfig, organizationId, user);
        notify(ResourceEvent.RDS_CONFIG_CREATED);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse deleteInOrganization(Long organizationId, String name) {
        RDSConfig deleted = getRdsConfigService().deleteByNameFromOrganization(name, organizationId);
        notify(ResourceEvent.RDS_CONFIG_DELETED);
        return getConversionService().convert(deleted, RDSConfigResponse.class);
    }

    @Override
    public RdsTestResult testRdsConnection(Long organizationId, RDSTestRequest rdsTestRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = getRdsConfigService().getOrganizationService().get(organizationId, user);
        return testRdsConnection(rdsTestRequest, organization);
    }

    @Override
    public RDSConfigRequest getRequestFromName(Long organizationId, String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForOrganizationId(name, organizationId);
        return getConversionService().convert(rdsConfig, RDSConfigRequest.class);
    }
}