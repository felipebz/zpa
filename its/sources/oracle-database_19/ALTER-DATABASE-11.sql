ALTER DATABASE  
    ADD LOGFILE THREAD 5 GROUP 4  
        ('diska:log4.log', 
         'diskb:log4:log');