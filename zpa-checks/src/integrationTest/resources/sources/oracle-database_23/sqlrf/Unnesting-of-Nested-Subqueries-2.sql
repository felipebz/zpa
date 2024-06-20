-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Unnesting-of-Nested-Subqueries.html
SELECT C.cust_last_name, C.country_id
FROM customers C
WHERE NOT EXISTS (SELECT 1
                  FROM sales S, products P
                  WHERE P.prod_id = S.prod_id and
                        P.prod_min_price > 90 and
                        S.cust_id = C.cust_id);