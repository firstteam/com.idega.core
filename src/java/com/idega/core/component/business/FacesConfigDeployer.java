/*
 * $Id: FacesConfigDeployer.java,v 1.7 2006/09/18 12:36:59 gediminas Exp $
 * Created on 5.2.2006 in project org.apache.axis
 * 
 * Copyright (C) 2006 Idega Software hf. All Rights Reserved.
 * 
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 */
package com.idega.core.component.business;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.idega.idegaweb.IWModule;
import com.idega.idegaweb.JarLoader;

/**
 * <p>
 * Implementation of JarLoader to automatically scan all faces-config.xml files
 * in all installed Jar files, parse them, and read into the componentRegistry.
 * </p>
 * Last modified: $Date: 2006/09/18 12:36:59 $ by $Author: gediminas $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.7 $
 */
public class FacesConfigDeployer implements JarLoader {

	private static Logger LOGGER = Logger.getLogger(FacesConfigDeployer.class.getName());
	private ComponentRegistry registry;

	/**
	 * @param registry
	 * 
	 */
	public FacesConfigDeployer(ComponentRegistry registry) {
		this.registry = registry;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.idega.idegaweb.JarLoader#loadJar(java.io.File,
	 *      java.util.jar.JarFile, java.lang.String)
	 */
	public void loadJar(File bundleJarFile, JarFile jarFile, String jarPath) {
		JarEntry entry = jarFile.getJarEntry("META-INF/faces-config.xml");
		
		if (entry != null) {
			LOGGER.fine("Loading components from " + jarPath);
			try {
				InputStream stream = jarFile.getInputStream(entry);
				processFacesConfig(jarFile, stream);
			}
			catch (IOException e) {
				LOGGER.log(Level.WARNING, null, e);
			}
			catch (ParserConfigurationException e) {
				LOGGER.log(Level.WARNING, null, e);
			}
			catch (SAXException e) {
				LOGGER.log(Level.WARNING, null, e);
			}
		}
	}

	public void processFacesConfig(JarFile jarFile,InputStream stream) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(stream);
		processDocument(jarFile,document);
	}

	public void processDocument(JarFile jarFile, Document document) {
		Element rootElement = document.getDocumentElement();
		NodeList childList = rootElement.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			Node child = childList.item(i);
			if (child instanceof Element) {
				Element elem = (Element) child;
				if (elem.getNodeName().equals("component")) {
					processComponentElement(jarFile, elem);
				}
			}
		}
	}

	protected void processComponentElement(JarFile jarFile,Element element) {
		NodeList children = element.getChildNodes();
		String componentClass = null;
		String componentType = null;
		int childrenCount = children.getLength();
		for (int i = 0; i < childrenCount; i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element elem = (Element) child;
				if (elem.getNodeName().equals("component-class")) {
					componentClass = getNodeTextValue(elem);
				}
				else if (elem.getNodeName().equals("component-type")) {
					componentType = getNodeTextValue(elem);
				}
			}
		}
		if (componentClass != null) {
			ComponentInfo info = this.registry.getComponentByClassName(componentClass);
			info = processComponentExtension(jarFile,element,info,componentClass,componentType);
			if (info != null) {
				processProperties(element, info);
			}
		}
	}

	/**
	 * <p>
	 * TODO tryggvil describe method processComponentExtension
	 * </p>
	 * @param element
	 * @param info
	 * @param componentType 
	 * @param componentClass 
	 */
	private ComponentInfo processComponentExtension(JarFile jarFile,Element componentElement, ComponentInfo info, String componentClass, String componentType) {
		NodeList componentExtensions = componentElement.getElementsByTagName("component-extension");
		String objectType = null;
		boolean builderVisible = false;
		if (componentExtensions != null) {
			for (int i = 0; i < componentExtensions.getLength(); i++) {
				Node componentExtension = componentExtensions.item(i);
				if(componentExtension instanceof Element){
					NodeList idegaWebInfos = ((Element) componentExtension).getElementsByTagName("idegaweb-info");
					for (int j = 0; j < idegaWebInfos.getLength(); j++) {
						Node idegaWebInfo = idegaWebInfos.item(j);
						if(idegaWebInfo instanceof Element){
							NodeList iwChildren = idegaWebInfo.getChildNodes();
							for (int k = 0; k < iwChildren.getLength(); k++) {
								Node nChild = iwChildren.item(k);
								if(nChild instanceof Element){
									Element child = (Element)nChild;
									if (child.getNodeName().equals("builder-visible")) {
										builderVisible = Boolean.valueOf(getNodeTextValue(child)).booleanValue();
									}
									else if (child.getNodeName().equals("object-type")) {
										objectType = getNodeTextValue(child);
									}
								}
							}
						}
					}
				}
			}
		}
		if (builderVisible && objectType != null && info == null) {
			String moduleIdentifier = null;
			if (jarFile instanceof IWModule) {
				IWModule module = (IWModule) jarFile;
				moduleIdentifier = module.getModuleIdentifier();
			}
			String componentName = componentType;
			LOGGER.fine("Registering component " + componentName);
			info = this.registry.registerComponentPersistent(componentName,componentClass,componentType,objectType,moduleIdentifier);
		}
		return info;
	}

	private String getNodeTextValue(Node node) {
		String value = node.getNodeValue();
		if (value == null) {
			NodeList values = node.getChildNodes();
			Node child0 = values.item(0);
			if (child0 != null) {
				value = child0.getNodeValue();
			}
		}
		return value != null ? value.trim() : null;
	}

	/**
	 * <p>
	 * TODO tryggvil describe method processProperties
	 * </p>
	 * 
	 * @param element
	 * @param info
	 */
	private void processProperties(Element componentElement, ComponentInfo info) {
		NodeList propertiesList = componentElement.getElementsByTagName("property");
		for (int i = 0; i < propertiesList.getLength(); i++) {
			Node nProperty = propertiesList.item(i);
			String propertyName = null;
			String propertyClass = null;
			String displayName = null;
			String description = null;
			String icon = null;
			String suggestedValue = null;
			if (nProperty instanceof Element) {
				Element property = (Element) nProperty;
				NodeList lPropertyAttributes = property.getChildNodes();
				for (int j = 0; j < lPropertyAttributes.getLength(); j++) {
					Node nPropertyAttr = lPropertyAttributes.item(j);
					if (nPropertyAttr instanceof Element) {
						Element elem = (Element) nPropertyAttr;
						if (elem.getNodeName().equals("property-name")) {
							propertyName = getNodeTextValue(elem);
						}
						else if (elem.getNodeName().equals("property-class")) {
							propertyClass = getNodeTextValue(elem);
						}
						else if (elem.getNodeName().equals("display-name")) {
							displayName = getNodeTextValue(elem);
						}
						else if (elem.getNodeName().equals("description")) {
							description = getNodeTextValue(elem);
						}
						else if (elem.getNodeName().equals("icon")) {
							icon = getNodeTextValue(elem);
						}
						else if (elem.getNodeName().equals("suggested-value")) {
							suggestedValue = getNodeTextValue(elem);
						}
					}
				}
				DefaultComponentProperty prop = new DefaultComponentProperty(info);
				prop.setName(propertyName);
				prop.setClassName(propertyClass);
				prop.setDisplayName(displayName);
				prop.setDescription(description);
				prop.setIcon(icon);
				prop.setSuggestedValue(suggestedValue);

				info.getProperties().add(prop);

			}
		}
	}

}