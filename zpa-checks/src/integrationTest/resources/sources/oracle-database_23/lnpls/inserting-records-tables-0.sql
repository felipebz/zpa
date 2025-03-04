-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/inserting-records-tables.html
DROP TABLE schedule;
CREATE TABLE schedule (
  week  NUMBER,
  Mon   VARCHAR2(10),
  Tue   VARCHAR2(10),
  Wed   VARCHAR2(10),
  Thu   VARCHAR2(10),
  Fri   VARCHAR2(10),
  Sat   VARCHAR2(10),
  Sun   VARCHAR2(10)
);
DECLARE
  default_week  schedule%ROWTYPE;
  i             NUMBER;
BEGIN
  default_week.Mon := '0800-1700';
  default_week.Tue := '0800-1700';
  default_week.Wed := '0800-1700';
  default_week.Thu := '0800-1700';
  default_week.Fri := '0800-1700';
  default_week.Sat := 'Day Off';
  default_week.Sun := 'Day Off';

  FOR i IN 1..6 LOOP
    default_week.week    := i;

    INSERT INTO schedule VALUES default_week;
  END LOOP;
END;
/
COLUMN week FORMAT 99

COLUMN Mon  FORMAT A9

COLUMN Tue  FORMAT A9

COLUMN Wed  FORMAT A9

COLUMN Thu  FORMAT A9

COLUMN Fri  FORMAT A9

COLUMN Sat  FORMAT A9

COLUMN Sun  FORMAT A9

SELECT * FROM schedule;