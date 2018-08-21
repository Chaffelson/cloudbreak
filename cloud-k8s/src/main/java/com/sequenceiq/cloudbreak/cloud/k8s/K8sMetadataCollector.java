package com.sequenceiq.cloudbreak.cloud.k8s;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import io.kubernetes.client.ApiException;

@Service
public class K8sMetadataCollector implements MetadataCollector {

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources,
            List<CloudInstance> vms, List<CloudInstance> knownInstances) {
        try {
            CloudResource k8sApplication = getK8sApplcationResource(resources);

            List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = Lists.newArrayList();
            Map<String, Map<String, K8sServicePodContainer>> groupServicesByInstanceGroup =
                    K8sApiUtils.collectContainersByGroup(k8sApplication.getName());

            ListMultimap<String, CloudInstance> groupInstancesByInstanceGroup = groupInstancesByInstanceGroup(vms);
            for (Map.Entry<String, Map<String, K8sServicePodContainer>> e: groupServicesByInstanceGroup.entrySet()) {
                List<CloudInstance> groupInstances = groupInstancesByInstanceGroup.get(e.getKey());
                Map<String, K8sServicePodContainer> groupServices = groupServicesByInstanceGroup.get(e.getKey());
                Map<String, CloudInstance> mapByInstanceId = mapByInstanceId(groupInstances);
                Queue<CloudInstance> untrackedInstances = untrackedInstances(groupInstances);
                for (K8sServicePodContainer k8sContainer : groupServices.values()) {
                    CloudInstance cloudInstance = mapByInstanceId.get(k8sContainer.getPrivateIp());
                    if (cloudInstance == null) {
                        if (!untrackedInstances.isEmpty()) {
                            cloudInstance = untrackedInstances.remove();
                            cloudInstance = new CloudInstance(k8sContainer.getPublicIp(), cloudInstance.getTemplate(), cloudInstance.getAuthentication());
                        }
                    }
                    if (cloudInstance != null) {
                        CloudInstanceMetaData md = new CloudInstanceMetaData(k8sContainer.getPrivateIp(), k8sContainer.getPublicIp());
                        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, md);
                        cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
                    }
                }
            }
            return cloudVmMetaDataStatuses;
        } catch (ApiException ioe) {
            throw new CloudConnectorException(ioe);
        } catch (InterruptedException ioe) {
            throw new CloudConnectorException(ioe);
        } catch (CloudbreakException ioe) {
            throw new CloudConnectorException(ioe);
        } catch (IOException ioe) {
            throw new CloudConnectorException(ioe);
        }
    }

    private CloudResource getK8sApplcationResource(Iterable<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.K8S_APPLICATION) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.K8S_APPLICATION));
    }

    private ListMultimap<String, CloudInstance> groupInstancesByInstanceGroup(Iterable<CloudInstance> vms) {
        ListMultimap<String, CloudInstance> groupByInstanceGroup = ArrayListMultimap.create();
        for (CloudInstance vm : vms) {
            String groupName = vm.getTemplate().getGroupName();
            groupByInstanceGroup.put(groupName, vm);
        }
        return groupByInstanceGroup;
    }

    private Map<String, CloudInstance> mapByInstanceId(Iterable<CloudInstance> vms) {
        Map<String, CloudInstance> groupByInstanceId = Maps.newHashMap();
        for (CloudInstance vm : vms) {
            String instanceId = vm.getInstanceId();
            if (instanceId != null) {
                groupByInstanceId.put(instanceId, vm);
            }
        }
        return groupByInstanceId;
    }

    private Queue<CloudInstance> untrackedInstances(Iterable<CloudInstance> vms) {
        Queue<CloudInstance> cloudInstances = Lists.newLinkedList();
        for (CloudInstance vm : vms) {
            if (vm.getInstanceId() == null) {
                cloudInstances.add(vm);
            }
        }
        return cloudInstances;
    }
}
