PACKAGE PK_ACCORDION IS
/** 
    
    This is just sample code, its free to use.
    It is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

    The code relies on the internal structure of forms when rendering stacked canvases
    It is tested against Forms 10.1.2.0.2, but may stop working with any patch or future version of forms

    Sample code for a forms-Side Package for the Accordion Java-Bean

    To build up an accordion, follow these steps
    
    -Create as many stacked canvases as you want to have accordion-canvases
    -Create a Button on each canvas at position 0,0 with the full width of the canvas and
     set the Implementation class for that buttons to forms.AccordionButton
    -Arrange these canvases on one content canvas, so that all canvases are placed beneath each other
     +The viewport for one canvas should be so that the canvas is fully shown
     +The viewport for all other canvases should be so that just the button is visible.
     
    -Initialize the Accordion in the WHEN-NEW-FORM-INSTANCE-trigger with code lik
    
      DECLARE
        lAccordionList PK_ACCORDION.tAccordionList;
        rAccordion     PK_ACCORDION.tAccordion;
      BEGIN
        rAccordion.vcCanvas:='CANVAS1';
        rAccordion.vcButton:='BLOCK.BUTTON1';
        rAccordion.vcExpandedImage:=PK_ACCORDION.VCC_DEFAULT_EXPANDED_IMAGE;
        rAccordion.vcCollapsedImage:=PK_ACCORDION.VCC_DEFAULT_COLLAPSED_IMAGE;
        rAccordion.bOpened:=TRUE;
        lAccordionList(1):=rAccordion;
  
        rAccordion.vcCanvas:='CANVAS2';
        rAccordion.vcButton:='BLOCK.BUTTON2';
        rAccordion.vcExpandedImage:=PK_ACCORDION.VCC_DEFAULT_EXPANDED_IMAGE;
        rAccordion.vcCollapsedImage:=PK_ACCORDION.VCC_DEFAULT_COLLAPSED_IMAGE;
        rAccordion.bOpened:=FALSE;
        lAccordionList(2):=rAccordion;
  
        PK_ACCORDION.PR_INIT_ACCORDION('MAIN', lAccordionList);
      END;
    
    -make sure the jar is on the archive or achive_jini-tag    
    
*/
  -- constants used for Default-Images conatined in the jar-file
  VCC_DEFAULT_EXPANDED_IMAGE  CONSTANT VARCHAR2(30):='DEFAULT_EXPANDED';
  VCC_DEFAULT_COLLAPSED_IMAGE CONSTANT VARCHAR2(30):='DEFAULT_COLLAPSED';
  
  -- Type which describes one Accordion
  TYPE tAccordion IS RECORD (
    vcCanvas         VARCHAR2(30), -- the Canvas which represents the Accordion-area
    vcButton         VARCHAR2(61), -- the "Activation"-button on that canvas which is used to activate the Accordion
    vcExpandedImage  VARCHAR2(255),-- image-name for Expanded-state, image must be accessible by forms
    vcCollapsedImage VARCHAR2(255),-- image-name for Collapsed-state, image must be accessible by forms
    bOpened          BOOLEAN       -- Flag, if this accordion is the one that is displayed at startup
                                      -- Must match the canvas. The canvases area beneath the button is taken
                                      -- as the area which is given to other accordions when opened
  );
  
  -- Table-Type of Accordion-records
  TYPE tAccordionList IS TABLE OF tAccordion INDEX BY BINARY_INTEGER;
  
  /** Initialization-method for an accordion group
      i_vcAccordionGroup indicates a logical name. It gives the capability of having several Accordion-groups inside one form.
      i_lAccordionList   is a list of the Accordion-entries which belong to the accordion-group
  */    
  PROCEDURE PR_INIT_ACCORDION(i_vcAccordionGroup IN VARCHAR2, i_lAccordionList IN tAccordionList);
  
  /** method to activate a specific accordion programmatically
      i_vcAccordionGroup indicates a logical name. Must be a name which has been initialized via PR_INIT_ACCORDION before
      i_vcCanvas         is the name of the canvas in one of the accordion-entries in the group
  */    
  PROCEDURE PR_ACTIVATE(i_vcAccordionGroup IN VARCHAR2, i_vcCanvas IN VARCHAR2);

  /** method to go to an item placed on an accordion-canvas.
      i_vcItem Name of the item which should get the focus
      
      Thanks to Francois Degrelle for supplying the code
  */    
  PROCEDURE PR_GO_ITEM(i_vcItem IN VARCHAR2);

  /** method to go to a block the first item of which is placed on an accordion-canvas.
      i_vcBlock Name of the block which should get the focus
      
      Thanks to Francois Degrelle for supplying the code
  */    
  PROCEDURE PR_GO_BLOCK(i_vcBlock IN VARCHAR2);
 
  
END;