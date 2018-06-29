package no.ssb.saga.execution.adapter;

import no.ssb.saga.api.SagaNode;

import java.util.Map;

/**
 * Implementors of adapter must provide a stateless, re-usage adapter that
 * is safe for usage with multiple threads. This should be easy enough as
 * all needed context is passed as arguments to every method.
 */
public interface SagaAdapter<INPUT, OUTPUT> {

    /**
     * The adapter name as it should appear in the saga-log. Note that this name
     * will be used for serialization and de-serialization. This means that care
     * must be taken if the name is changed for an adapter that has already been
     * used for serialization or saga-log operations.
     *
     * @return the adapter name.
     */
    String name();

    /**
     * Prepare input needed for processing of action or compensating action
     * of the saga-node represented by this adapter. The prepared input should
     * in turn be passed to the {@link #executeAction(INPUT) executeAction}
     * or {@link #executeCompensatingAction(INPUT) executeCompensatingAction} method.
     * This method should generally be used to prepare input that is compatible with
     * the execute methods.
     *
     * @param sagaRequestData the original request data associated with this saga.
     * @param dependeesOutput the output from the execution of the dependees of the saga-node
     *                        represented by this adapter.
     * @return the input to be passed when this adapter's action or compensating action is called.
     */
    INPUT prepareInputFromDependees(Object sagaRequestData, Map<SagaNode, Object> dependeesOutput);

    /**
     * @param input execute the action that this adapter implicitly represents using input.
     * @return the output from executing the action as a json string.
     */
    OUTPUT executeAction(INPUT input);

    /**
     * @param input execute the compensating action that this adapter implicitly represents using input.
     * @return the output from executing the action as a json string.
     */
    OUTPUT executeCompensatingAction(INPUT input);

    /**
     * @return a serializer than can be used to serialize and de-serialize any object output
     * by one of the action execute methods.
     */
    ActionOutputSerializer<OUTPUT> serializer();
}
