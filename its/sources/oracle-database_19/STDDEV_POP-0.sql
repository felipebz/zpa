SELECT STDDEV_POP(amount_sold) "Pop", 
   STDDEV_SAMP(amount_sold) "Samp"
   FROM sales;