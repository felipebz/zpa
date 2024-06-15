-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN temperature AS NUMBER(3)
ANNOTATIONS (display_units '{ "units": ["celsius", "fahrenheit"] }');