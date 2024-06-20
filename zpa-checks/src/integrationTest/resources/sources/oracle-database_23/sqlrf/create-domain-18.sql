-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN w2_form AS JSON CONSTRAINT CHECK (VALUE IS JSON VALIDATE USING 
'{ 
  "title": "W2_form",
  "type": "object",
  "properties": {
  "social_security_number": {
  "type": "string",
  "description": "The person social security number."
 },
 "wages": {
 "description": "total wages",
 "type": "number",
 "minimum": 0
 },
 "social_security_wages": {
 "type": "number",
 "description": "wages subject to social security tax" 
 },
 "Federal Income Tax Withheld": {
 "type": "number",
 "description": "withheld of tax to federal income tax"
 },
 "Social Security Tax Withheld": {
 "type": "number",
 "description": "withheld of social security tax"
 }
 },
 "required": ["social_security_number", "wages", "Federal Income Tax Withheld"]
 }'
 );