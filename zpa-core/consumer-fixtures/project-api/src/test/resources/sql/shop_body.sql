CREATE OR REPLACE PACKAGE BODY shop AS
  PROCEDURE place(o IN order_t, notify IN BOOLEAN) IS
  BEGIN
    NULL;
  END place;

  FUNCTION total(o IN order_t) RETURN money_t IS
  BEGIN
    RETURN o.total;
  END total;

  PROCEDURE hidden IS
  BEGIN
    NULL;
  END hidden;
END shop;
/
