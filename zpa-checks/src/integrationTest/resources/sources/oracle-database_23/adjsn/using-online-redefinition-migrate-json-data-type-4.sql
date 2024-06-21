-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-online-redefinition-migrate-json-data-type.html
DECLARE
n_errors INTEGER;
BEGIN
  DBMS_REDEFINITION.copy_table_dependents('table_owner',
                                          'j_purchaseorder',
                                          'j_purchaseorder_new',
                                          NUM_ERRORS => n_errors,
                                          IGNORE_ERRORS => FALSE);
  DBMS_OUTPUT.put_line(n_errors);
END;
/