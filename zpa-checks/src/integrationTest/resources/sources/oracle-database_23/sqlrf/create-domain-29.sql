-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN order_status AS
  ENUM (
    New ,     
    Open ,
    Shipped ,
    Closed ,
    Cancelled
  );