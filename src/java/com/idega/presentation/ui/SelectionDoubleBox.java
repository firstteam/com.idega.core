//idega 2001 - Tryggvi Larusson
/*
*Copyright 2001 idega.is All Rights Reserved.
*/

package com.idega.presentation.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.web2.business.JQuery;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Page;
import com.idega.presentation.Script;
import com.idega.presentation.Table;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

/**
*@author <a href="mailto:tryggvi@idega.is">Tryggvi Larusson</a><br>
* modified <a href="mailto:eiki@idega.is">Eirikur Hrafnsson</a>
*@version 1.0
*/

public class SelectionDoubleBox extends InterfaceObject {

	@Autowired
	private JQuery jQuery;
	
	private SelectionBox leftBox = null;
	private SelectionBox rightBox = null;
	private GenericButton toTheRight = null;
	private GenericButton toTheLeft = null;
	private String leftName = null;
	private String rightName = null;
	private String leftLabel = null;
	private String rightLabel = null;

	private boolean isSetAsNotEmpty = false;
	private String notEmptyErrorMessage;

	public static final String LEFT_INPUT_NAME = "leftName";
	public static final String LEFT_INPUT_LABEL = "leftLabel";
	public static final String LEFT_INPUT_OPTIONS = "leftOptions";
	public static final String RIGHT_INPUT_NAME = "rightName";
	public static final String RIGHT_INPUT_LABEL = "rightLabel";
	public static final String RIGHT_INPUT_OPTIONS = "rightOptions";
	
	
	private static final String FUNCTION_ADDOPT_NAME = "addOpt";
	private static final String FUNCTION_ADDOPT = "function addOpt( list, val, text, idx, selected ) {  if( selected == null ) selected = false;  if( idx != null ) {          list.options[idx] = new Option( text, val, false, selected );  } else {          list.options[list.length] = new Option( text, val, false, selected );  }}";
	
	private static final String FUNCTION_MOVE_NAME = "move";
	private static final String FUNCTION_MOVE = "function move( from, to ) {\n  var here =  from ;\n  var there =  to ;\n\n  if( here.selectedIndex != -1 && here.length > 0) {\n\n    for( h=0; h<here.length;h++) {\n      if( here.options[h].selected ) {\n        addOpt( there, here.options[h].value, here.options[h].text );\n      }\n    }\n    for( h1=here.length-1;h1>-1; h1-- ) {\n      if( here.options[h1].selected ) {\n        here.options[h1] = null;\n      }\n    }\n          \n  }\n}";
	
	public SelectionDoubleBox(){
		this("untitled");
	}
	
	public SelectionDoubleBox(String nameOfRightBox){
	  this(nameOfRightBox+"_left",nameOfRightBox);
	}
	
	public SelectionDoubleBox(String nameOfRightBox,String headerOfLeftBox,String headerOfRightBox){
	  this(nameOfRightBox+"_left",nameOfRightBox);
	  this.leftLabel = headerOfLeftBox;
	  this.rightLabel = headerOfRightBox;
	}

	public SelectionDoubleBox(String nameOfLeftBox, String nameOfRightBox) {
		this.leftName = nameOfLeftBox;
		this.rightName = nameOfRightBox;
	}

    @Override
	public void encodeBegin(FacesContext context) throws IOException { 
    	ValueExpression ve = getValueExpression(LEFT_INPUT_NAME);
    	if (ve != null) {
	    	String name = (String) ve.getValue(context.getELContext());
	    	setLeftName(name);
    	}    
    	
    	ve = getValueExpression(LEFT_INPUT_LABEL);
    	if (ve != null) {
	    	String label = (String) ve.getValue(context.getELContext());
	    	setLeftLabel(label);
    	}    

		ve = getValueExpression(LEFT_INPUT_OPTIONS);
    	if (ve != null) {
    		@SuppressWarnings("unchecked")
			List<SelectOption> options = (List<SelectOption>) ve.getValue(context.getELContext());
    		for (SelectOption selectOption : options) {
				addToLeftBox(selectOption.getValueAsString(), selectOption.getName());
			}
    	}    
    	
    	ve = getValueExpression(RIGHT_INPUT_NAME);
    	if (ve != null) {
	    	String name = (String) ve.getValue(context.getELContext());
	    	setRightName(name);
    	}

    	ve = getValueExpression(RIGHT_INPUT_LABEL);
    	if (ve != null) {
	    	String label = (String) ve.getValue(context.getELContext());
	    	setRightLabel(label);
    	}    

		ve = getValueExpression(RIGHT_INPUT_OPTIONS);
    	if (ve != null) {
    		@SuppressWarnings("unchecked")
			List<SelectOption> options = (List<SelectOption>) ve.getValue(context.getELContext());
    		for (SelectOption selectOption : options) {
				addToRightBox(selectOption.getValueAsString(), selectOption.getName());
			}
    	}    
    	
    	super.encodeBegin(context);
    }
    
