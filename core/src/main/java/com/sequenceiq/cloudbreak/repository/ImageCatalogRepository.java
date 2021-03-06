package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.IMAGECATALOG)
public interface ImageCatalogRepository extends OrganizationResourceRepository<ImageCatalog, Long> {

    @CheckPermissionsByReturnValue
    Set<ImageCatalog> findAllByOrganizationIdAndArchived(Long organizationId, boolean archived);
}
