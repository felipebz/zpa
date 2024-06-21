-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-online-redefinition-migrate-json-data-type.html
EXEC DBMS_REDEFINITION.can_redef_table('table_owner', 'j_purchaseorder');