package com.sequenceiq.cloudbreak.startup;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.organization.TenantRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserAndOrganizationMigratorTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserOrgPermissionsService userOrgPermissionsService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Spy
    private final TransactionService transactionService = new TransactionService();

    @InjectMocks
    private UserAndOrganizationMigrator underTest;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(userService.getOrCreate(any(IdentityUser.class))).thenAnswer((Answer<User>) invocation -> {
            IdentityUser identityUser = invocation.getArgument(0);
            User user = new User();
            user.setUserId(identityUser.getUsername());
            return user;
        });
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer((Answer<Organization>) invocation -> invocation.getArgument(0));
        when(organizationRepository.getByName(anyString(), any())).thenReturn(null);
        when(userOrgPermissionsService.findForUserAndOrganization(any(), any())).thenReturn(null);
    }

    @Test
    public void testWithUsersInCommonAccounts() throws TransactionExecutionException {
        List<IdentityUser> identityUsers = List.of(
                new IdentityUser("1", "1@hw.com", "1",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("2", "2@hw.com", "1",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("3", "3@hw.com", "3",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("4", "4@hw.com", "4",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("5", "5@hw.com", "4",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("6", "6@hw.com", "6",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())));


        when(userDetailsService.getAllUsers(null)).thenReturn(identityUsers);

        UserMigrationResults userMigrationResults = underTest.migrateUsersAndOrgs();
        assertEquals(6, userMigrationResults.getOwnerIdToUser().size());
        assertEquals(6, userMigrationResults.getIdentityUsers().size());
        assertEquals(4, userMigrationResults.getAccountToUsers().size());
        assertEquals(4, userMigrationResults.getAccountToOrganization().size());
    }
}