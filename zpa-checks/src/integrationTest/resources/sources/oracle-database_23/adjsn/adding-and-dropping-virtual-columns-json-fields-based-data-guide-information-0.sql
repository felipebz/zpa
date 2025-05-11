-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/adding-and-dropping-virtual-columns-json-fields-based-data-guide-information.html
DECLARE
  dg CLOB;
BEGIN
  SELECT json_dataguide(data, DBMS_JSON.FORMAT_HIERARCHICAL) INTO dg
    FROM j_purchaseorder;
  DBMS_JSON.add_virtual_columns('J_PURCHASEORDER',
                                'DATA',
                                dg,
                                resolveNameConflicts=>TRUE,
                                colNamePrefix=>'DATA$',
                                mixedCaseColumns=>TRUE);
END;
/
DESCRIBE j_purchaseorder;