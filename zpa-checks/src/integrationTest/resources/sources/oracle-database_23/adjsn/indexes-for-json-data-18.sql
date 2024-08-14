-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
SELECT count(*) FROM parts_tab t
  WHERE json_exists(jparts, '$.parts.subparts?(@.number() == 730)');
SELECT count(*) FROM parts_tab t
  WHERE json_exists(jparts, '$.parts.subparts?(@ == 730)');