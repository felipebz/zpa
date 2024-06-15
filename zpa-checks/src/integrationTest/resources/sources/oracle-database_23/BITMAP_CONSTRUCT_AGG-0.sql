-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/BITMAP_CONSTRUCT_AGG.html
62577, 00000, "The bitmap size exceeds maximum size of its SQL data type."
// *Cause: An attempt was made to construct a bitmap larger than its maximum SQL type size.
// *Action: Break the input to BITMAP_CONSTRUCT_AGG into smaller ranges.