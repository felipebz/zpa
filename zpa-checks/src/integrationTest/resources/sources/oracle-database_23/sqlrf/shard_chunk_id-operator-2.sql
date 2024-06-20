-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/shard_chunk_id-operator.html
SELECT SHARD_CHUNK_ID(null, class, custno, name) FROM customers;