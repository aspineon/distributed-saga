import no.ssb.saga.samples.polyglot.sagalog.FileSagaLogInitializer;
import no.ssb.sagalog.SagaLogInitializer;

module no.ssb.saga.samples.polyglot {
    requires no.ssb.saga.api;
    requires no.ssb.saga.execution;
    requires no.ssb.saga.serialization;
    requires no.ssb.concurrent.futureselector;
    requires java.base;
    requires java.net.http;
    requires undertow.core;
    requires org.json;
    requires tape;
    requires no.ssb.sagalog;

    provides SagaLogInitializer with FileSagaLogInitializer;

    uses SagaLogInitializer;
}
