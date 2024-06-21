-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-dataguide.html
EXEC DBMS_JSON.drop_virtual_columns('J_PURCHASEORDER', 'PO_DOCUMENT');