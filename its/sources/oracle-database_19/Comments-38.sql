-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ OPT_PARAM('star_transformation_enabled' 'true') */ *
  FROM ... ;