-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-dataguide.html
EXEC DBMS_STATS.gather_index_stats(docuser, po_search_idx, NULL, 100);