-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-query-rewrite-use-materialized-view-json_table.html
CREATE INDEX mv_idx ON mv_for_query_rewrite(userid,
                                            upc_code,
                                            quantity);