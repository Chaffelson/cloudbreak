package com.sequenceiq.cloudbreak.startup;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ORG_MANAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.Tenant;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.organization.TenantRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class UserAndOrganizationMigrator {

    private static final String ORPHANED_RESOURCES = "OrphanedResources";

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private UserOrgPermissionsService userOrgPermissionsService;

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private UserService userService;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private TransactionService transactionService;

    @Value("${cb.client.secret}")
    private String clientSecret;

    public UserMigrationResults migrateUsersAndOrgs() throws TransactionExecutionException {
        List<IdentityUser> identityUsers = userDetailsService.getAllUsers(clientSecret);
        Map<String, User> ownerIdToUser = new HashMap<>();
        createUsersAndFillUserDataStructures(identityUsers, ownerIdToUser);
        Organization orphanedResources = getOrCreateOrphanedResourcesOrg();
        addUsersToOrphanedResourcesOrg(ownerIdToUser, orphanedResources);
        return new UserMigrationResults(identityUsers, ownerIdToUser, orphanedResources);
    }

    private void createUsersAndFillUserDataStructures(List<IdentityUser> identityUsers, Map<String, User> ownerIdToUser) throws TransactionExecutionException {
        transactionService.required(() -> {
            identityUsers.forEach(identityUser -> {
                User user = userService.getOrCreate(identityUser);
                ownerIdToUser.put(identityUser.getUserId(), user);
                userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user);
            });
            return null;
        });
    }

    private Organization getOrCreateOrphanedResourcesOrg() throws TransactionExecutionException {
        return transactionService.required(() -> {
            Tenant tenant = tenantRepository.findByName("DEFAULT");
            Organization orphanedResources = organizationRepository.getByName(ORPHANED_RESOURCES, tenant);
            if (orphanedResources == null) {
                orphanedResources = new Organization();
                orphanedResources.setName(ORPHANED_RESOURCES);
                orphanedResources.setDescription("Organization for storing resources that were created by users " +
                        "who were not available during organization database migration.");
                orphanedResources.setTenant(tenant);
                orphanedResources = organizationRepository.save(orphanedResources);
            }
            return orphanedResources;
        });
    }

    private void addUsersToOrphanedResourcesOrg(Map<String, User> ownerIdToUser, Organization orphanedResources) throws TransactionExecutionException {
        Set<UserOrgPermissions> orphanedUserPermissions = ownerIdToUser.values().stream().map(u -> {
            UserOrgPermissions userOrgPermissions = new UserOrgPermissions();
            userOrgPermissions.setUser(u);
            userOrgPermissions.setOrganization(orphanedResources);
            userOrgPermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), ORG_MANAGE.value()));
            return userOrgPermissions;
        }).collect(Collectors.toSet());
        transactionService.required(() -> {
            userOrgPermissionsService.saveAll(orphanedUserPermissions);
            return null;
        });
    }
}
