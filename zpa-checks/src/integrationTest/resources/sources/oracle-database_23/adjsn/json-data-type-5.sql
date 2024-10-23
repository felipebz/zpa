-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-data-type.html
CREATE TABLE t (tz TIMESTAMP WITH TIME ZONE);
  INSERT INTO t
    VALUES (to_timestamp_tz('2019-05-03 20:00:00 -8:30',
                            'YYYY-MM-DD HH24:MI:SS TZH:TZM'));
SELECT json_scalar(tz) FROM t;
SELECT json_object('timestamp'       : json_scalar(tz),
                   'timezoneHours'   : extract(TIMEZONE_HOUR FROM tz),
                   'timezoneMinutes' : extract(TIMEZONE_MINUTE FROM tz))
  FROM t;