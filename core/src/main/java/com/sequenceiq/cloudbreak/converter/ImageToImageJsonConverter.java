package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class ImageToImageJsonConverter extends AbstractConversionServiceAwareConverter<Image, ImageJson> {

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ImageJson convert(Image source) {
        ImageJson imageJson = new ImageJson();
        imageJson.setImageName(source.getImageName());
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        imageJson.setImageCatalogUrl(Strings.isNullOrEmpty(source.getImageCatalogUrl())
                ? imageCatalogService.getImageDefaultCatalogUrl(identityUser, user) : source.getImageCatalogUrl());
        imageJson.setImageCatalogName(Strings.isNullOrEmpty(source.getImageCatalogName())
                ? "cloudbreak-default" : source.getImageCatalogName());
        imageJson.setImageId(Strings.isNullOrEmpty(source.getImageId())
                ? null : source.getImageId());
        return imageJson;
    }

}
