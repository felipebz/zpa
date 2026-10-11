CREATE OR REPLACE PACKAGE shop AS
  TYPE order_t IS RECORD (
    id       NUMBER,
    customer VARCHAR2(40),
    total    NUMBER(10,2),
    note     orders.note%TYPE
  );
  SUBTYPE money_t IS NUMBER;

  PROCEDURE place(o IN order_t, notify IN BOOLEAN DEFAULT TRUE);
  PROCEDURE place(id IN NUMBER);
  FUNCTION total(o IN order_t) RETURN money_t DETERMINISTIC;
  PROCEDURE audit(msg IN OUT NOCOPY VARCHAR2);
END shop;
/