	@Override
	public void main(IWContext iwc) throws Exception {
		ELUtil.getInstance().autowire(this);
		
	    this.leftBox = getLeftBox();
	    this.rightBox = getRightBox();
	    this.toTheRight = getRightButton();
		this.toTheLeft = getLeftButton();
		
		if( this.leftLabel!=null){
		  this.leftBox.setTextHeading(this.leftLabel);
		}
		
		if(this.rightLabel!=null){
			this.rightBox.setTextHeading(this.rightLabel);
		}
		
		if(this.isSetAsNotEmpty){
			this.rightBox.setAsNotEmpty(this.notEmptyErrorMessage);	
		}
    
		if (getStyleAttribute() != null) {
			this.leftBox.setStyleAttribute(getStyleAttribute());
			this.rightBox.setStyleAttribute(getStyleAttribute());
			if (getWidth() != null) {
				this.leftBox.setWidth(getWidth());
				this.rightBox.setWidth(getWidth());
			}
			this.toTheLeft.setStyleAttribute(getStyleAttribute());
			this.toTheRight.setStyleAttribute(getStyleAttribute());
		}

		Table table = new Table(3, 2);
		add(table);

		table.add(this.leftBox, 1, 1);

		this.toTheRight.setOnClick("move( this.form." + this.leftBox.getName() + ", this.form." + this.rightBox.getName() + " )");
		table.add(this.toTheRight, 2, 1);

		table.addBreak(2, 1);

		this.toTheLeft.setOnClick("move( this.form." + this.rightBox.getName() + ", this.form." + this.leftBox.getName() + " )");
		table.add(this.toTheLeft, 2, 1);

		table.add(this.rightBox, 3, 1);
		if (enableOrdering) {
			IWResourceBundle iwrb = getResourceBundle(iwc);
			
			Layer buttons = new Layer();
			GenericButton up = new GenericButton(iwrb.getLocalizedString("up", "Up"));
			up.setOnClick("jQuery('#"+getRightBox().getId()+" option:selected').each( function() { moveOptionElementUp(jQuery(this), '"+getRightBox().getId()+"'); });");
			buttons.add(up);
			GenericButton down = new GenericButton(iwrb.getLocalizedString("down", "Down"));
			down.setOnClick("jQuery('#"+getRightBox().getId()+" option:selected').each( function() { moveOptionElementDown(jQuery(this), '"+getRightBox().getId()+"'); });");
			buttons.add(down);
			table.add(buttons, 3, 2);
		}
		
		//add the script
		Page page =  this.getParentPage();
		if(page != null){
			Script script = page.getAssociatedScript();
			addToScripts(script);
		}else{
			//if page does not exists
			Layer scriptLayer = new Layer();
			this.add(scriptLayer);
			Map<String, String> functions = getFunctions();
			Set<String> keys = functions.keySet();
			StringBuilder script = new StringBuilder();
			for(Iterator<String> iter = keys.iterator();iter.hasNext();){
				String key = iter.next();
				script.append(functions.get(key));
			}
			String scriptString = PresentationUtil.getJavaScriptAction(script.toString());
			scriptLayer.add(scriptString);
		}
		if (enableOrdering)
			PresentationUtil.addJavaScriptSourceLineToHeader(iwc, jQuery.getBundleURIToJQueryLib());
	}

	public SelectionBox getLeftBox() {
		if( this.leftBox == null ) {
			this.leftBox = new SelectionBox(this.leftName);
		}
		return this.leftBox;		
	}

	public SelectionBox getRightBox() {
		if( this.rightBox == null ) {
			this.rightBox = new SelectionBox(this.rightName);
		}
		return this.rightBox;
	}

	public GenericButton getLeftButton() {
		if( this.toTheLeft == null ) {
			this.toTheLeft = new GenericButton("sdb_left", "<<");
		}
		return this.toTheLeft;
	}

	public GenericButton getRightButton() {
		if( this.toTheRight == null ) {
			this.toTheRight = new GenericButton("sdb_right", ">>");
		}
		return this.toTheRight;
	}

	public void addToScripts(Script script) {
		Map<String, String> functions = getFunctions();
		Set<String> keys = functions.keySet();
		for(Iterator<String> iter = keys.iterator();iter.hasNext();){
			String key = iter.next();
			script.addFunction(key, functions.get(key));
		}
	}
	
	private boolean enableOrdering;
	
	private Map<String, String> getFunctions(){
		Map<String, String> functions = new HashMap<String, String>();
		functions.put(FUNCTION_ADDOPT_NAME, FUNCTION_ADDOPT);
		functions.put(FUNCTION_MOVE_NAME, FUNCTION_MOVE);
		return functions;
	}
	
	public boolean isEnableOrdering() {
		return enableOrdering;
	}

	public void setEnableOrdering(boolean enableOrdering) {
		this.enableOrdering = enableOrdering;
	}

	public void addToSelectedBox(String value, String displayString) {
		getRightBox().addElement(value, displayString);
	}

	public void addToAvailableBox(String value, String displayString) {
		getLeftBox().addElement(value, displayString);
	}

	public void addToLeftBox(String value, String displayString) {
		addToAvailableBox(value, displayString);
	}

	public void addToRightBox(String value, String displayString) {
		addToSelectedBox(value, displayString);
	}

	public void setLeftName(String name) {
		getLeftBox().setName(name);	
	}
	
	public void setRightName(String name) {
		getRightBox().setName(name);	
	}
	
	/**
	 * Sets the right selection box so that it can not be empty, displays an alert with the given 
	 * error message if the "error" occurs.  Uses Javascript.
	 * @param errorMessage	The error message to display.
	 */
	public void setAsNotEmpty(String errorMessage) {
		this.isSetAsNotEmpty = true;
		this.notEmptyErrorMessage = errorMessage;
	}
	/**
	 * @param string
	 */
	public void setLeftLabel(String string) {
		this.leftLabel = string;
	}

	/**
	 * @param string
	 */
	public void setRightLabel(String string) {
		this.rightLabel = string;
	}

	/* (non-Javadoc)
	 * @see com.idega.presentation.ui.InterfaceObject#handleKeepStatus(com.idega.presentation.IWContext)
	 */
	@Override
	public void handleKeepStatus(IWContext iwc) {
	}

	/* (non-Javadoc)
	 * @see com.idega.presentation.PresentationObject#isContainer()
	 */
	@Override
	public boolean isContainer() {
		return false;
	}
}