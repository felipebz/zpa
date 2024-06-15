-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_order.html
SELECT order_id,
       product_id,
       DOMAIN_ORDER(amount, currency_code) domain_cols,
       DOMAIN_ORDER(currency_code, amount) domain_cols_wrong_order,
       DOMAIN_ORDER(order_id, product_id) nondomain_cols,
       DOMAIN_ORDER(amount) domain_cols_subset
  FROM order_items
  ORDER BY domain_cols;