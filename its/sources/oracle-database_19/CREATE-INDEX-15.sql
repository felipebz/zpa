-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-INDEX.html
CREATE TABLE rect_tab OF rectangle; 
CREATE INDEX area_idx ON rect_tab x (x.area());