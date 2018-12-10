PACKAGE Const_local IS
/*
|| Name        : Const_local
|| Description : This Package is the local Form-Constant-Package
||
|| Version   Updates     Author    Change-Description
|| -------   ----------  -------   ------------------
|| 1.0.001   20.07.2007, Volberg - Created
*/

-- Blocks
  blk_Control                    CONSTANT VARCHAR2 (30) := upper ('Control');
  blk_Filter                     CONSTANT VARCHAR2 (30) := upper ('Filter');
  blk_Mainblock                  CONSTANT VARCHAR2 (30) := upper ('Employees');

-- One Time Timer
  ott_Query                      CONSTANT VARCHAR2 (40) := upper ('Query');

END;
