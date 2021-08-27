-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/APPROX_COUNT_DISTINCT_DETAIL.html
CREATE MATERIALIZED VIEW annual_prod_count_mv AS
  SELECT year,
         APPROX_COUNT_DISTINCT_AGG(daily_detail) annual_detail
  FROM daily_prod_count_mv
  GROUP BY year;