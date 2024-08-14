-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
CREATE VIEW department_view AS 
  SELECT  dep.department_id id,
          JSON {'id'                : dep.department_id,
                'departmentName'    : dep.department_name,
                'departmentAddress' : JSON {'street'  : loc.street_address,
                                            'zip'     : loc.postal_code,
                                            'city'    : loc.city,
                                            'state'   : loc.state_province,
                                            'country' : loc.country_id},
                'employees'         : [ SELECT
                                          JSON {'id'    : emp.employee_id,
                                                'name'  : emp.first_name || ' ' || emp.last_name,
                                                'title' : (SELECT job_title 
                                                             FROM jobs job 
                                                             WHERE job.job_id = emp.job_id)}
                                          FROM employees emp 
                                          WHERE emp.department_id = dep.department_id ]} data
    FROM departments dep, locations loc
    WHERE dep.location_id = loc.location_id;