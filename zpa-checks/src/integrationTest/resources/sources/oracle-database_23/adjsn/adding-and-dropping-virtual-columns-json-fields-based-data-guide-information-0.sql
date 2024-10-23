-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/adding-and-dropping-virtual-columns-json-fields-based-data-guide-information.html
DECLARE
  dg CLOB;
BEGIN
  SELECT json_dataguide(po_document, DBMS_JSON.FORMAT_HIERARCHICAL) INTO dg
    FROM j_purchaseorder;
  DBMS_JSON.add_virtual_columns('J_PURCHASEORDER',
                                'PO_DOCUMENT',
                                dg,
                                resolveNameConflicts=>TRUE,
                                colNamePrefix=>'PO_DOCUMENT$',
                                mixedCaseColumns=>TRUE);
END;
/
DESCRIBE j_purchaseorder;