package com.sequenceiq.cloudbreak.core.flow2;

import static com.sequenceiq.cloudbreak.core.flow2.FlowTriggers.STACK_FORCE_TERMINATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.FlowTriggers.STACK_TERMINATE_TRIGGER_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<? extends Payload>> {
    public static final String FLOW_FINAL = "FLOWFINAL";

    public static final String FLOW_CANCEL = "FLOWCANCEL";

    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS =
            Collections.unmodifiableList(Arrays.asList(STACK_FORCE_TERMINATE_TRIGGER_EVENT, STACK_TERMINATE_TRIGGER_EVENT));

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private EventBus eventBus;

    private Lock lock = new ReentrantLock(true);

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        Payload payload = event.getData();
        String flowId = getFlowId(event);
        String flowChainId = getFlowChainId(event);

        if (FLOW_CANCEL.equals(key)) {
            cancelRunningFlows(payload.getStackId());
        } else if (FLOW_FINAL.equals(key)) {
            finalizeFlow(flowId, flowChainId, payload.getStackId());
        } else {
            if (flowId == null) {
                LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                FlowConfiguration<?> flowConfig = flowConfigurationMap.get(key);
                if (flowConfig != null && flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getStackId())) {
                    Flow flow;
                    lock.lock();
                    try {
                        if (!acceptFlow(key, payload)) {
                            LOGGER.info("Flow operation not allowed, other flow is running. Stack ID {}, event {}", payload.getStackId(), key);
                            return;
                        }
                        flowId = UUID.randomUUID().toString();
                        flow = flowConfig.createFlow(flowId);
                        flow.initialize();
                        flowLogService.save(flowId, key, payload, flowConfig.getClass(), flow.getCurrentState());
                    } finally {
                        lock.unlock();
                    }
                    runningFlows.put(flow, flowChainId);
                    flow.sendEvent(key, payload);
                }
            } else {
                LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
                Flow flow = runningFlows.get(flowId);
                if (flow != null) {
                    flowLogService.save(flowId, key, payload, flow.getFlowConfigClass(), flow.getCurrentState());
                    flow.sendEvent(key, payload);
                } else {
                    LOGGER.info("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getStackId(), flowId, key);
                }
            }
        }
    }

    private boolean acceptFlow(String key, Payload payload) {
        if (payload instanceof Acceptable) {
            Acceptable acceptable = (Acceptable) payload;
            if (!ALLOWED_PARALLEL_FLOWS.contains(key) && isOtherFlowRunning(payload.getStackId())) {
                acceptable.accepted().accept(Boolean.FALSE);
                return false;
            }
            acceptable.accepted().accept(Boolean.TRUE);
        }
        return true;
    }

    private boolean isOtherFlowRunning(Long stackId) {
        Set<String> flowIds = flowLogService.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        return !flowIds.isEmpty();
    }

    private void cancelRunningFlows(Long stackId) {
        Set<String> flowIds = flowLogService.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        LOGGER.debug("flow cancellation arrived: ids: {}", flowIds);
        for (String id : flowIds) {
            String flowChainId = runningFlows.getFlowChainId(id);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId);
            }
            Flow flow = runningFlows.remove(id);
            if (flow != null) {
                flowLogService.cancel(stackId, id);
            }
        }
    }

    private void finalizeFlow(String flowId, String flowChainId, Long stackId) {
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.close(stackId, flowId);
        Flow flow = runningFlows.remove(flowId);
        if (flowChainId != null) {
            if (flow.isFlowFailed()) {
                flowChains.removeFullFlowChain(flowChainId);
            } else {
                flowChains.triggerNextFlow(flowChainId);
            }
        }
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get("FLOW_ID");
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get("FLOW_CHAIN_ID");
    }
}
