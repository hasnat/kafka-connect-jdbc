package com.datamountaineer.streamreactor.connect.jdbc.sink.writer;

import com.datamountaineer.streamreactor.connect.Pair;
import com.datamountaineer.streamreactor.connect.jdbc.sink.binders.PreparedStatementBinder;
import com.datamountaineer.streamreactor.connect.jdbc.sink.StructFieldsDataExtractor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a sql statement for all records sharing the same columns to be inserted.
 */
public final class BatchedPreparedStatementBuilder implements PreparedStatementBuilder {
    private static final Logger logger = LoggerFactory.getLogger(BatchedPreparedStatementBuilder.class);

    private final String tableName;
    private final StructFieldsDataExtractor fieldsExtractor;


    /**
     * @param tableName       - The name of the database tabled
     * @param fieldsExtractor - An instance of the SinkRecord fields value extractor
     */
    public BatchedPreparedStatementBuilder(String tableName, StructFieldsDataExtractor fieldsExtractor) {
        this.tableName = tableName;
        this.fieldsExtractor = fieldsExtractor;
    }

    /**
     * @param records    - The sequence of records to be inserted to the database
     * @param connection - The database connection instance
     * @return A sequence of PreparedStatement to be executed. It will batch the sql operation.
     */
    @Override
    public List<PreparedStatement> build(final Collection<SinkRecord> records, final Connection connection) throws SQLException {

        final Map<String, PreparedStatement> mapStatements = new HashMap<>();
        for (final SinkRecord record : records) {
            logger.debug("Received record from topic:%s partition:%d and offset:$d", record.topic(), record.kafkaPartition(), record.kafkaOffset());
            if (record.value() == null || record.value().getClass() != Struct.class)
                throw new IllegalArgumentException("The SinkRecord payload should be of type Struct");

            final List<Pair<String, PreparedStatementBinder>> fieldsAndBinders = fieldsExtractor.get((Struct) record.value());

            if (!fieldsAndBinders.isEmpty()) {
                final List<String> columns = Lists.transform(fieldsAndBinders, new Function<Pair<String, PreparedStatementBinder>, String>() {
                    @Override
                    public String apply(Pair<String, PreparedStatementBinder> input) {
                        return input.first;
                    }
                });
                final String statementKey = Joiner.on("").join(columns);


                final List<PreparedStatementBinder> binders = Lists.transform(fieldsAndBinders, new Function<Pair<String, PreparedStatementBinder>, PreparedStatementBinder>() {
                    @Override
                    public PreparedStatementBinder apply(Pair<String, PreparedStatementBinder> input) {
                        return input.second;
                    }
                });

                if (!mapStatements.containsKey(statementKey)) {
                    final String query = BuildInsertQuery.apply(tableName, columns);
                    final PreparedStatement statement = connection.prepareStatement(query);
                    PreparedStatementBindData.apply(statement, binders);
                    statement.addBatch();
                    mapStatements.put(statementKey, statement);
                } else {
                    final PreparedStatement statement = mapStatements.get(statementKey);
                    PreparedStatementBindData.apply(statement, binders);
                    statement.addBatch();
                }
            }
        }

        return Lists.newArrayList(mapStatements.values());
    }
}
