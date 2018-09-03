package com.sequenceiq.cloudbreak.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.organization.TenantRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class StackOrganizatationMigratorTest {

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
    private UserAndOrganizationMigrator userAndOrganizationMigrator;

    private UserMigrationResults userMigrationResults;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private StackService stackService;

    @Mock
    private OrganizationService organizationService;

    @Captor
    private ArgumentCaptor<Stack> stackDeleteCaptor;

    @Captor
    private ArgumentCaptor<Stack> stackSaveCaptor;

    @InjectMocks
    private StackOrganizatationMigrator underTest;

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
        userMigrationResults = userAndOrganizationMigrator.migrateUsersAndOrgs();

        when(organizationService.getDefaultOrganizationForUser(any(User.class))).thenAnswer((Answer<Organization>) invocation -> {
            User user = invocation.getArgument(0);
            Organization organization = new Organization();
            organization.setName(user.getUserId());
            return organization;
        });
    }

    @Test
    public void testStackMigration() throws TransactionExecutionException {
        // expected creator: 1@hw.com, expected org: 1@hw.com
        Stack stackPrivate = new Stack();
        stackPrivate.setName("stackPrivate");
        stackPrivate.setOwner("1");
        stackPrivate.setAccount("1");
        stackPrivate.setPublicInAccount(false);

        // expected creator: 2@hw.com expected org: 1
        Stack stackPublic = new Stack();
        stackPublic.setName("stackPublic");
        stackPublic.setOwner("2");
        stackPublic.setAccount("1");
        stackPublic.setPublicInAccount(true);

        // expected creator: 1@hw.com, expected org: 1
        Stack stackMissingOwner = new Stack();
        stackMissingOwner.setName("stackMissingOwner");
        stackMissingOwner.setOwner("7");
        stackMissingOwner.setAccount("1");
        stackMissingOwner.setPublicInAccount(true);

        // expected to be deleted
        Stack stackMissingOwnerPrivate = new Stack();
        stackMissingOwnerPrivate.setName("stackMissingOwnerPrivate");
        stackMissingOwnerPrivate.setOwner("7");
        stackMissingOwnerPrivate.setAccount("1");
        stackMissingOwnerPrivate.setPublicInAccount(false);

        // expected creator: 3@hw.com, expected org: 3
        Stack stackOwnerIsNotInAccount = new Stack();
        stackOwnerIsNotInAccount.setName("stackOwnerIsNotInAccount");
        stackOwnerIsNotInAccount.setOwner("1");
        stackOwnerIsNotInAccount.setAccount("3");
        stackOwnerIsNotInAccount.setPublicInAccount(true);

        // expected creator: 5@hw.com, expected org: 5@hw.com
        Stack stackOwnerIsNotInAccountPrivate = new Stack();
        stackOwnerIsNotInAccountPrivate.setName("stackOwnerIsNotInAccountPrivate");
        stackOwnerIsNotInAccountPrivate.setOwner("5");
        stackOwnerIsNotInAccountPrivate.setAccount("3");
        stackOwnerIsNotInAccountPrivate.setPublicInAccount(false);

        // expected to be deleted
        Stack stackOwnerAndAccountIsMissing = new Stack();
        stackOwnerAndAccountIsMissing.setName("stackOwnerAndAccountIsMissing");
        stackOwnerAndAccountIsMissing.setOwner("7");
        stackOwnerAndAccountIsMissing.setAccount("7");
        stackOwnerAndAccountIsMissing.setPublicInAccount(true);

        List<Stack> stacks = List.of(stackPrivate, stackPublic, stackMissingOwner, stackMissingOwnerPrivate,
                stackOwnerIsNotInAccount, stackOwnerIsNotInAccountPrivate, stackOwnerAndAccountIsMissing);

        underTest.migrateStackOrgAndCreator(stacks, userMigrationResults);

        verify(stackService, times(2)).delete(stackDeleteCaptor.capture(), anyBoolean(), anyBoolean());
        verify(stackRepository, times(5)).save(stackSaveCaptor.capture());

        List<Stack> deletedStacks = stackDeleteCaptor.getAllValues();
        assertEquals("7", deletedStacks.get(0).getOwner());
        assertEquals("1", deletedStacks.get(0).getAccount());
        assertEquals("stackMissingOwnerPrivate", deletedStacks.get(0).getName());
        assertFalse(deletedStacks.get(0).isPublicInAccount());

        assertEquals("7", deletedStacks.get(1).getOwner());
        assertEquals("7", deletedStacks.get(1).getAccount());
        assertEquals("stackOwnerAndAccountIsMissing", deletedStacks.get(1).getName());
        assertTrue(deletedStacks.get(1).isPublicInAccount());

        List<Stack> savedStacks = stackSaveCaptor.getAllValues();
        assertEquals("1@hw.com", savedStacks.get(0).getCreator().getUserId());
        assertEquals("1@hw.com", savedStacks.get(0).getOrganization().getName());
        assertEquals("stackPrivate", savedStacks.get(0).getName());

        assertEquals("2@hw.com", savedStacks.get(1).getCreator().getUserId());
        assertEquals("1", savedStacks.get(1).getOrganization().getName());
        assertEquals("stackPublic", savedStacks.get(1).getName());

        assertEquals("1@hw.com", savedStacks.get(2).getCreator().getUserId());
        assertEquals("1", savedStacks.get(2).getOrganization().getName());
        assertEquals("stackMissingOwner", savedStacks.get(2).getName());

        assertEquals("3@hw.com", savedStacks.get(3).getCreator().getUserId());
        assertEquals("3", savedStacks.get(3).getOrganization().getName());
        assertEquals("stackOwnerIsNotInAccount", savedStacks.get(3).getName());

        assertEquals("5@hw.com", savedStacks.get(4).getCreator().getUserId());
        assertEquals("5@hw.com", savedStacks.get(4).getOrganization().getName());
        assertEquals("stackOwnerIsNotInAccountPrivate", savedStacks.get(4).getName());
    }
}