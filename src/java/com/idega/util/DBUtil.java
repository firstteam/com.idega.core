package com.idega.util;

import com.idega.business.SpringBeanName;
import com.idega.util.expression.ELUtil;

@SpringBeanName(DBUtil.BEAN_NAME)
public abstract class DBUtil {

	static final String BEAN_NAME = "iwCoreDBUtil";
	
	public static final DBUtil getInstance() {
		return ELUtil.getInstance().getBean(BEAN_NAME);
	}
	
	/**
	 * This method uses session bounded with the current context (thread)
	 * 
	 * @param entity
	 * @return
	 */
	public abstract <T> T initializeAndUnproxy(T entity);

	/**
	 * Checks if JPA entity is initialized
	 * 
	 * @param object
	 * @return
	 */
	public abstract boolean isInitialized(Object object);
	
	/**
	 * This method opens a new session 
	 * 
	 * @param entity
	 * @return
	 */
	public abstract <T> T lazyLoad(T entity);
	
}