-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html
CREATE DOMAIN order_status AS
  ENUM (
    New ,     
    Open ,
    Shipped ,
    Closed ,
    Cancelled
  );