package com.datamountaineer.streamreactor.connect.jdbc.sink.writer;


import com.datamountaineer.streamreactor.connect.jdbc.sink.config.ErrorPolicyEnum;
import org.apache.kafka.connect.sink.SinkRecord;

import java.sql.Connection;
import java.util.Collection;

/**
 * Defines the approach to take when the db operation fails inserting the new records
 */
public interface ErrorHandlingPolicy {
    /**
     * Called when the sql operation to insert the SinkRecords fails
     *
     * @param records    The list of records received by the SinkTask
     * @param error      - The error raised when the insert failed
     * @param connection - The database connection instance
     */
    void handle(Collection<SinkRecord> records, final Throwable error, final Connection connection);
}

/**
 * Helper class for creating new instances of ErrorHandlingPolicy.
 */
final class ErrorHandlingPolicyHelper{
    /**
     * Creates a new instance of ErrorHandling policy given the enum value.
     * @param value - The enum value specifying how errors should be handled during a database data insert.
     * @return new instance of ErrorHandlingPolicy
     */
    public  static ErrorHandlingPolicy from(final ErrorPolicyEnum value){
        switch (value) {
            case NOOP:
                return new NoopErrorHandlingPolicy();

            case THROW:
                return new ThrowErrorHandlingPolicy();

            default:
                throw new RuntimeException(value + " error handling policy is not recognized.");
        }

    }
}

