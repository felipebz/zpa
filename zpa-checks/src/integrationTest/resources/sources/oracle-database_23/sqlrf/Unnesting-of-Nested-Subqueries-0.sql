-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Unnesting-of-Nested-Subqueries.html
SELECT C.cust_last_name, C.country_id
        FROM customers C
        WHERE EXISTS (SELECT 1
              FROM sales S
              WHERE S.quantity_sold > 1000 and
                    S.cust_id = C.cust_id);