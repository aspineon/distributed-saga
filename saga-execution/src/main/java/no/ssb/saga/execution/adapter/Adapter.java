package no.ssb.saga.execution.adapter;

import no.ssb.saga.api.SagaNode;

import java.util.Map;
import java.util.function.Function;

public class Adapter<I, O> implements SagaAdapter<I, O> {

    protected final Class<O> outputClazz;
    protected final String name;
    private final Function<I, O> action;
    private final Function<I, O> compensatingAction;

    public Adapter(Class<O> outputClazz, String name) {
        this(outputClazz, name, input -> null, input -> null);
    }

    public Adapter(Class<O> outputClazz, String name, Function<I, O> action) {
        this(outputClazz, name, action, input -> null);
    }

    public Adapter(Class<O> outputClazz, String name, Function<I, O> action, Function<I, O> compensatingAction) {
        this.outputClazz = outputClazz;
        this.name = name;
        this.action = action;
        this.compensatingAction = compensatingAction;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public I prepareInputFromDependees(Object startInput, Map<SagaNode, Object> dependeesOutput) {
        return (I) startInput;
    }

    @Override
    public O executeAction(I input) {
        return action.apply(input);
    }

    @Override
    public O executeCompensatingAction(I input) {
        return compensatingAction.apply(input);
    }

    @Override
    public ActionOutputSerializer<O> serializer() {
        return new ActionOutputSerializerToStringAndStringConstructor<O>(outputClazz);
    }
}
