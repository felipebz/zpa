-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/file_specification.html
ALTER DATABASE ADD LOGFILE GROUP 5
     ('4k_disk_a:log5.log', '4k_disk_b:log5.log')
     SIZE 100M BLOCKSIZE 4096 REUSE;