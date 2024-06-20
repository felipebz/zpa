-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-MATERIALIZED-ZONEMAP.html
ALTER MATERIALIZED ZONEMAP sales_zmap
  PCTFREE 20 PCTUSED 50 NOCACHE;