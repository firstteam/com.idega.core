/*
 * $Id: IDOContainer.java,v 1.28 2009/04/22 12:50:56 valdas Exp $
 * Created in 2002 by Tryggvi Larusson
 *
 * Copyright (C) 2002-2006 Idega software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 *
 */
package com.idega.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EntityBean;
import javax.ejb.FinderException;

import com.idega.repository.data.Instantiator;
import com.idega.repository.data.Singleton;
import com.idega.repository.data.SingletonRepository;
import com.idega.util.datastructures.HashtableDoubleKeyed;

/**
 * <p>
 * IDOContainer is the central service for the IDO persistence Framework.<br/>
 * This class is a singleton for the application and is the "base center" for
 * getting access to other component of the persistence engine.
 * </p>
 * Last modified: $Date: 2009/04/22 12:50:56 $ by $Author: valdas $
 *
 * @author <a href="mailto:tryggvil@idega.com">Tryggvi Larusson</a>
 * @version $Revision: 1.28 $
 */
public class IDOContainer implements Singleton {

	// Static variables:
	private static Instantiator instantiator = new Instantiator() {
		@Override
		public Object getInstance() {
			return new IDOContainer();
		}
	};
	// Instance variables:
	// this instance variable sets if beancaching is active by default for all
	// entities
	private boolean beanCachingActiveByDefault = false;
	private boolean queryCachingActive = false;
	private Map<Class<?>, List<?>> emptyBeanInstances;
	private Map<String, Map<Class<?>, IDOBeanCache>> beanCacheMap;
	private Map<Class<?>, Boolean> isBeanCacheActive;
	// These variables were moved from GenericEntity:
	private Map<Class<?>, IDOEntityDefinition> entityAttributes;
	private Map<Class<?>, IDOEntity> entityStaticInstances;
	private HashtableDoubleKeyed<String, EntityRelationship> relationshipTables = new HashtableDoubleKeyed<String, EntityRelationship>();

	protected IDOContainer() {
		// unload
	}

	public static IDOContainer getInstance() {
		return (IDOContainer) SingletonRepository.getRepository().getInstance(IDOContainer.class, instantiator);
	}

	protected Map<Class<?>, List<?>> getBeanMap() {
		if (this.emptyBeanInstances == null) {
			this.emptyBeanInstances = new HashMap<Class<?>, List<?>>();
		}
		return this.emptyBeanInstances;
	}

	/**
	 * <p>
	 * Map with all datasources and hashmaps for beancache for each datasource.<br/>
	 * Keys are datasourceNames and values are Maps for each datasource.
	 * </p>
	 *
	 * @return
	 */
	protected Map<String, Map<Class<?>, IDOBeanCache>> getDatasourcesBeanCacheMaps() {
		if (this.beanCacheMap == null) {
			this.beanCacheMap = new HashMap<String, Map<Class<?>, IDOBeanCache>>();
		}
		return this.beanCacheMap;
	}

	/**
	 * <p>
	 * Gets a BeanCacheMap for each datasource, where the key is a
	 * entityInterfacesClass (Class) and value is a IDOBeanCache instance.
	 * </p>
	 *
	 * @param dataSource
	 * @return
	 */
	protected Map<Class<?>, IDOBeanCache> getBeanCacheMap(String dataSource) {
		Map<String, Map<Class<?>, IDOBeanCache>> bCacheMap = getDatasourcesBeanCacheMaps();
		Map<Class<?>, IDOBeanCache> dataSourceMap = bCacheMap.get(dataSource);
		if (dataSourceMap == null) {
			dataSourceMap = new HashMap<Class<?>, IDOBeanCache>();
			bCacheMap.put(dataSource, dataSourceMap);
		}
		return dataSourceMap;
	}

	protected Map<Class<?>, Boolean> getIsBeanCachActiveMap() {
		if (this.isBeanCacheActive == null) {
			this.isBeanCacheActive = new HashMap<Class<?>, Boolean>();
		}
		return this.isBeanCacheActive;
	}

	protected IDOBeanCache getBeanCache(String datasource, Class<? extends IDOEntity> entityInterfaceClass) {
		IDOBeanCache idobc = getBeanCacheMap(datasource).get(entityInterfaceClass);
		if (idobc == null) {
			idobc = new IDOBeanCache(entityInterfaceClass, datasource);
			getBeanCacheMap(datasource).put(entityInterfaceClass, idobc);
		}
		return idobc;
	}

	protected List<?> getFreeBeansList(Class<? extends IDOEntity> entityInterfaceClass) {
		List<?> l = getBeanMap().get(entityInterfaceClass);
		if (l == null) {
			l = new ArrayList<Object>();
		}
		getBeanMap().put(entityInterfaceClass, l);
		return l;
	}

