/**
 * Copyright (C) 2004  idega Software
 *
 */
package com.idega.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWUserContext;
import com.idega.presentation.util.RenderUtil;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.text.AttributeParser;
import com.idega.util.xml.XmlUtil;

/**
 * @author <a href="mailto:tryggvi@idega.is">Tryggvi Larusson</a>
 * @version 1.0
 * HTML based Page component.
 * This component can be used to template the look and feel
 * of your site in a simple manner. You supply an HTML template
 * directly.
 * Inside the Layout template tags are used to define "regions" where UIComponent
 * components can be added in and rendered dynamically.<br>
 * The regions are defined like this:
 *
 * <code><pre>
 * <HTML>... <BODY>... <!-- TemplateBeginEditable name="MyUniqueRegionId1" -->MyUniqueRegionId1<!-- TemplateEndEditable --> ... <table><tr><td><!-- TemplateBeginEditable name="MyUniqueRegionId2" -->MyUniqueRegionId2<!-- TemplateEndEditable --</td></tr></table>
 * </pre></code>
 *
 * This class parses the HTML and looks for the tag  <code><pre><!-- TemplateBeginEditable ... ></pre></code>
 * Where the first region found becomes the "default".
 * This class also parses the  <code><pre> <HEAD> </pre></code> attribute contents and includes the things normally found inside
 * an idegaWeb Page.
 */
public class HtmlPage extends Page {

	private static final Logger LOGGER = Logger.getLogger(HtmlPage.class.getName());

	private String html;
	private Map<String, Integer> regionMap;

	//This variable sets if regions are treated as facets if set to true. Otherwise they are treated as children
	private boolean regionAsFacet;

	public HtmlPage() {
		super();
	}

	public void setResource(InputStream htmlStream) {
		if (htmlStream != null) {
			try {
				setHtml(StringHandler.getContentFromInputStream(htmlStream));
			} catch(Exception e) {
				throw new RuntimeException("Attribute <resourceName> for component <" + getId() + ">. Could not load the html from named resource <>");
			} finally {
				IOUtil.closeInputStream(htmlStream);
			}
		}

		findOutRegions();
	}

	/**
	 * Need to render my children myself. Typical for layout
	 * management components.
	 *
	 * @see javax.faces.component.UIComponent#getRendersChildren()
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * Gets the default (first found) region.
	 * Returns null if none is found.
	 * @return
	 */
	public String getDefaultRegion(){
		for (Iterator<String> iter = getRegionIdsMap().keySet().iterator(); iter.hasNext();) {
			String key = iter.next();
			Integer value = getRegionIdsMap().get(key);
			if (value.intValue() == 0) {
				return key;
			}
		}
		return null;
	}

	/**
	 *
	 */
	@Override
	public List<UIComponent> getChildren() {
		return super.getChildren();
	}

	/**
	 *
	 */
	@Override
	protected void setChildren(List<UIComponent> newChildren) {
		super.setChildren(newChildren);
	}

	public UIComponent getRegion(String regionKey) {
		if (this.regionAsFacet) {
			return getFacets().get(regionKey);
		}
		else{
			Integer index = getRegionIdsMap().get(regionKey);
			if(index!=null){
				Object o = getChildren().get(index.intValue());
				UIComponent child = (UIComponent) o;
				return child;
			}
			else{
				return null;
			}
		}
	}

	public void setRegion(String regionKey, UIComponent region){
		if (this.regionAsFacet) {
			if (regionKey != null) {
				getFacets().put(regionKey,region);
			}
		}
		else {
			getChildren().add(region);
		}
	}

	public void add(UIComponent component, String regionId) {
		UIComponent region = getRegion(regionId);
		if (region != null) {
			region.getChildren().add(component);
		}
		else{
			getLogger().info("No Region found for regionId="+regionId);
		}
	}

	@Override
	public void add(UIComponent comp){
		add(comp, getDefaultRegion());
	}

	@Override
	public void add(PresentationObject po) {
		add((UIComponent) po);
	}

