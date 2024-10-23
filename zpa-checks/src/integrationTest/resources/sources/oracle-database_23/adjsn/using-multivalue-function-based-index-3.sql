-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-multivalue-function-based-index.html
SELECT a FROM parts_tab
  WHERE json_exists(jparts,'$.parts[*]?(@.partno == 4 &&
                                        @.subparts[1] == 730)');