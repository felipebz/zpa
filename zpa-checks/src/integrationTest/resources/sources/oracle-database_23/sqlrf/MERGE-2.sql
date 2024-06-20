-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MERGE.html
MERGE INTO people_target pt 
USING people_source ps 
ON    (pt.person_id = ps.person_id) 
WHEN MATCHED THEN UPDATE 
  SET pt.first_name = ps.first_name, 
      pt.last_name = ps.last_name, 
      pt.title = ps.title;