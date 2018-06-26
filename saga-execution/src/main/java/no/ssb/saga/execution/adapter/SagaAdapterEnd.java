package no.ssb.saga.execution.adapter;

import no.ssb.saga.api.Saga;

import java.util.List;

class SagaAdapterEnd implements SagaAdapter {

    @Override
    public String name() {
        return Saga.ADAPTER_END;
    }

    @Override
    public String prepareJsonInputFromDependees(String originalRequestJson, List<VisitationResult<String>> dependeesOutput) {
        return "{}";
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
