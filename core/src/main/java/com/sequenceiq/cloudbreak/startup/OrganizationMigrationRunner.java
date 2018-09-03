package com.sequenceiq.cloudbreak.startup;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.constraint.ConstraintTemplateService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

@Component
public class OrganizationMigrationRunner {

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private UserAndOrganizationMigrator userAndOrganizationMigrator;

    @Inject
    private StackOrganizatationMigrator stackOrganizatationMigrator;

    @Inject
    private OrganizationAwareResourceMigrator organizationAwareResourceMigrator;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private CredentialService credentialService;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ConstraintTemplateRepository constraintTemplateRepository;

    @Inject
    private ConstraintTemplateService constraintTemplateService;

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private ImageCatalogRepository imageCatalogRepository;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private List<OrganizationAwareResourceService<? extends OrganizationAwareResource>> services;

    public void run() {
        try {
            List<Stack> stacks = transactionService.required(() -> stackRepository.findAllAlive());
            if (areAllStacksSet(stacks)) {
                return;
            }
            UserMigrationResults userMigrationResults = userAndOrganizationMigrator.migrateUsersAndOrgs();
            stackOrganizatationMigrator.migrateStackOrgAndCreator(stacks, userMigrationResults);
            services.stream()
                    .filter(service -> !service.resource().equals(OrganizationResource.STRUCTURED_EVENT))
                    .map(service -> (OrganizationAwareResourceService<OrganizationAwareResource>) service)
                    .forEach(service -> {
                        organizationAwareResourceMigrator.migrateResourceOrg(userMigrationResults, service.repository()::findAll,
                                service.repository()::save);
                    });

        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private boolean areAllStacksSet(List<Stack> stacks) {
        return stacks.stream().allMatch(stack -> stack.getOrganization() != null && stack.getCreator() != null);
    }
}
