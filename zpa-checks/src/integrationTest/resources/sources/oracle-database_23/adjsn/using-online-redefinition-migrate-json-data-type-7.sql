-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-online-redefinition-migrate-json-data-type.html
BEGIN
  DBMS_REDEFINITION.finish_redef_table('table_owner',
                                       'j_purchaseorder',
                                       'j_purchaseorder_new',
                                       DML_LOCK_TIMEOUT => 0);
END;
/