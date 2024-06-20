-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLPARSE.html
SELECT XMLPARSE(CONTENT '124 <purchaseOrder poNo="12435"> 
   <customerName> Acme Enterprises</customerName>
   <itemNo>32987457</itemNo>
   </purchaseOrder>' 
WELLFORMED) AS PO FROM DUAL;