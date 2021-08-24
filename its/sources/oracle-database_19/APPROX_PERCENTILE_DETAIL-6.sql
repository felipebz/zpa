SELECT country,
       TO_APPROX_PERCENTILE(APPROX_PERCENTILE_AGG(city_detail), .25, 'NUMBER') "25th Percentile"
FROM amt_sold_by_city_mv
GROUP BY country
ORDER BY country;