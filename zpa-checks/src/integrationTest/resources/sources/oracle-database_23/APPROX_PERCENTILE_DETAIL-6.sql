-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_PERCENTILE_DETAIL.html
SELECT country,
       TO_APPROX_PERCENTILE(APPROX_PERCENTILE_AGG(city_detail), .25, 'NUMBER') "25th Percentile"
FROM amt_sold_by_city_mv
GROUP BY country
ORDER BY country;