	/**
	 * The Map over the regions.
	 * Has as a key the regionId and as the value the index of
	 * the corresponding HtmlPageRegion object int the getChildren() List.
	 * @return
	 */
	private Map<String, Integer> getRegionIdsMap() {
		if (this.regionMap == null) {
			this.regionMap = new HashMap<String, Integer>();
		}
		return this.regionMap;
	}

	/**
	 * Returns all the regionIds as Strings
	 * @return
	 */
	public Set<String> getRegionIds(){
		return getRegionIdsMap().keySet();
	}

	private void findOutRegions(){
		String template = getHtml();
		if (template == null) {
			LOGGER.info("There is no template for this page");
			return;
		}

		String[] parts = template.split("<!-- TemplateBeginEditable");
		int regionIndex=0;
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			String[] t = part.split("TemplateEndEditable -->");

			String toParse = t[0];
			String[] a1 = toParse.split("name=\"");
			if (a1.length < 2) {
				LOGGER.warning("Invalid region pattern! Got part:\n" + toParse);
				continue;
			}
			String[] a2 = a1[1].split("\"");

			String regionId = a2[0];

			getRegionIdsMap().put(regionId,new Integer(regionIndex));
			//	Instantiate the region in the children list:
			HtmlPageRegion region = new HtmlPageRegion();
			region.setRegionId(regionId);
			setRegion(regionId,region);
			regionIndex++;
		}
	}

	/**
	 * Overrided from Page
	 */
	@Override
	public void encodeBegin(FacesContext context)throws IOException{
		//Does nothing here
	}

	/**
	 * Overrided from Page
	 * @throws IOException
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException{
		//Does just call the print(iwc) method below:
		callPrint(context);
	}

	/**
	 * Overrided from Page
	 */
	@Override
	public void encodeEnd(FacesContext context)throws IOException{
		//Does nothing here
		encodeRenderTime(context);
	}

	@Override
	public void print(IWContext ctx) throws IOException {
		IWContext iwc = IWContext.getIWContext(ctx);

		addSessionPollingDWRFiles(iwc);
		addNotifications(iwc);
		enableReverseAjax(iwc);
		enableChromeFrame(iwc);

		Writer out = IWMainApplication.useJSF ? ctx.getResponseWriter() : ctx.getWriter();

		String template = getHtml();
		if (template == null) {
			out.write("Template file could not be found.");
			out.close();
			return;
		}

		//	Process the HEAD first:
		Pattern headOpensPattern = Pattern.compile("<head>", Pattern.CASE_INSENSITIVE);
		String[] headOpensSplit = headOpensPattern.split(template);
		String preHead = headOpensSplit[0];
		String postHeadOpens = headOpensSplit[1];
		out.write(preHead);
		out.write("<head>");

		Pattern headClosesPattern = Pattern.compile("</head>", Pattern.CASE_INSENSITIVE);
		String[] headClosesSplit = headClosesPattern.split(postHeadOpens);
		String headContent = headClosesSplit[0];
		String body = headClosesSplit[1];

		//	Get the contents from the superclass first
		out.write(getHeadContents(ctx));
		Script associatedScript = getAssociatedScript();
		renderChild(ctx,associatedScript);

		//	Then printout the head contents from the HTML page find out where the title is in the head:
		String htmlTitle = CoreConstants.EMPTY, writtenTitle = null;
		try {
			//	Try to find where the TITLE tag is in the HEAD:
			Pattern titlePattern = Pattern.compile("<title>", Pattern.CASE_INSENSITIVE);
			String[] titleSplit = titlePattern.split(headContent);
			String preTitleHead= titleSplit[0];
			String postTitleOpens = titleSplit[1];

			Pattern postTitlePattern = Pattern.compile("</title>", Pattern.CASE_INSENSITIVE);
			String[] postTitleSplit = postTitlePattern.split(postTitleOpens);
			htmlTitle = postTitleSplit[0];
			String postTitleHead = postTitleSplit[1];

			//	Print out all before the TITLE tag in the HEAD
			out.write(preTitleHead);
			//	Print out the title from the idegaWeb page
			String locTitle = this.getLocalizedTitle(ctx);
			out.write("<title>");
			if (StringUtil.isEmpty(locTitle)) {
				writtenTitle = htmlTitle;
			} else {
				writtenTitle = locTitle;
			}
			out.write(writtenTitle);
			out.write("</title>");
			//	Print out all after the TITLE tag in the HEAD
			out.write(postTitleHead);
		} catch (ArrayIndexOutOfBoundsException ae) {
			//	If there is an error (title not found) then just write out the whole head contents + idegaWeb Title
			out.write(headContent);
			String locTitle = this.getLocalizedTitle(ctx);
			out.write("<title>");
			if (StringUtil.isEmpty(locTitle)) {
				writtenTitle = htmlTitle;
			} else {
				writtenTitle = locTitle;
			}
			out.write(writtenTitle);
			out.write("</title>");
		}
		out.write("</head>");

		String[] htmlBody = body.split("<body");
		int index = 0;
		if (htmlBody.length > 1) {
			out.write(htmlBody[index++]);
		}
		body = htmlBody[index];

		String attributes = body.substring(0, body.indexOf(">"));
		Map<String, String> attributeMap = AttributeParser.parse(attributes);
		for (Iterator<String> iter = attributeMap.keySet().iterator(); iter.hasNext();) {
			String attribute = iter.next();
			String value = attributeMap.get(attribute);
			if (attribute.equals("onload")) {
				this.setMarkupAttributeMultivalued("onload", value);
			}
			if (attribute.equals("onunload")) {
				this.setMarkupAttributeMultivalued("onunload", value);
			}
			else {
				if (!isMarkupAttributeSet(attribute)) {
					setMarkupAttribute(attribute, value);
				}
			}
		}
		String attributesString = getMarkupAttributesString();
		out.write("<body" + (StringUtil.isEmpty(attributesString) ? CoreConstants.EMPTY : (CoreConstants.SPACE + attributesString + CoreConstants.SPACE)) + ">\n");

		body = body.substring(body.indexOf(">") + 1);

		//	Process the template regions:
		String[] parts = body.split("<!-- TemplateBeginEditable");
		out.write(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			String[] t = part.split("TemplateEndEditable -->");

			String toParse = t[0];
			String[] a1 = toParse.split("name=\"");
			if (a1.length < 2) {
				LOGGER.warning("Invalid region pattern! Unable to extract region's ID! Got region's part:\n" + toParse);
				continue;
			}
			String[] a2 = a1[1].split("\"");

			String regionId = a2[0];

			try {
				UIComponent region = getRegion(regionId);
				renderChild(ctx,region);
			}
			catch (ClassCastException cce) {
				LOGGER.log(Level.WARNING, "Error occured while rendering UI component", cce);
			}
			if (t.length < 2) {
				LOGGER.warning("Invalid region pattern! Got template's part:\n" + part);
			} else {
				out.write(t[1]);
			}
		}

		for (UIComponent child: getChildren()) {
			if (!(child instanceof HtmlPageRegion)) {
				renderChild(ctx, child);
			}
		}

		Map<?, ?> renderUtils = WebApplicationContextUtils.getWebApplicationContext(iwc.getServletContext()).getBeansOfType(RenderUtil.class);
		if (!MapUtil.isEmpty(renderUtils)) {
			String newTitle = getLocalizedTitle(iwc);
			for (Object renderUtil: renderUtils.values()) {
				if (renderUtil instanceof RenderUtil) {
					((RenderUtil) renderUtil).doRemoveNeedlessContentAndSetRealPageTitle(out, newTitle, writtenTitle);
				}
			}
		}

		out.close();
	}

	/**
	 * @see javax.faces.component.UIPanel#saveState(javax.faces.context.FacesContext)
	 */
	@Override
	public Object saveState(FacesContext ctx) {
		Object values[] = new Object[4];
		values[0] = super.saveState(ctx);
		values[1] = getHtml();
		values[2] = this.regionMap;
		values[3] = Boolean.valueOf(this.regionAsFacet);
		return values;
	}

	/**
	 * @see javax.faces.component.UIPanel#restoreState(javax.faces.context.FacesContext, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void restoreState(FacesContext ctx, Object state) {
		Object values[] = (Object[])state;
		super.restoreState(ctx, values[0]);
		setHtml((String)values[1]);
		this.regionMap = (Map<String, Integer>) values[2];
		this.regionAsFacet = ((Boolean)values[3]).booleanValue();
	}

	/**
	 * @return
	 */
	public String getHtml() {
		return this.html;
	}

	/**
	 * @param string
	 */
	public void setHtml(String string) {
		this.html = string;
		findOutRegions();
		findOutResources();
	}

	private void findOutResources() {
		if (StringUtil.isEmpty(html)) {
			return;
		}

		Document templateDoc = XmlUtil.getJDOMXMLDocument(html, false);
		if (templateDoc == null) {
			Logger.getLogger(getClass().getName()).warning("Unable to optimize resources! Template file can not be resolved!");
			return;
		}

		Element root = templateDoc.getRootElement();
		Namespace namespace = root.getNamespace();
		List<Element> uselessElements = new ArrayList<Element>();

		List<Element> javaScripts = XmlUtil.getElementsByXPath(templateDoc, "script", namespace);
		if (!ListUtil.isEmpty(javaScripts)) {
			for (Element script: javaScripts) {
				Attribute source = script.getAttribute("src");
				if (source == null || StringUtil.isEmpty(source.getValue())) {
					String action = script.getValue();
					if (!StringUtil.isEmpty(action)) {
						addJavaScriptAction(action);
					}
				} else {
					String sourceUri = source.getValue();
					if (StringUtil.isEmpty(sourceUri)) {
						break;
					}

					addJavascriptURL(sourceUri);
				}

				uselessElements.add(script);
			}
		}

		List<Element> styleSheets = XmlUtil.getElementsByXPath(templateDoc, "link", namespace);
		if (!ListUtil.isEmpty(styleSheets)) {
			for (Element style: styleSheets) {
				Attribute source = style.getAttribute("href");
				if (source == null) {
					break;
				}
				String sourceUri = source.getValue();
				if (StringUtil.isEmpty(sourceUri)) {
					break;
				}

				Attribute media = style.getAttribute("media");
				if (media == null) {
					break;
				}

				addStyleSheetURL(sourceUri, media.getValue());
				uselessElements.add(style);
			}
		}

		if (uselessElements.size() > 0) {
			for (Iterator<Element> uselessElementsIter = uselessElements.iterator(); uselessElementsIter.hasNext();) {
				uselessElementsIter.next().detach();
			}

			this.html = XmlUtil.getPrettyJDOMDocument(templateDoc);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object clone(IWUserContext iwc, boolean askForPermission){
		HtmlPage newPage = (HtmlPage) super.clone(iwc,askForPermission);
		if (this.regionMap != null) {
			newPage.regionMap = (Map<String, Integer>) ((HashMap<String, Integer>) this.regionMap).clone();
		}
		return newPage;
	}

	@Override
	public void main(IWContext iwc) throws Exception {
		super.main(iwc);
	}

	/* (non-Javadoc)
	 * @see com.idega.presentation.PresentationObject#_main(com.idega.presentation.IWContext)
	 */
	@Override
	public void _main(IWContext iwc) throws Exception {
		super._main(iwc);
	}

	/**
	 * This variable gets if regions are treated as facets if set to true.
	 * Default is false.
	 * @return Returns the regionAsFacet.
	 */
	protected boolean isRegionAsFacet() {
		return this.regionAsFacet;
	}

	/**
	 * This sets if regions are treated as facets if set to true. Otherwise they are treated as children.
	 * Default value is set to false.
	 * @param regionAsFacet The regionAsFacet to set.
	 */
	protected void setRegionAsFacet(boolean regionAsFacet) {
		this.regionAsFacet = regionAsFacet;
	}

	@Override
	public Map<String, UIComponent> getFacets() {
		if (this.facetMap == null) {
			this.facetMap = new HtmlPageRegionFacetMap(this);
		}
		return this.facetMap;
	}
}
