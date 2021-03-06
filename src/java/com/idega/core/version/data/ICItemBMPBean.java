/*
 * Created on 23.6.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.idega.core.version.data;

import java.sql.SQLException;
import java.sql.Timestamp;

import com.idega.data.GenericEntity;
import com.idega.user.data.User;

/**
 * Title:		ICItemBMPBean
 * Description:
 * Copyright:	Copyright (c) 2003
 * Company:		idega Software
 * @author		2003 - idega team - <br><a href="mailto:gummi@idega.is">Gudmundur Agust Saemundsson</a><br>
 * @version		1.0
 */
public class ICItemBMPBean extends GenericEntity implements ICItem {

	private final static String COLUMNNAME_NAME = "name";
	private final static String COLUMNNAME_DESCRIPTION = "description";
	private final static String COLUMNNAME_CREATED_TIMESTAMP = "created_timestamp";
	private final static String COLUMNNAME_CREATED_BY_USER = "created_by_user";
	private final static String COLUMNNAME_CURRENT_OPEN_VERSION_ID = "current_open_version_id";

	/**
	 * 
	 */
	public ICItemBMPBean() {
		super();
	}

	/**
	 * @param id
	 * @throws SQLException
	 */
	public ICItemBMPBean(int id) throws SQLException {
		super(id);
	}

	/* (non-Javadoc)
	 * @see com.idega.data.IDOLegacyEntity#getEntityName()
	 */
	public String getEntityName() {
		return "ic_item";
	}

	/* (non-Javadoc)
	 * @see com.idega.data.IDOLegacyEntity#initializeAttributes()
	 */
	public void initializeAttributes() {
		addAttribute(getIDColumnName());
		addAttribute(COLUMNNAME_NAME, "Name", true, true, String.class);
		addAttribute(COLUMNNAME_DESCRIPTION, "Description", true, true, String.class);
		addAttribute(COLUMNNAME_CREATED_TIMESTAMP, "Created Timestamp", true, true, String.class);

		addManyToOneRelationship(COLUMNNAME_CREATED_BY_USER, User.class);
		addManyToOneRelationship(COLUMNNAME_CURRENT_OPEN_VERSION_ID, ICVersion.class);

	}

	/**
	 * @return
	 */
	public int getCreatedByUserID() {
		return this.getIntColumnValue(COLUMNNAME_CREATED_BY_USER);
	}

	/**
	 * @return
	 */
	public User getCreatedByUser() {
		return (User)this.getColumnValue(COLUMNNAME_CREATED_BY_USER);
	}

	/**
	 * @return
	 */
	public Timestamp getCreatedTimestamp() {
		return (Timestamp)this.getColumnValue(COLUMNNAME_CREATED_TIMESTAMP);
	}

	/**
	 * @return
	 */
	public String getDescription() {
		return this.getStringColumnValue(COLUMNNAME_DESCRIPTION);
	}

	/**
	 * @return
	 */
	public String getName() {
		return this.getStringColumnValue(COLUMNNAME_NAME);
	}

	/**
	 * @return
	 */
	public int getCurrentOpenVersionID() {
		return this.getIntColumnValue(COLUMNNAME_CURRENT_OPEN_VERSION_ID);
	}

	/**
	 * @return
	 */
	public ICVersion getCurrentOpenVersion() {
		return (ICVersion)this.getColumnValue(COLUMNNAME_CURRENT_OPEN_VERSION_ID);
	}

	/**
	 * @param userID
	 */
	public void setCreatedByUser(int userID) {
		this.setColumn(COLUMNNAME_CREATED_BY_USER, userID);
	}

	/**
	 * @param user
	 */
	public void setCreatedByUser(User user) {
		this.setColumn(COLUMNNAME_CREATED_BY_USER, user);
	}

	/**
	 * @param time
	 */
	public void setCreatedTimestamp(Timestamp time) {
		this.setColumn(COLUMNNAME_CREATED_TIMESTAMP, time);
	}

	/**
	 * @param desc
	 */
	public void setDescription(String desc) {
		this.setColumn(COLUMNNAME_DESCRIPTION, desc);
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.setColumn(COLUMNNAME_NAME, name);
	}

	/**
	 * @param versionID
	 */
	public void setCurrentOpenVersionID(int versionID) {
		this.setColumn(COLUMNNAME_CURRENT_OPEN_VERSION_ID, versionID);
	}

	/**
	 * @param version
	 */
	public void setCurrentOpenVersion(ICVersion version) {
		this.setColumn(COLUMNNAME_CURRENT_OPEN_VERSION_ID, version);
	}

}
