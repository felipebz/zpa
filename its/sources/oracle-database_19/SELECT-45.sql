SELECT country, year, sale, csum
   FROM 
   (SELECT country, year, SUM(sale) sale
    FROM sales_view_ref
    GROUP BY country, year
   )
   MODEL DIMENSION BY (country, year)
         MEASURES (sale, 0 csum) 
         RULES (csum[any, any]= 
                  SUM(sale) OVER (PARTITION BY country 
                                  ORDER BY year 
                                  ROWS UNBOUNDED PRECEDING) 
                )
   ORDER BY country, year;