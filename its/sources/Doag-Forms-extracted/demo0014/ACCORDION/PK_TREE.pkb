PACKAGE BODY PK_TREE IS
 
  itTree ITEM:=FIND_ITEM('BL_TREE.IT_TREE');
 
  FUNCTION FK_ADD_NODE(i_ndMaster IN FTree.NODE,
                       i_vcValue  IN VARCHAR2,
                       i_vcLabel  IN VARCHAR2,
                       i_vcIcon   IN VARCHAR2 DEFAULT NULL,
                       i_nState   IN NUMBER DEFAULT Ftree.EXPANDED_NODE
                      )
  RETURN Ftree.NODE IS
  BEGIN
    RETURN Ftree.Add_Tree_Node(itTree,
                               i_ndMaster,
                               Ftree.PARENT_OFFSET,
                               Ftree.LAST_CHILD,
                               i_nState,
                               i_vcLabel,
                               i_vcIcon,
                               i_vcValue);
  END;
 
  PROCEDURE PR_QUERY_DETAILS(i_ndMaster   IN Ftree.NODE,
                             i_nManagerId IN NUMBER) IS
    CURSOR crDetails IS
      SELECT EMPLOYEE_ID VALUE,
             LAST_NAME || ', ' ||FIRST_NAME LABEL
        FROM EMPLOYEES
       WHERE MANAGER_ID=i_nManagerId
       ORDER BY LAST_NAME;
    nd FTree.NODE;
  BEGIN
    FOR rec IN crDetails LOOP
      -- add the node with a negative id
      nd:=FK_ADD_NODE(i_ndMaster, -rec.VALUE , rec.LABEL);
      -- don't add details here
    END LOOP;
  END;
 
 
  PROCEDURE PR_FILL_TREE IS
    CURSOR crTop IS
      SELECT EMPLOYEE_ID VALUE,
             LAST_NAME || ', ' ||FIRST_NAME LABEL
        FROM EMPLOYEES
       WHERE MANAGER_ID IS NULL
       ORDER BY LAST_NAME;
    nd FTree.NODE;
  BEGIN
    Ftree.DELETE_TREE_NODE(itTree, FTree.ROOT_NODE);
    FOR rec IN crTop LOOP
      -- add the node with a negative id
      nd:=FK_ADD_NODE(Ftree.ROOT_NODE, -rec.VALUE , rec.LABEL, NULL, FTree.COLLAPSED_NODE);
      -- don't add details here
    END LOOP;
  END;
 
  PROCEDURE PR_WTNE(i_ndNode IN Ftree.NODE) IS
    nValue   NUMBER;
    ndDetail FTree.NODE;
  BEGIN
    -- Get the value of the node to be expanded
    nValue:=FTree.GET_TREE_NODE_PROPERTY(itTree, i_ndNode, Ftree.NODE_VALUE);
    IF nValue<0 THEN
      -- value is negative, then we have to read the details
      PR_QUERY_DETAILS(i_ndNode, ABS(nValue));
      -- Now set the value of the node to positive, so that at next expansion we won't re-read the details
      FTree.SET_TREE_NODE_PROPERTY(itTree, i_ndNode, Ftree.NODE_VALUE, ABS(nValue));
      ndDetail:=FTree.FIND_TREE_NODE(itTree,'',FTREE.FIND_NEXT, FTREE.NODE_LABEL,i_ndNode,i_ndNode);  
      -- if there we're no details, set node as lead,
      IF Ftree.ID_NULL(ndDetail) THEN
        FTree.SET_TREE_NODE_PROPERTY(itTree, i_ndNode, Ftree.NODE_STATE, FTree.EXPANDED_NODE);    
      ELSE
        -- otherwise set node to expanded
        FTree.SET_TREE_NODE_PROPERTY(itTree, i_ndNode, Ftree.NODE_STATE, FTree.EXPANDED_NODE);    
      END IF;
    END IF; 
  END;

  PROCEDURE PR_WTNS(i_ndNode IN Ftree.NODE) IS
    nValue NUMBER;  
  BEGIN
    IF :SYSTEM.TRIGGER_NODE_SELECTED='TRUE' THEN
      -- Get the Employee-id, remeber that we made it negative
      nValue:=ABS(FTree.GET_TREE_NODE_PROPERTY(itTree, i_ndNode, Ftree.NODE_VALUE));
      IF nValue IS NOT NULL THEN
        :OTHRS.TREE_WHERE:='MANAGER_ID=' || TO_CHAR(nValue);
      ELSE               
        :OTHRS.TREE_WHERE:=NULL;
      END IF;
      PR_INIT_BY_TREE;
    END IF;
  END;
 
END;