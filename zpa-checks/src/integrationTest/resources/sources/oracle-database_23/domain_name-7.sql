-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_name.html
SELECT order_id,
       product_id,
       DOMAIN_NAME(amount, currency_code) domain_cols,
       DOMAIN_NAME(currency_code, amount) domain_cols_wrong_order,
       DOMAIN_NAME(order_id, product_id) nondomain_cols,
       DOMAIN_NAME(amount) domain_cols_subset
  FROM co.order_items
  ORDER BY domain_cols;