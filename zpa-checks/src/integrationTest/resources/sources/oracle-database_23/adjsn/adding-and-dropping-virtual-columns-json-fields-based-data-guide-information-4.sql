-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/adding-and-dropping-virtual-columns-json-fields-based-data-guide-information.html
EXEC DBMS_JSON.add_virtual_columns('J_PURCHASEORDER', 'DATA', 100, TRUE);
DESCRIBE j_purchaseorder;
SELECT column_name FROM user_tab_columns
  WHERE table_name = 'J_PURCHASEORDER'
  ORDER BY 1;