-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-INDEX.html
/* Unless you first sort the table oe.orders, this example fails
   because you cannot specify NOSORT unless the base table is
   already sorted.
*/
CREATE INDEX ord_customer_ix_demo
   ON orders (order_mode)
   NOSORT
   NOLOGGING;