	protected <T extends IDOEntity> T getFreeBeanInstance(Class<T> entityInterfaceClass) throws Exception {
		T entity = null;
		/*
		 * List l = getFreeBeansList(entityInterfaceClass); if(!l.isEmpty()){
		 * entity= (IDOEntity)l.get(0); }
		 */
		if (entity == null) {
			entity = this.instanciateBean(entityInterfaceClass);
		}
		return entity;
	}

	public <T extends IDOEntity> T createEntity(Class<T> entityInterfaceClass) throws javax.ejb.CreateException {
		try {
			T entity = null;
			try {
				entity = getFreeBeanInstance(entityInterfaceClass);
			}
			catch (Error e) {
				System.err.println("[idoContainer] : Error creating bean for " + entityInterfaceClass.getName());
				e.printStackTrace();
			}
			((EntityBean) entity).ejbActivate();
			((IDOEntityBean) entity).ejbCreate();
			return entity;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new IDOCreateException(e);
		}
	}

	protected <T extends IDOEntity> T instanciateBean(Class<T> entityInterfaceClass) throws Exception {
		Class<T> beanClass = null;
		T entity = null;
		try {
			beanClass = IDOLookup.getBeanClassFor(entityInterfaceClass);
		}
		catch (Error t) {
			System.err.println("Error looking up bean class for bean: " + entityInterfaceClass.getName());
			t.printStackTrace();
		}
		try {
			entity = beanClass.newInstance();
		}
		catch (Error t) {
			System.err.println("Error instanciating bean class for bean: " + entityInterfaceClass.getName());
			t.printStackTrace();
		}
		return entity;
	}

	/**
	 * To find the data by a primary key (cached if appropriate), usually called
	 * by HomeImpl classes
	 */
	public <T extends IDOEntity> T findByPrimaryKey(Class<T> entityInterfaceClass, Object pk, IDOHome home) throws javax.ejb.FinderException {
		return findByPrimaryKey(entityInterfaceClass, pk, null, home);
	}

	/**
	 * To find the data by a primary key (cached if appropriate), usually called
	 * by HomeImpl classes
	 */
	<T extends IDOEntity> T findByPrimaryKey(Class<T> entityInterfaceClass, Object pk, IDOHome home, String dataSourceName) throws javax.ejb.FinderException {
		return findByPrimaryKey(entityInterfaceClass, pk, null, home, dataSourceName);
	}

	/**
	 * Workaround to speed up finders where the ResultSet is already created
	 */
	<T extends IDOEntity> T findByPrimaryKey(Class<T> entityInterfaceClass, Object pk, java.sql.ResultSet rs, IDOHome home) throws javax.ejb.FinderException {
		return findByPrimaryKey(entityInterfaceClass, pk, rs, home, null);
	}

	/**
	 * Workaround to speed up finders where the ResultSet is already created
	 */
	<T extends IDOEntity> T findByPrimaryKey(
			Class<T> entityInterfaceClass,
			Object pk,
			java.sql.ResultSet rs,
			IDOHome home,
			String dataSourceName
	) throws javax.ejb.FinderException {
		try {
			T entity = null;
			IDOBeanCache cache = null;

			boolean useBeanCaching = beanCachingActive(entityInterfaceClass);
			if (useBeanCaching) {
				cache = this.getBeanCache(dataSourceName, entityInterfaceClass);
				entity = cache.getCachedEntity(pk);
			}
			if (entity == null) {
				entity = this.instanciateBean(entityInterfaceClass);
				if (dataSourceName != null) {
					try {
						((GenericEntity) entity).setDatasource(dataSourceName);
					}
					catch (ClassCastException ce) {
						ce.printStackTrace();
					}
				}
				/**
				 * @todo
				 */
				((IDOEntityBean) entity).ejbFindByPrimaryKey(pk);
				if (rs != null) {
					((GenericEntity) entity).preEjbLoad(rs);
				}
				else {
					((IDOEntityBean) entity).ejbLoad();
				}
				((IDOEntityBean) entity).setEJBLocalHome(home);
				if (useBeanCaching) {
					cache.putCachedEntity(pk, entity);
				}
			}
			return entity;
		}
		catch (Exception e) {
//			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error getting by primary key: " + pk + " ("+pk.getClass()+")", e);
			throw new FinderException(e.getMessage());
		}
	}

	/**
	 * <p>
	 * Sets if the beanCaching is active by default for all entities
	 * </p>
	 *
	 * @param onOrOff
	 */
	public synchronized void setBeanCachingActiveByDefault(boolean active) {
		if (!active) {
			this.flushAllBeanCache();
		}
		this.beanCachingActiveByDefault = active;
	}

