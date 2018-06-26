package no.ssb.saga.execution.adapter;

import java.util.List;

/**
 * Implementors of adapter must provide a stateless, re-usage adapter that
 * is safe for usage with multiple threads. This should be easy enough as
 * all needed context is passed as arguments to every method.
 */
public interface SagaAdapter {

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
     * in turn be passed to the {@link #executeAction(String) executeAction}
     * or {@link #executeCompensatingAction(String) executeCompensatingAction} method.
     * This method should generally be used to prepare compatible input to the execute methods.
     *
     * @param originalRequestJson the original request data associated with this saga in json format.
     * @param dependeesOutput     the output from the execution of the dependees of the saga-node represented by this adapter.
     * @return a json string representing the input needed to
     * execute this adapter's action or compensating action.
     */
    String prepareJsonInputFromDependees(String originalRequestJson, List<VisitationResult<String>> dependeesOutput);

    /**
     * @param inputJson execute the action that this adapter implicitly represents and use the
     *                  passed json as input.
     * @return the output from executing the action as a json string.
     */
    String executeAction(String inputJson);

    /**
     * @param inputJson execute the compensating action that this adapter implicitly represents
     *                  and use the passed json as input.
     * @return the output from executing the action as a json string.
     */
    String executeCompensatingAction(String inputJson);
}
