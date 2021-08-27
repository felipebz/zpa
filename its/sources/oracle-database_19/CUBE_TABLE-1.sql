-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CUBE_TABLE.html
SELECT dim_key, level_name, long_description, channel_total_id tot_id,
       channel_channel_id chan_id, channel_long_description chan_desc,
       total_long_description tot_desc
  FROM TABLE(CUBE_TABLE('global.channel'));