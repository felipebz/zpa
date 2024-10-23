-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/persistent-data-guide-information-part-json-search-index.html
ALTER INDEX index_name REBUILD ('dataguide off');
ALTER INDEX index_name REBUILD ('dataguide on');
ALTER TABLE table_name ENABLE CONSTRAINT is_json_check_constraint_name;