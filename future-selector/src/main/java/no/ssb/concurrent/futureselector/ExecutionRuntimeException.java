package no.ssb.concurrent.futureselector;

public class ExecutionRuntimeException extends RuntimeException {
    public ExecutionRuntimeException() {
        super();
    }

    public ExecutionRuntimeException(String message) {
        super(message);
    }

    public ExecutionRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionRuntimeException(Throwable cause) {
        super(cause);
    }
}
