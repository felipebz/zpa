-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/analytic-view-measure-expressions.html
SELECT geography_hier.member_name AS "Region", 
       units AS "Units", 
       units_geog_rank_level AS "Rank"
  FROM ANALYTIC VIEW (
    USING sales_av HIERARCHIES (geography_hier)
    ADD MEASURES (
      units_geog_rank_level AS (
        RANK() OVER (
          HIERARCHY geography_hier
          ORDER BY units desc nulls last
          WITHIN LEVEL))
    )
  )
  WHERE geography_hier.level_name IN ('REGION')
  ORDER BY units_geog_rank_level;