-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN days_of_week AS
  ENUM (
    Sunday     = Su  = 0,     
    Monday     = Mo,
    Tuesday    = Tu,
    Wednesday  = We,
    Thursday   = Th,
    Friday     = Fr,
    Saturday   = Sa
  );