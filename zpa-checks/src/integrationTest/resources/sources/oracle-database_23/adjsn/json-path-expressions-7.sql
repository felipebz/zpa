-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-path-expressions.html
@.locations?( @.country in ("France", "Germany") )

@.locations?( !(@.country in ("France", "Germany")) )

@.locations?( exists(@.country)
