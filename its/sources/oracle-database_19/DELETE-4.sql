DELETE FROM sales PARTITION (sales_q1_1998)
   WHERE amount_sold > 1000;