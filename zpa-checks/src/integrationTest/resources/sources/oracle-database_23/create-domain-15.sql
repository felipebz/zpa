-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN lodging_details AS
    (
      hotel AS VARCHAR2(100) NOT NULL,
      nights_count AS NUMBER
    )
    CONSTRAINT lodging_c
     CHECK (hotel IS NOT NULL AND nights_count > 0)
    DISPLAY hotel||', '||nights_count;