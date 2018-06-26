package no.ssb.saga.execution.adapter;

import no.ssb.saga.api.Saga;

import java.util.List;

class SagaAdapterStart implements SagaAdapter {

    @Override
    public String name() {
        return Saga.ADAPTER_START;
    }

    @Override
    public String prepareJsonInputFromDependees(String originalRequestJson, List<VisitationResult<String>> dependeesOutput) {
        return originalRequestJson;
    }

    @Override
    public String executeAction(String inputJson) {
        return "{}";
    }

    @Override
    public String executeCompensatingAction(String inputJson) {
        return "{}";
    }
}
