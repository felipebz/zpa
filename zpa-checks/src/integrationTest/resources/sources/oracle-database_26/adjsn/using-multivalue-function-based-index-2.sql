-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/using-multivalue-function-based-index.html
SELECT a FROM parts_tab
  WHERE json_exists(jparts,'$.parts[*]?(@.partno == 4 &&
                                        @.subparts == 730)');