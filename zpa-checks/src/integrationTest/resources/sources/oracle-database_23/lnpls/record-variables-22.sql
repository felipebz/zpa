-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/record-variables.html
DROP TABLE plch_departure;
CREATE TABLE plch_departure (
  destination    VARCHAR2(100),
  departure_time DATE,
  delay          NUMBER(10),
  expected       GENERATED ALWAYS AS (departure_time + delay/24/60/60)
);
DECLARE
 dep_rec plch_departure%ROWTYPE;
BEGIN
  dep_rec.destination := 'X'; 
  dep_rec.departure_time := SYSDATE;
  dep_rec.delay := 1500;

  INSERT INTO plch_departure VALUES dep_rec;
END;
/