-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
CREATE OR REPLACE VIEW sales_view_ref AS
  SELECT country_name country,
         prod_name prod,
         calendar_year year,
         SUM(amount_sold) sale,
         COUNT(amount_sold) cnt
    FROM sales,times,customers,countries,products
    WHERE sales.time_id = times.time_id
      AND sales.prod_id = products.prod_id
      AND sales.cust_id = customers.cust_id
      AND customers.country_id = countries.country_id
      AND ( customers.country_id = 52779
            OR customers.country_id = 52776 )
      AND ( prod_name = 'Standard Mouse'
            OR prod_name = 'Mouse Pad' )
    GROUP BY country_name,prod_name,calendar_year;
SELECT country, prod, year, sale
  FROM sales_view_ref
  ORDER BY country, prod, year;