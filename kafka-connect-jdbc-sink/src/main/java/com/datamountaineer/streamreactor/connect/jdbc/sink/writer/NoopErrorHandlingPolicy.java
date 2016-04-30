package com.datamountaineer.streamreactor.connect.jdbc.sink.writer;

import org.apache.kafka.connect.sink.SinkRecord;

import java.sql.Connection;
import java.util.Collection;

/**
 * The policy swallows the exception
 */
public final class NoopErrorHandlingPolicy implements ErrorHandlingPolicy {
    @Override
    public void handle(Collection<SinkRecord> records, final Throwable error, final Connection connection) {
        //Do nothing
    }
}
