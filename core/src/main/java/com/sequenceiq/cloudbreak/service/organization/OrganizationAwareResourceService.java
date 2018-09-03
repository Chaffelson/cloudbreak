package com.sequenceiq.cloudbreak.service.organization;

import java.util.Set;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;

public interface OrganizationAwareResourceService<T extends OrganizationAwareResource> {

    T create(T resource, Long organizationId, User user);

    T create(T resource, Organization organization, User user);

    T getByNameForOrganizationId(String name, Long organizationId);

    T getByNameForOrganization(String name, Organization organization);

    Set<T> findAllByOrganization(Organization organization);

    Set<T> findAllByOrganizationId(Long organizationId);

    T delete(T resource);

    T deleteByNameFromOrganization(String name, Long organizationId);

    OrganizationResourceRepository<T, Long> repository();

    OrganizationResource resource();
}
