-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-dataguide.html
EXEC DBMS_JSON.add_virtual_columns('J_PURCHASEORDER', 'PO_DOCUMENT', 100, TRUE);
DESCRIBE j_purchaseorder;
SELECT column_name FROM user_tab_columns
  WHERE table_name = 'J_PURCHASEORDER'
  ORDER BY 1;