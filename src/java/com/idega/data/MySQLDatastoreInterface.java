// idega 2000 - Tryggvi Larusson
/*
 * 
 * Copyright 2000 idega.is All Rights Reserved.
 * 
 */
package com.idega.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 
 * @author <a href="mailto:tryggvi@idega.is">Tryggvi Larusson</a>
 * 
 * @version 1.0
 * 
 */
public class MySQLDatastoreInterface extends DatastoreInterface {

	private static final int MAX_TOTAL_INDEX_COLUMN_SIZE = 255; // 767 / 3 (Innodb max / 3 for unicode)
	private static final int MAX_INDEX_COLUMN_SIZE = 100;
	
	MySQLDatastoreInterface() {
		this.useTransactionsInEntityCreation = false;
	}

	/*protected String getCreateUniqueIDQuery(GenericEntity entity) {
		return "insert into " + getSequenceTableName(entity) + "(" + entity.getIDColumnName() + ") values(null)";
	}*/

	/*
	public int createUniqueID(GenericEntity entity) throws Exception {
		int returnInt = -1;
		Connection conn = null;
		Statement stmt = null;
		ResultSet RS = null;
		try {
			conn = entity.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(getCreateUniqueIDQuery(entity));
			stmt.close();
			stmt = conn.createStatement();
			RS = stmt.executeQuery("select last_insert_id()");
			RS.next();
			returnInt = RS.getInt(1);
		}
		finally {
			if (RS != null) {
				RS.close();
			}
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				entity.freeConnection(conn);
			}
		}
		return returnInt;
	}

	protected void executeBeforeInsert(GenericEntity entity) throws Exception {
		if (entity.isNull(entity.getIDColumnName())) {
			entity.setID(createUniqueID(entity));
		}
	}

	public String getSequenceTableName(GenericEntity entity) {
		return "seq_" + entity.getTableName();
	}*/

	public String getSQLType(String javaClassName, int maxlength) {
		String theReturn;
		if (javaClassName.equals("java.lang.Integer")) {
			theReturn = "INTEGER";
		}
		else if (javaClassName.equals("java.lang.String")) {
			if (maxlength < 0) {
				theReturn = "VARCHAR(255)";
			}
			else if (maxlength <= 255) {
				theReturn = "VARCHAR(" + maxlength + ")";
			}
			else {
				theReturn = "LONGTEXT";
			}
		}
		else if (javaClassName.equals("java.lang.Boolean")) {
			theReturn = "CHAR(1)";
		}
		else if (javaClassName.equals("java.lang.Float")) {
			theReturn = "DOUBLE";
		}
		else if (javaClassName.equals("java.lang.Double")) {
			theReturn = "DOUBLE";
		}
		else if (javaClassName.equals("java.sql.Timestamp")) {
			theReturn = "DATETIME";
		}
		else if (javaClassName.equals("java.sql.Date") || javaClassName.equals("java.util.Date")) {
			theReturn = "DATE";
		}
		else if (javaClassName.equals("java.sql.Blob")) {
			theReturn = "LONGBLOB";
		}
		else if (javaClassName.equals("java.sql.Time")) {
			theReturn = "TIME";
		}
		else if (javaClassName.equals("com.idega.util.Gender")) {
			theReturn = "VARCHAR(1)";
		}
		else if (javaClassName.equals("com.idega.data.BlobWrapper")) {
			theReturn = "LONGBLOB";
		}
		else {
			theReturn = "";
		}
		return theReturn;
	}

	/*
	 * Not Tested public boolean updateTriggers(GenericEntity entity, boolean
	 * createIfNot) throws Exception { Connection conn = null; Statement Stmt =
	 * null; ResultSet rs = null; ResultSet rs2 = null; boolean returner =
	 * false; try { conn = entity.getConnection(); Stmt =
	 * conn.createStatement(); boolean tableExists = false; String seqSQL =
	 * "select * from "+getSequenceTableName(entity); try { rs =
	 * Stmt.executeQuery(seqSQL); tableExists = true; } catch (Exception e) {
	 * log("Error finding sequence table"); } if (rs != null && rs.next()) {
	 * returner = true; } else if (createIfNot) { String maxSQL = "select max
	 * ("+entity.getIDColumnName()+" as MAX from "+entity.getEntityName(); if
	 * (!tableExists) { createSequenceTable(entity); } int valueToSet = 1; rs2 =
	 * Stmt.executeQuery(maxSQL); if (rs2 != null && rs2.next()) { valueToSet =
	 * Integer.parseInt(rs2.getString("MAX")); Stmt.executeUpdate("update
	 * "+getSequenceTableName(entity)+" set "+entity.getIDColumnName()+" =
	 * "+valueToSet); }
	 * 
	 * returner = true; } } finally { if (Stmt != null) { Stmt.close(); } if (rs !=
	 * null) { rs.close(); } if (rs2 != null) { rs2.close(); } if (conn != null) {
	 * entity.freeConnection(conn); } } return returner; }
	 */
	
