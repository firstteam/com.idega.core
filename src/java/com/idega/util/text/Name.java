package com.idega.util.text;

import java.util.*;

import com.idega.util.LocaleUtil;

/**
 * Title: com.idega.util.text.Name
 * Description: A helper class for splitting up a fullname into first,middle and lastnames.
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author  Eirikur Hrafnsson
 * @version 1.1
 */

 public class Name {

    private String firstName = null;
    private String middleName = null;
    private String lastName = null;
    private String fullName = null;

    public Name() {
    }

    public Name(String first, String middle, String last) {
        if(first != null)
            firstName = first;
        if(middle != null)
            middleName = middle;
        if(last != null)
            lastName = last;
    }

    public Name(String fullName) {
        setName(fullName);
    }

    public String getName() {
	    	if(fullName==null){
	    		StringBuffer fullNameBuffer = new StringBuffer();

	    		  firstName = (firstName==null) ? "" : firstName;
	    		  middleName = (middleName==null) ? "" : middleName;
	    		  lastName = (lastName==null) ? "" : lastName;
	    		
	    		 
	    		  fullNameBuffer.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
	    		  
	    		  fullName = fullNameBuffer.toString();
	    		  fullName = TextSoap.findAndReplace(fullName,"  "," ");
	    	}
        return this.fullName;
    }
    
    public String getName(Locale locale) {
    		return getName(locale, false);
    }
    
    public String getName(Locale locale, boolean commaSeperated) {
    		if (fullName == null) {
    			StringBuffer buffer = new StringBuffer();
    			 firstName = (firstName==null) ? "" : firstName;
	    		 middleName = (middleName==null) ? "" : middleName;
	    		 lastName = (lastName==null) ? "" : lastName;
    			if (locale.equals(LocaleUtil.getIcelandicLocale())) {     
    				buffer.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
    			}
    			else {
    				buffer.append(lastName);
    				if (commaSeperated) {
    					buffer.append(",");
    				}
    				buffer.append(" ").append(firstName).append(" ").append(middleName);
    			}
    			return buffer.toString();
    			
    		}
    		return getName();
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getMiddleName() {
        return this.middleName;
    }

    public String getLastName() {
        return this.lastName;
    }
    
    public void setMiddleName(String middleName) {
      this.middleName = middleName;
      fullName = null;
    }

    public void setFirstName(String firstName) {
    		this.firstName = firstName;
        fullName = null;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        fullName = null;
    }

    public void setName(String name) {
		if ((name != null) && (name.length() > 0)) {
		    fullName = name;
			StringTokenizer token = new StringTokenizer(name);
			int countWithoutFirstAndLast = token.countTokens() - 2;
			
			if( token.hasMoreTokens() ){
				firstName = token.nextToken();
	
				if (countWithoutFirstAndLast >= 1) {
					StringBuffer middleName = new StringBuffer();
	
					for (int i = 0; i < countWithoutFirstAndLast; i++) {
						middleName.append(token.nextToken());
	
						if (i != (countWithoutFirstAndLast - 1))
							middleName.append(" ");
	
					}
	
					this.middleName = middleName.toString();
				}
				else { //set middle name == null
					this.middleName = null;
				}
	
				if (countWithoutFirstAndLast >= 0) {
					lastName = token.nextToken();
				}
				else { //remove last name
					this.lastName = null;
				}
			}
			else System.out.println("com.idega.util.text.Name fullname is an empty string!");
		}
	}
    
    /**
     * Capitalizes every part of a Name
     * @return 
     */
    public Name capitalize(){
        if(this.firstName!=null)
            this.firstName = TextSoap.capitalize(this.firstName);
        if(this.lastName!=null)
            this.lastName = TextSoap.capitalize(this.lastName);
        if(this.middleName!=null)
            this.middleName = TextSoap.capitalize(this.middleName," ");
        this.fullName = getName();
        return this;
    }
  }