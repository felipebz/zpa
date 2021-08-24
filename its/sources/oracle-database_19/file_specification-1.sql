ALTER DATABASE payable 
   ADD LOGFILE GROUP 3 ('diska:log3.log', 'diskb:log3.log') 
   SIZE 50K REUSE;