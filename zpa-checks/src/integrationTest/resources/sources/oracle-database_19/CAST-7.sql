-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CAST.html
SELECT s.custno, s.name,
       CAST(MULTISET(SELECT ca.street_address,   
                            ca.postal_code, 
                            ca.city, 
                            ca.state_province, 
                            ca.country_id
                       FROM cust_address ca
                       WHERE s.custno = ca.custno)
       AS address_book_t)
  FROM cust_short s
  ORDER BY s.custno;