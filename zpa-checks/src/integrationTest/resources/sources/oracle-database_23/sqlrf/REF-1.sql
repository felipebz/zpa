-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REF.html
CREATE TABLE addresses OF cust_address_typ;
INSERT INTO addresses VALUES (
   '123 First Street', '4GF H1J', 'Our Town', 'Ourcounty', 'US');
SELECT REF(e) FROM addresses e;