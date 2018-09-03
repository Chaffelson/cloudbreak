package com.sequenceiq.cloudbreak.startup;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;

public class UserMigrationResults {

    private final List<IdentityUser> identityUsers;

    private final Map<String, User> ownerIdToUser;

    private final Organization orgForOrphanedResources;

    public UserMigrationResults(List<IdentityUser> identityUsers, Map<String, User> ownerIdToUser, Organization orgForOrphanedResources) {
        this.identityUsers = identityUsers;
        this.ownerIdToUser = ownerIdToUser;
        this.orgForOrphanedResources = orgForOrphanedResources;
    }

    public List<IdentityUser> getIdentityUsers() {
        return identityUsers;
    }

    public Map<String, User> getOwnerIdToUser() {
        return ownerIdToUser;
    }

    public Organization getOrgForOrphanedResources() {
        return orgForOrphanedResources;
    }
}
