-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Object-Access-Expressions.html
CREATE TABLE short_orders (
   sales_rep VARCHAR2(25), item order_item_typ);

UPDATE short_orders s SET sales_rep = 'Unassigned';

SELECT o.item.line_item_id, o.item.quantity FROM short_orders o;