	/*public void createSequenceTable(GenericEntity entity) throws Exception {
		Connection conn = null;
		Statement Stmt = null;
		try {
			conn = entity.getConnection();
			Stmt = conn.createStatement();
			String s = "CREATE table " + getSequenceTableName(entity) + "(" + entity.getIDColumnName()
					+ " integer PRIMARY KEY auto_increment)";
			System.out.println(s);
			Stmt.executeUpdate(s);
		}
		finally {
			if (Stmt != null) {
				Stmt.close();
			}
			if (conn != null) {
				entity.freeConnection(conn);
			}
		}
	}*/

	public void createTrigger(GenericEntity entity) throws Exception {
		//createSequenceTable(entity);
		//Not needed anymore because now using auto_increment directly
		
	}

	//public void createForeignKeys(GenericEntity entity) throws Exception {
	//}

	public void createIndex(GenericEntity entity, String name, String[] fields) throws Exception {
		if (useIndexes()) {
			StringBuffer sql = new StringBuffer("CREATE INDEX ").append(name).append(" ON ").append(entity.getTableName()).append(" (");
			int maxSize = MAX_TOTAL_INDEX_COLUMN_SIZE / fields.length;
			if (maxSize > MAX_INDEX_COLUMN_SIZE) {
				maxSize = MAX_INDEX_COLUMN_SIZE;
			}
			
			for (int i = 0; i < fields.length; i++) {
				IDOEntityField field = entity.getEntityDefinition().findFieldByUniqueName(fields[i]);
				int maxLength = field.getMaxLength();
				
				if (i > 0) {
					sql.append(", ");
				}
				sql.append(fields[i]);

				Class dataTypeClass = field.getDataTypeClass();
				if (maxLength > 0) {
					if (maxLength > MAX_INDEX_COLUMN_SIZE) {
						maxLength = MAX_INDEX_COLUMN_SIZE;
					}
					sql.append("(").append(maxLength).append(")");
				} else if (maxLength == -1 && String.class.isAssignableFrom(dataTypeClass)) {
					sql.append("(").append(maxSize).append(")");
				} else if (Boolean.class.isAssignableFrom(dataTypeClass)) {
					sql.append("(1)");
				}
			}
			sql.append(")");
			executeUpdate(entity, sql.toString());
		}
	}
	
	//Auto_increment handling:
	
	protected void updateNumberGeneratedValue(GenericEntity entity, Connection conn){
		Statement stmt = null;
		ResultSet rs = null;
		try {
			boolean pkIsNull = entity.isNull(entity.getIDColumnName());
			if (pkIsNull) {
				stmt = conn.createStatement();
				rs = stmt.executeQuery("select last_insert_id()");
				rs.next();
				int id = rs.getInt(1);
				entity.setID(id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * @return boolean
	 */
	protected boolean updateNumberGeneratedValueAfterInsert()
	{
		return true;
	}
	public String getIDColumnType(GenericEntity entity)
	{
		if (entity.getIfAutoIncrement()) {
			return "INTEGER AUTO_INCREMENT";
		} else {
			return "INTEGER";
		}
	}

	/*public void setNumberGeneratorValue(GenericEntity entity, int value) {
		// throw new RuntimeException("setSequenceValue() not implemented for
		// "+this.getClass().getName());
		String statement = "insert into " + this.getSequenceTableName(entity) + " values(" + value + ")";
		try {
			this.executeUpdate(entity, statement);
		}
		catch (Exception e) {
			// e.printStackTrace();
			System.err.println("MySQLDatastoreInterface.setNumberGeneratorValue() Exception: " + e.getMessage());
		}
	}*/
	
}
