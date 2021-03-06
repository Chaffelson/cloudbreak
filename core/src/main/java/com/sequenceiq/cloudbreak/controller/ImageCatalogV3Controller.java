package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ImageCatalogV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StackImageFilterService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class ImageCatalogV3Controller extends NotificationController implements ImageCatalogV3Endpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackImageFilterService stackImageFilterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<ImageCatalogResponse> listByOrganization(Long organizationId) {
        return imageCatalogService.findAllByOrganizationId(organizationId).stream()
                .map(imageCatalog -> conversionService.convert(imageCatalog, ImageCatalogResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public ImageCatalogResponse getByNameInOrganization(Long organizationId, String name, boolean withImages) {
        ImageCatalogResponse imageCatalogResponse = conversionService.convert(imageCatalogService.get(organizationId, name), ImageCatalogResponse.class);
        Images images = imageCatalogService.propagateImagesIfRequested(organizationId, name, withImages);
        if (images != null) {
            imageCatalogResponse.setImagesResponse(conversionService.convert(images, ImagesResponse.class));
        }
        return imageCatalogResponse;
    }

    @Override
    public ImageCatalogResponse createInOrganization(Long organizationId, ImageCatalogRequest request) {
        ImageCatalog imageCatalog = conversionService.convert(request, ImageCatalog.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        imageCatalog = imageCatalogService.create(imageCatalog, organizationId, user);
        notify(ResourceEvent.IMAGE_CATALOG_CREATED);
        return conversionService.convert(imageCatalog, ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogResponse deleteInOrganization(Long organizationId, String name) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        ImageCatalog deleted = imageCatalogService.delete(organizationId, name, identityUser, user);
        notify(ResourceEvent.IMAGE_CATALOG_DELETED);
        return conversionService.convert(deleted, ImageCatalogResponse.class);
    }

    @Override
    public ImagesResponse getImagesByProviderFromImageCatalogInOrganization(Long organizationId, String name, String platform) throws Exception {
        Images images = imageCatalogService.getImages(organizationId, name, platform).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImagesResponse getImagesByProvider(Long organizationId, String platform) throws Exception {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        Images images = imageCatalogService.getImagesOsFiltered(platform, null, identityUser, user).getImages();
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImagesResponse getImagesFromCustomImageCatalogByStackInOrganization(Long organizationId, String name, String stackName) throws Exception {
        Images images = stackImageFilterService.getApplicableImages(organizationId, name, stackName);
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImagesResponse getImagesFromDefaultImageCatalogByStackInOrganization(Long organizationId, String stackName) throws Exception {
        Images images = stackImageFilterService.getApplicableImages(organizationId, stackName);
        return conversionService.convert(images, ImagesResponse.class);
    }

    @Override
    public ImageCatalogResponse putPublicInOrganization(Long organizationId, UpdateImageCatalogRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        ImageCatalog imageCatalog = imageCatalogService.update(organizationId, conversionService.convert(request, ImageCatalog.class), user);
        return conversionService.convert(imageCatalog, ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogResponse putSetDefaultByNameInOrganization(Long organizationId, String name) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        return conversionService.convert(imageCatalogService.setAsDefault(organizationId, name, identityUser, user), ImageCatalogResponse.class);
    }

    @Override
    public ImageCatalogRequest getRequestFromName(Long organizationId, String name) {
        ImageCatalog imageCatalog = imageCatalogService.get(organizationId, name);
        return conversionService.convert(imageCatalog, ImageCatalogRequest.class);
    }
}
