-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/BIN_TO_NUM.html
SELECT order_status
  FROM orders
  WHERE order_id = 2441;
DECLARE
  warehouse NUMBER := 1;
  ground    NUMBER := 1;
  insured   NUMBER := 1;
  result    NUMBER;
BEGIN
  SELECT BIN_TO_NUM(warehouse, ground, insured) INTO result FROM DUAL;
  UPDATE orders SET order_status = result WHERE order_id = 2441;
END;
/
SELECT order_status
  FROM orders
  WHERE order_id = 2441;