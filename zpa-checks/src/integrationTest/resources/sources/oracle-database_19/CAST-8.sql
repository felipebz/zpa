-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CAST.html
SELECT CAST(s.addresses AS address_book_t)
  FROM states s 
  WHERE s.state_id = 111;