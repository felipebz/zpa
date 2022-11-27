PACKAGE BODY BL_Employees IS

  PROCEDURE PRQ IS
  BEGIN
    :Employees.Employee_ID := :Filter.TI_Employee_ID;
    :Employees.First_Name  := '%' || :Filter.TI_First_Name || '%';
    :Employees.Last_Name   := '%' || :Filter.TI_Last_Name  || '%';
    :Employees.Email       := '%' || :Filter.TI_Email      || '%';
  END;

  PROCEDURE POQ IS
  BEGIN
    NULL;
  END;

  PROCEDURE PRI IS
  BEGIN
    NULL;
  END;

  PROCEDURE PRU IS
  BEGIN
    NULL;
  END;

  PROCEDURE PRD IS
  BEGIN
    NULL;
  END;

  PROCEDURE WVR IS
  BEGIN
    NULL;
  END;

  PROCEDURE Initialize IS
  BEGIN
    NULL;
  END;

  PROCEDURE Destroy IS
  BEGIN
    NULL;
  END;

  PROCEDURE Query IS
  BEGIN
    NULL;
  END;

END;
