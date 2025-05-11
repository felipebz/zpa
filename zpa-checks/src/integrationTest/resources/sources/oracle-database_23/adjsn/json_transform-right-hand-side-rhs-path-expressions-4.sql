-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-right-hand-side-rhs-path-expressions.html
SELECT json_transform('{"items":[ {"quantity":2, "unitPrice":3},
                                           {"quantity":2, "unitPrice":7} ]}',
                      SET '$priceVar' = PATH '0.00',
                      NESTED PATH '$.items[*]'
                        (SET '$priceVar' =
                             PATH '$priceVar + (@.unitPrice * @.quantity)'),
                      SET '$.totalPrice' = PATH '$priceVar');