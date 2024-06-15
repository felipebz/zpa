-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_display.html
SELECT order_id,
       product_id,
       DOMAIN_DISPLAY(amount, currency_code) domain_cols,
       DOMAIN_DISPLAY(currency_code, amount) domain_cols_wrong_order,
       DOMAIN_DISPLAY(order_id, product_id) nondomain_cols,
       DOMAIN_DISPLAY(amount) domain_cols_subset
  FROM order_items;