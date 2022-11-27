-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-DATABASE.html
ALTER DATABASE
  ADD LOGFILE GROUP 3 
    ('diska:log3.log' ,  
     'diskb:log3.log') SIZE 50K;