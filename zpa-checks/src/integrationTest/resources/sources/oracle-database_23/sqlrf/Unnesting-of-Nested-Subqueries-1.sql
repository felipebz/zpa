-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Unnesting-of-Nested-Subqueries.html
SELECT C.cust_last_name, C.country_id
        FROM customers C        
        WHERE C.cust_id =ANY (SELECT S.cust_id
                      FROM sales S
                      WHERE S.quantity_sold > 1000);