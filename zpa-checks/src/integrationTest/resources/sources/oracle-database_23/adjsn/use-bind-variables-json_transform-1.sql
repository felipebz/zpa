-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/use-bind-variables-json_transform.html
UPDATE customers c
  SET c.data =
      json_transform(c.data,
                     APPEND '$?(@status == "gold").tags' = 'free shipping')
  WHERE json_exists(c.data, '$?(@joined < 2025)');