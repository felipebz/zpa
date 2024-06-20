-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-SYSTEM.html
SELECT SYS_CONTEXT('SYS_CLUSTER_PROPERTIES', 'CURRENT_PATCHLVL') FROM DUAL;