-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/sql-json-path-expression-syntax.html
@.locations?( @.country in ("France", "Germany") )
@.locations?( !(@.country in ("France", "Germany")) )
@.locations?( exists(@.country)