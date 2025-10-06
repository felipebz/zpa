-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-ANALYTIC-VIEW.html
ALTER ANALYTIC VIEW TKHCSGL308_UNITS_AVIEW_CACHE ADD CACHE
    MEASURE GROUP (sales, units, cost)
    LEVELS (TIME.FISCAL.FISCAL_QUARTER, WAREHOUSE);