package com.sequenceiq.cloudbreak.service.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    @Inject
    private UserProfileRepository userProfileRepository;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    public UserProfile getOrCreate(String account, String owner, User user) {
        return getOrCreate(account, owner, null, user);
    }

    public UserProfile getOrCreate(String account, String owner, String userName, User user) {
        UserProfile userProfile = getSilently(account, owner);
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setAccount(account);
            userProfile.setOwner(owner);
            userProfile.setUserName(userName);
            addUiProperties(userProfile);
            userProfile.setUser(user);
            userProfile = userProfileRepository.save(userProfile);
        } else if (userProfile.getUserName() == null && userName != null) {
            userProfile.setUserName(userName);
            userProfile = userProfileRepository.save(userProfile);
        }
        return userProfile;
    }

    private UserProfile getSilently(String account, String owner) {
        try {
            return userProfileRepository.findOneByOwnerAndAccount(account, owner);
        } catch (AccessDeniedException ignore) {
            return null;
        }
    }

    public UserProfile save(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public Set<UserProfile> findOneByCredentialId(Long credentialId) {
        return userProfileRepository.findOneByCredentialId(credentialId);
    }

    public Set<UserProfile> findByImageCatalogId(Long catalogId) {
        return userProfileRepository.findOneByImageCatalogName(catalogId);
    }

    private void addUiProperties(UserProfile userProfile) {
        try {
            userProfile.setUiProperties(new Json(new HashMap<>()));
        } catch (JsonProcessingException ignored) {
            userProfile.setUiProperties(null);
        }
    }

    public void put(UserProfileRequest request, IdentityUser identityUser, User user, Organization organization) {
        UserProfile userProfile = getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user);
        if (request.getCredentialId() != null) {
            Credential credential = credentialService.get(request.getCredentialId(), organization);
            userProfile.setCredential(credential);
        } else if (request.getCredentialName() != null) {
            Credential credential = credentialService.getByNameForOrganization(request.getCredentialName(), organization);
            userProfile.setCredential(credential);
        }
        if (request.getImageCatalogName() != null) {
            Long organizationId = organization.getId();
            ImageCatalog imageCatalog = imageCatalogService.get(organizationId, request.getImageCatalogName());
            userProfile.setImageCatalog(imageCatalog);
        }
        for (Entry<String, Object> uiStringObjectEntry : request.getUiProperties().entrySet()) {
            Map<String, Object> map = userProfile.getUiProperties().getMap();
            if (map == null || map.isEmpty()) {
                map = new HashMap<>();
            }
            map.put(uiStringObjectEntry.getKey(), uiStringObjectEntry.getValue());
            try {
                userProfile.setUiProperties(new Json(map));
            } catch (JsonProcessingException ignored) {
                throw new BadRequestException("The modification of the ui properties was unsuccesfull.");
            }
        }
        userProfileRepository.save(userProfile);
    }
}
