-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check_type.html
SELECT order_id,
       product_id,
       amount,
       currency_code,
       DOMAIN_CHECK_TYPE(currency, order_id, product_id) order_product,
       DOMAIN_CHECK_TYPE(currency, amount, currency_code) amount_currency,
       DOMAIN_CHECK_TYPE(currency, currency_code, amount) currency_amount,
       DOMAIN_CHECK_TYPE(currency, order_id, currency_code) order_currency
  FROM order_items;