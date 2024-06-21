-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/handling-dependent-objects.html
SELECT INDEX_NAME FROM USER_INDEXES
  WHERE TABLE_NAME = 'J_PURCHASEORDER';