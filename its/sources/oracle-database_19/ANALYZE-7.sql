-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ANALYZE.html
SELECT owner_name, table_name, head_rowid, analyze_timestamp 
    FROM chained_rows
    ORDER BY owner_name, table_name, head_rowid, analyze_timestamp; 