package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class RepositoryLookupService {

    @Inject
    private List<CrudRepository<?, ?>> repositoryList;

    private final Map<Class<?>, CrudRepository<?, ?>> repositoryMap = new HashMap<>();

    @PostConstruct
    private void checkRepoMap() {
        if (CollectionUtils.isEmpty(repositoryList)) {
            throw new IllegalStateException("No repositories provided!");
        } else {
            fillRepositoryMap();
        }
    }

    private void fillRepositoryMap() {
        for (CrudRepository<?, ?> repo : repositoryList) {
            repositoryMap.put(getEntityClassForRepository(repo), repo);
        }
    }

    private Class<?> getEntityClassForRepository(CrudRepository<?, ?> repo) {
        Class<?> originalInterface = repo.getClass().getInterfaces()[0];
        EntityType annotation = originalInterface.getAnnotation(EntityType.class);
        if (annotation == null) {
            throw new IllegalStateException("Entity class is not specified for repository: " + originalInterface.getSimpleName());
        }
        return annotation.entityClass();
    }

    public <R> R getRepositoryForEntity(Class<?> clazz) {
        R repo = (R) repositoryMap.get(clazz);
        if (repo == null) {
            throw new IllegalStateException("No repository found for the entityClass:" + clazz);
        }
        return repo;
    }
}