	/**
	 *
	 * @param entityInterfaceClass
	 * @return returns true if bean cashing is active for all entities or if it
	 *         is active for this entityInterfaceClass
	 */
	protected <T extends IDOEntity> boolean beanCachingActive(Class<T> entityInterfaceClass) {
		Boolean isActive = getIsBeanCachActiveMap().get(entityInterfaceClass);
		if (isActive == null) {
			try {
				IDOEntityDefinition def = IDOLookup.getEntityDefinitionForClass(entityInterfaceClass);
				isActive = def.isBeanCachingActive();
			}
			catch (IDOLookupException t) {
				System.err.println("Error looking up entity defitition for bean: " + entityInterfaceClass.getName());
				t.printStackTrace();
			}
			if (isActive == null) { // still null, use system-default
				isActive = ((this.beanCachingActiveByDefault) ? Boolean.TRUE : Boolean.FALSE);
			}
			getIsBeanCachActiveMap().put(entityInterfaceClass, isActive);
		}
		return isActive.booleanValue();
	}

	public synchronized void setQueryCaching(boolean onOrOff) {
		if (!onOrOff) {
			this.flushAllQueryCache();
		}
		this.isBeanCacheActive = null; // remove at least all elements useing
										// system-default (queryCachingActive)
		this.queryCachingActive = onOrOff;
	}

	protected <T extends IDOEntity> boolean queryCachingActive(Class<T> entityInterfaceClass) {
		return this.queryCachingActive;
	}

	<T extends IDOEntity> T getPooledInstance(Class<T> entityInterfaceClass) {
		return null;
	}

	public synchronized void flushAllCache() {
		this.flushAllBeanCache();
		this.flushAllQueryCache();
	}

	public synchronized void flushAllBeanCache() {
		Iterator<String> dsIterator = getDatasourcesBeanCacheMaps().keySet().iterator();
		while (dsIterator.hasNext()) {
			String dataSource = dsIterator.next();
			Iterator<Class<?>> iter = getBeanCacheMap(dataSource).keySet().iterator();
			while (iter.hasNext()) {
				@SuppressWarnings("unchecked")
				Class<IDOEntity> interfaceClass = (Class<IDOEntity>) iter.next();
				this.getBeanCache(dataSource, interfaceClass).flushAllBeanCache();
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[idoContainer] Flushed all Bean Cache");
	}

	public synchronized void flushAllQueryCache() {
		if (this.queryCachingActive) {
			Iterator<String> dsIterator = getDatasourcesBeanCacheMaps().keySet().iterator();
			while (dsIterator.hasNext()) {
				String dataSource = dsIterator.next();
				Iterator<Class<?>> iter = getBeanCacheMap(dataSource).keySet().iterator();
				while (iter.hasNext()) {
					@SuppressWarnings("unchecked")
					Class<IDOEntity> interfaceClass = (Class<IDOEntity>) iter.next();
					this.getBeanCache(dataSource, interfaceClass).flushAllQueryCache();
				}
			}
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "[idoContainer] Flushed all Query Cache");
		}
	}

	/**
	 * Map Used by the IDO Framework and stores a static instance of a
	 * IDOEntityDefinition. This map has as a key a Class instance and a value a
	 * IDOEntityDefinition instance.
	 *
	 * @return Returns the entityAttributes.
	 */
	Map<Class<?>, IDOEntityDefinition> getEntityDefinitions() {
		if (this.entityAttributes == null) {
			this.entityAttributes = new HashMap<Class<?>, IDOEntityDefinition>();
		}
		return this.entityAttributes;
	}

	/**
	 * Map Used by the IDO Framework and stores a static instance of a
	 * IDOEntity. This map has as a key a Class instance and a value a IDOEntity
	 * instance.
	 */
	Map<Class<?>, IDOEntity> getEntityStaticInstances() {
		if (this.entityStaticInstances == null) {
			this.entityStaticInstances = new HashMap<Class<?>, IDOEntity>();
		}
		return this.entityStaticInstances;
	}

	/**
	 * Map used to look up relationships (Many-to-many) between tables.<br>
	 * The keys here are two Strings (the EntityNames or TableNames for the
	 * Entity beans that have the relationship) and as a value an instance of
	 * EntityRelationship.
	 *
	 * @return the relationship Map
	 */
	HashtableDoubleKeyed<String, EntityRelationship> getRelationshipTableMap() {
		if (this.relationshipTables == null) {
			this.relationshipTables = new HashtableDoubleKeyed<String, EntityRelationship>();
		}
		return this.relationshipTables;
	}

	private DatastoreInterfaceManager datastoreInterfaceManager;

	/**
	 * @return Returns the datastoreInterfaceManager.
	 */
	public DatastoreInterfaceManager getDatastoreInterfaceManager() {
		if (this.datastoreInterfaceManager == null) {
			this.datastoreInterfaceManager = new DatastoreInterfaceManager();
		}
		return this.datastoreInterfaceManager;
	}

	/**
	 * @param datastoreInterfaceManager
	 *            The datastoreInterfaceManager to set.
	 */
	public void setDatastoreInterfaceManager(DatastoreInterfaceManager datastoreInterfaceManager) {
		this.datastoreInterfaceManager = datastoreInterfaceManager;
	}
}
