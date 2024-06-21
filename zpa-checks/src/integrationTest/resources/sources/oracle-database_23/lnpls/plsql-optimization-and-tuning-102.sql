-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
CREATE OR REPLACE FUNCTION get_external_source_data
  RETURN t PIPELINED AUTHID DEFINER IS
BEGIN
  External_Source.Init();           -- Initialize.
  <<b>> BEGIN
    LOOP                            -- Pipe rows from external source.
      PIPE ROW (External_Source.Next_Row());
    END LOOP;
  EXCEPTION
    WHEN External_Source.Done THEN  -- When no more rows are available,
      External_Source.Clean_Up();   --  clean up.
    WHEN NO_DATA_NEEDED THEN        -- When no more rows are needed,
      External_Source.Clean_Up();   --  clean up.
      RAISE NO_DATA_NEEDED;           -- Optional, equivalent to RETURN.
  END b;
END get_external_source_data;
/