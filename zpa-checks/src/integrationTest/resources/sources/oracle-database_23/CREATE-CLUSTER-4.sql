-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-CLUSTER.html
CREATE CLUSTER address
   (postal_code NUMBER, country_id CHAR(2))
   HASHKEYS 20
   HASH IS MOD(postal_code + country_id, 101);