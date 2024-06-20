-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Multiset-Operators.html
UPDATE customers_demo cd
  SET cust_address_ntab = 
    CAST(MULTISET(SELECT cust_address
                    FROM customers c
                    WHERE c.customer_id =
                          cd.customer_id) as cust_address_tab_typ);

UPDATE customers_demo cd
  SET cust_address2_ntab = 
    CAST(MULTISET(SELECT cust_address
                    FROM customers c
                    WHERE c.customer_id =
                          cd.customer_id) as cust_address_tab_typ);