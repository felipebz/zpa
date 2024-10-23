-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-multivalue-function-based-index.html
SELECT count(*) FROM parts_tab
  WHERE json_exists(jparts, '$.parts.subparts?(@.numberOnly() == 730)');