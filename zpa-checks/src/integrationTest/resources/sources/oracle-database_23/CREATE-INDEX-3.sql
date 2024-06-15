-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html
ALTER TABLE table ADD (new_hash_column AS (STANDARD_HASH(column)));
CREATE INDEX index ON table (new_hash_column);