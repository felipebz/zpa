-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-PLUGGABLE-DATABASE.html
SELECT partition_name, high_value
    FROM dba_tab_partitions
    WHERE table_name='MAP' AND table_owner='SYS'