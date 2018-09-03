package com.sequenceiq.cloudbreak.startup;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackOrganizatationMigrator {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackService stackService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private TransactionService transactionService;

    public void migrateStackOrgAndCreator(List<Stack> stacks, UserMigrationResults userMigrationResults) throws TransactionExecutionException {
        transactionService.required(() -> {
            stacks.forEach(stack -> {
                if (stack.getOrganization() == null) {
                    String owner = stack.getOwner();
                    User creator = userMigrationResults.getOwnerIdToUser().get(owner);
                    if (creator == null) {
                        putIntoOrphanedOrg(userMigrationResults, stack);
                    } else {
                        putIntoDefaultOrg(stack, creator);
                    }
                    stackRepository.save(stack);
                }
            });
            return null;
        });
    }

    private void putIntoOrphanedOrg(UserMigrationResults userMigrationResults, Stack stack) {
        Iterator<User> userIterator = userMigrationResults.getOwnerIdToUser().values().iterator();
        if (userIterator.hasNext()) {
            stack.setCreator(userIterator.next());
            stack.setOrganization(userMigrationResults.getOrgForOrphanedResources());
        }
    }

    private void putIntoDefaultOrg(Stack stack, User creator) {
        Organization organization = organizationService.getDefaultOrganizationForUser(creator);
        stack.setCreator(creator);
        stack.setOrganization(organization);
    }
}
