CREATE TABLE empl_h  
  (  
     employee_id  NUMBER(6) PRIMARY KEY,  
     first_name   VARCHAR2(20),  
     last_name    VARCHAR2(25),  
     email        VARCHAR2(25),  
     phone_number VARCHAR2(20),  
     hire_date    DATE DEFAULT SYSDATE,  
     job_id       VARCHAR2(10),  
     salary       NUMBER(8, 2),  
     part_name    VARCHAR2(25)  
  ) PARTITION BY RANGE (hire_date) (  
PARTITION hire_q1 VALUES less than(to_date('01-APR-2014', 'DD-MON-YYYY')),   
PARTITION hire_q2 VALUES less than(to_date('01-JUL-2014', 'DD-MON-YYYY')),   
PARTITION hire_q3 VALUES less than(to_date('01-OCT-2014', 'DD-MON-YYYY')),   
PARTITION hire_q4 VALUES less than(to_date('01-JAN-2015', 'DD-MON-YYYY'))  
);