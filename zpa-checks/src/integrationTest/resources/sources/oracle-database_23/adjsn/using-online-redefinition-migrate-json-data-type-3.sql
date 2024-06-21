-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-online-redefinition-migrate-json-data-type.html
BEGIN
  DBMS_REDEFINITION.start_redef_table('table_owner',
                                      'j_purchaseorder',
                                      'j_purchaseorder_new',
                                      'ID ID, DATE_LOADED DATE_LOADED, JSON(PO_DOCUMENT) PO_DOCUMENT',
                                      REFRESH_DEP_MVIEWS => 'Y',
                                      ENABLE_ROLLBACK => FALSE);
END;
/