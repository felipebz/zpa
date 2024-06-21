-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/handling-dependent-objects.html
SELECT DBMS_METADATA.get_ddl('INDEX', po_num_idx1) FROM DUAL;