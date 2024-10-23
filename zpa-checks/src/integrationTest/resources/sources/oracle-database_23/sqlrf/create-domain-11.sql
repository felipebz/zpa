-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN flight_details AS
  (
   flight_num AS VARCHAR2(100) NOT NULL,
   origin AS VARCHAR2(200)
     CONSTRAINT origin_3_char_c CHECK (LENGTH(origin) = 3),
   destination AS VARCHAR2(200)
     CONSTRAINT dest_3_char_c CHECK (LENGTH(destination) = 3)
  )
  CONSTRAINT flight_c
    CHECK
      (
        flight_num LIKE '%-%' AND
        origin IS NOT NULL AND
        destination IS NOT NULL
      )
  CONSTRAINT origin_dest_different_c
    CHECK (origin <> destination)
  DISPLAY flight_num||', '||origin||', '||destination
  ORDER flight_num||destination;