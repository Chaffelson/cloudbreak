package com.sequenceiq.cloudbreak.cloud.k8s;

import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class K8sPlatformParameters implements PlatformParameters {
    // There is no need to initialize the disk on ycloud
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("nonexistent_device", 97);

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(Collections.emptyList(), DiskType.diskType(""), Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("yarn", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        return additionalStackParameterValidations;
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(
                Collections.singletonList(orchestrator(OrchestratorConstants.SALT)),
                orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public TagSpecification tagSpecification() {
        return null;
    }

    @Override
    public VmRecommendations recommendedVms() {
        return null;
    }

    @Override
    public String platforName() {
        return K8sConstants.K8S_PLATFORM.value();
    }

    @Override
    public SpecialParameters specialParameters() {
        Map<String, Boolean> specialParameters = Maps.newHashMap();
        specialParameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.NETWORK_IS_MANDATORY, Boolean.FALSE);
        specialParameters.put(PlatformParametersConsts.SCALING_SUPPORTED, Boolean.FALSE);
        specialParameters.put(PlatformParametersConsts.STARTSTOP_SUPPORTED, Boolean.FALSE);
        return new SpecialParameters(specialParameters);
    }

    private Collection<VmType> virtualMachines(Boolean extended) {
        return new ArrayList<>();
    }

    private VmType defaultVirtualMachine() {
        return vmType("");
    }
}
