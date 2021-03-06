/*
 * Created on Jan 6, 2004
 *
 */
package com.idega.idegaweb.presentation;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import com.idega.core.file.business.FileIconSupplier;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Image;
import com.idega.presentation.Table;
import com.idega.presentation.text.DownloadLink;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.CheckBox;
import com.idega.presentation.ui.FileInput;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.IFrame;
import com.idega.presentation.ui.SubmitButton;
import com.idega.presentation.ui.TextInput;
import com.idega.util.FileUtil;



/**
 * The <code>FileManager</code> class represents a file
 * manager resource files of the running webapplication.
 * @author aron 
 * @version 1.0
 */
public class FileManager extends Block {
	
	private boolean relativePath = false;
	private String folder = null;
	private String topLevelFolder = "/";
	private String[] skipFiles = null;
	private String[] skipFolders = null;
	private String headerBackgroundColor = "#000000";
	private String fileBackgroundColor = "#e8e8e8";
	private String headerFontColor = "#ffffff";
	private String textFontColor = "#124274";
	private String boldFontColor = "#000000";
	private String bundleIdentifier =null;
	private IWBundle iwb =null;
	private IWResourceBundle iwrb = null;
	private String currentFolder =null,currentFile =null;
	private java.util.List maintainParameterNames =null;
	private boolean filesDeletable = true;
	private boolean displayFilesInFrame = true;
	private FileIconSupplier iconSupplier =null;
	private static final String PRM_FOLDER = "iw_b_r_m_dir";
	private static final String PRM_SUB_FOLDER ="iw_b_r_m_sdir";
	private static final String PRM_FILE = "iw_b_r_m_fil";
	private static final String PRM_DELFILE = "iw_b_r_m_rmfil";
	private static final String PRM_CREATE_FOLDER ="iw_b_r_m_mkdir";
	private static final String PRM_UPLOAD_FILE ="iw_b_r_m_upld";
	private static final String PRM_VIEW_FILE ="iw_b_r_m_vwld";
	
	public void main(IWContext iwc) {
		//debugParameters(iwc);
		this.iwb = getBundle(iwc);
		this.iwrb =getResourceBundle(iwc);
		if(iwc.isLoggedOn()){
			this.iconSupplier =FileIconSupplier.getInstance();
//			currentFolder = folder;
			if(null==this.currentFolder){
				if(null!=this.folder){
					if(this.relativePath == true){
						String appURI = iwc.getIWMainApplication().getApplicationRealPath();
						this.folder = appURI+this.folder;
						this.currentFolder = this.folder;
//						System.out.println("Path is relative to: "+appURI);
						this.relativePath = false;
					}else{
//						System.out.println("Path is absolute");
						this.currentFolder = this.folder;
					}
				}
			}
/*			else{
				currentFolder = topLevelFolder;
			}
*/
			if(iwc.isParameterSet(PRM_FOLDER)) {
				this.currentFolder = iwc.getParameter(PRM_FOLDER);
			}
			if(iwc.isParameterSet(PRM_FILE)) {
				this.currentFile = iwc.getParameter(PRM_FILE);
			}
			if(iwc.isParameterSet(PRM_SUB_FOLDER)) {
				this.currentFolder = this.folder+iwc.getParameter(PRM_SUB_FOLDER);
			}
			
			System.out.println("path:"+this.currentFolder);
			
			if(iwc.isMultipartFormData()){
				if(processUpload(iwc)) {
					presentateFileView(iwc);
				}
				else {
					presentateUploadView(iwc);
				}
			}
			else if(iwc.isParameterSet(PRM_VIEW_FILE)){
				presentateSelectedFile(iwc);
			}
			else if(iwc.isParameterSet("folder_name")){
				if(processFolderCreation(iwc)) {
					presentateFileView(iwc);
				}
				else {
					presentateCreateNewFolderView(iwc);
				}
			}
			else if(iwc.isParameterSet(PRM_CREATE_FOLDER)){
				presentateCreateNewFolderView(iwc);
			}
			else if(iwc.isParameterSet(PRM_UPLOAD_FILE)){
				presentateUploadView(iwc);
			}
			else if(iwc.isParameterSet(PRM_DELFILE)){
				processFileRemoval(iwc);
				presentateFileView(iwc);
			}
			else {
				presentateFileView(iwc);
			}
		}
		else{
			add(this.iwrb.getLocalizedString("user_not_logged_on","No user is logged on"));
		}
		
	}
	
	public String getBundleIdentifier(){
		if(this.bundleIdentifier==null) {
			return super.getBundleIdentifier();
		}
		return this.bundleIdentifier;
	}
	
	private boolean processUpload(IWContext iwc){
		iwc.getUploadedFile().getAbsolutePath();
		boolean overwrite =iwc.isParameterSet("overwrite");
		File uploadedFile = iwc.getUploadedFile().getAbsoluteFile();
		try {
			File newFile = new File(this.currentFolder,uploadedFile.getName());
			
			if(newFile.exists() &&  !overwrite){
				getParentPage().setOnLoad("alert('"+this.iwrb.getLocalizedString("file_already_exists","File already exists !")+"')");
			}
			else{
				FileUtil.copyFile(uploadedFile,newFile);
				return true;
				
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean processFolderCreation(IWContext iwc){
		String newFolderName = iwc.getParameter("folder_name");
		FileUtil.createFolder(new File(this.currentFolder,newFolderName).getAbsolutePath());
		return true;
	}
	
	private boolean processFileRemoval(IWContext iwc){
		if(this.currentFile!=null){
			FileUtil.delete(this.currentFile);
			return true;
		}
		return false;
	}
	
	private void presentateSelectedFile(IWContext iwc){
		Table table = new Table();
		table.add(getHeaderTable(iwc,new File(this.currentFolder)),1,1);
		table.add(getHeaderText(this.iwrb.getLocalizedString("selected_file","Selected file")+" : "+new File(this.currentFile).getName()),1,2);
		table.setColor(1,2,this.headerBackgroundColor);
		
		

		if(this.displayFilesInFrame){
			IFrame frame =new IFrame("fileview",600,600);
			frame.setSrc(getCurrentFileUrl(iwc,this.currentFile));
	//		frame.setSrc(currentFile);
			frame.setAsTransparent(true);
			frame.setBorder(0);
			table.add(frame,1,3);
		}
		
		add(table);
	}
	
	private String getCurrentFileUrl(IWContext iwc,String fileAbsPath){
//	  TODO fix x url
		String server = iwc.getServerURL().replace('\\','/');
		String context = iwc.getIWMainApplication().getApplicationContextURI().replace('\\','/');
		String appURI = iwc.getIWMainApplication().getApplicationRealPath().replace('\\','/');
		//String appURI =iwc.getApplication().getApplicationContextURI();
		//int index = currentFile.indexOf(appURI);
/*
		System.out.println("curr file "+currentFile);
		System.out.println("appURI "+appURI);
		System.out.println("context "+context);
		System.out.println("server "+server);
*/
		String url = fileAbsPath.substring(appURI.length());
		if(context.endsWith("/")) {
			url = context+url;
		}
		else {
			url =context+"/"+url;
		}
			//url ="/"+url;
		url = server+url;
//		System.out.println("url "+url);
		return url;
	}
	
	
	/**
	 * Creates a list of all files and folders in the current folder 
	 * @param iwc
	 */
	private void presentateFileView(IWContext iwc) {
		if(null==this.currentFolder){
			add(this.iwrb.getLocalizedString("please_set_the_path_for_this_block","Please set the path for this block"));
			return;
		}
		Table fileTable = new Table();
		int row = 1;
		fileTable.add(getHeaderText(this.iwrb.getLocalizedString("file","File")), 1, row);
		fileTable.add(getHeaderText(this.iwrb.getLocalizedString("size","Size")), 2, row);
		fileTable.add(getHeaderText(this.iwrb.getLocalizedString("date","Date")), 3, row);
		row++;
//		System.out.println("Path in present file "+currentFolder);
		File dir = new File(this.currentFolder);
		Table table = new Table();
		table.setWidth(Table.HUNDRED_PERCENT);
		int resourcesLength = 0, foldersLength = 0;
		File[] folders = dir.listFiles(new FolderFilter(this.skipFolders));
		if (folders != null) {
			Arrays.sort(folders);
			foldersLength = folders.length;
		}
		File[] resources = dir.listFiles(new ResourceFileFilter(this.skipFiles));
		if (resources != null) {
			Arrays.sort(resources);
			resourcesLength = resources.length;
		}
		File[] files = new File[foldersLength + resourcesLength];
		int numberOfFiles = files.length;
		if (folders != null) {
			System.arraycopy(folders, 0, files, 0, folders.length);
		}
		if (resources != null) {
			System.arraycopy(resources, 0, files, folders.length, resources.length);
		}
		File file;
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT, iwc.getCurrentLocale());
		if (!isTopLevel(dir)) {
			String dirPath = dir.getAbsolutePath().replace('\\','/');
			fileTable.add(getFolderIcon(), 1, row);
			fileTable.add(getUpFolderLink((dirPath.substring(0, dirPath.lastIndexOf("/"))), iwc), 1, row);
			fileTable.setColor(1, row, this.fileBackgroundColor);
			fileTable.setColor(2, row, this.fileBackgroundColor);
			fileTable.setColor(3, row, this.fileBackgroundColor);
			if(this.filesDeletable){
				fileTable.setColor(4, row, this.fileBackgroundColor);
			}
			row++;
		}
		
		for (int i = 0; i < files.length; i++) {
			file = files[i];
			if (file.isDirectory()) {
				fileTable.add(getFolderIcon(), 1, row);
				fileTable.add(getFolderLink(file, iwc), 1, row);
			}
			else {
				fileTable.add(getFileIcon(file), 1, row);
				fileTable.add(getFileLink(file, iwc), 1, row);
				long length = file.length();
				String bytes = " B";
				if (length >= 1000) {
					length = length / 1000;
					bytes = " kB";
				}
				fileTable.add(getText(String.valueOf(length) + bytes), 2, row);
				if(this.filesDeletable){
					fileTable.add(getDeleteFileLink(file, iwc), 4, row);
				}
			}
			fileTable.setColor(1, row, this.fileBackgroundColor);
			fileTable.setColor(2, row, this.fileBackgroundColor);
			fileTable.setColor(3, row, this.fileBackgroundColor);
			if(this.filesDeletable){
				fileTable.setColor(4, row, this.fileBackgroundColor);
			}
			fileTable.add(getText(df.format(new java.util.Date(file.lastModified()))), 3, row);
			row++;
		}
		
		fileTable.setNoWrap();
		fileTable.setWidth(Table.HUNDRED_PERCENT);
		fileTable.setWidth(1, "50%");
		fileTable.setWidth(2, "1%");
		fileTable.setWidth(3, "1%");
		if(this.filesDeletable){
			fileTable.setWidth(4, "1%");
		}
		fileTable.setColumnAlignment(3, Table.HORIZONTAL_ALIGN_RIGHT);
		if(this.filesDeletable){
			fileTable.setColumnAlignment(4, Table.HORIZONTAL_ALIGN_RIGHT);
		}
		fileTable.setRowColor(1, "black");
		fileTable.setCellspacing(2);
		fileTable.setNoWrap();
		fileTable.setColumnAlignment(2, Table.HORIZONTAL_ALIGN_RIGHT);
		table.add(getHeaderTable(iwc, dir), 1, 1);
		table.setAlignment(1, 2, Table.HORIZONTAL_ALIGN_RIGHT);
		table.add(fileTable, 1, 2);
		table.add(getFooterTable(iwc, dir, numberOfFiles), 1, 3);
		add( table);
	}
	
	private Table getHeaderTable(IWContext iwc, File dir) {
		Table headerTable = new Table();
		headerTable.setWidth(Table.HUNDRED_PERCENT);
		headerTable.setCellspacing(2);
		headerTable.add(getText(this.iwrb.getLocalizedString("current_folder","Current folder")+": "), 1, 1);
		String absolute = dir.getAbsolutePath().replace('\\','/');
		String shortdir = absolute.substring(absolute.indexOf(this.topLevelFolder)+this.topLevelFolder.length());
/*
		System.out.println("absolute path: "+absolute);
		System.out.println("top level path: "+topLevelFolder);
		System.out.println("shortdir path: "+shortdir);
*/
		if(shortdir.length()==0){
			shortdir ="/";
			headerTable.add(getSubFolderLink(shortdir,iwc), 1, 1);
		}
		else{
			StringBuffer path = new StringBuffer("/");
			StringTokenizer tokener = new StringTokenizer(shortdir,"/");
			while(tokener.hasMoreTokens()){
				path.append(tokener.nextToken());
				headerTable.add("/");
				headerTable.add(getSubFolderLink((path.toString()),iwc),1,1);
				path.append("/");
			}
		}
		
		headerTable.add(getCreateFolderLink(dir, iwc), 3, 1);
		headerTable.add(Text.getNonBrakingSpace(),3,1);
		headerTable.add(getUploadLink(dir, iwc), 3, 1);
		headerTable.setAlignment(3, 1, Table.HORIZONTAL_ALIGN_RIGHT);
		return headerTable;
	}
	private Table getFooterTable(IWContext iwc, File dir, int numberOfFiles) {
		Table footerTable = new Table(3, 1);
		footerTable.setWidth(Table.HUNDRED_PERCENT);
		footerTable.add(getCreateFolderLink(dir, iwc), 1, 1);
		footerTable.add(Text.getNonBrakingSpace(),1,1);
		footerTable.add(getUploadLink(dir, iwc), 1, 1);
		footerTable.add( String.valueOf(numberOfFiles) +" "+getText(this.iwrb.getLocalizedString("files","Files")), 3, 1);
		footerTable.setAlignment(3, 1, Table.HORIZONTAL_ALIGN_RIGHT);
		footerTable.setCellspacing(2);
		return footerTable;
	}
	private void presentateUploadView(IWContext iwc) {
		Table table = new Table(2, 4);
		table.setWidth(Table.HUNDRED_PERCENT);
		table.setCellspacing(2);
		table.mergeCells(1, 1, 2, 1);
		table.add(getHeaderText(this.iwrb.getLocalizedString("upload_file","Upload file")+ ":"), 1, 1);
		table.setColor(1, 1, this.headerBackgroundColor);
		table.setAlignment(1, 1, Table.HORIZONTAL_ALIGN_LEFT);
		table.add(getBoldText(this.iwrb.getLocalizedString("file","File")+":"), 1, 2);
		FileInput fileInput = new FileInput();
		
		table.add(fileInput, 2, 2);
		CheckBox overWriteCheck = new CheckBox("overwrite");
		table.add(overWriteCheck, 2, 3);
		table.add(getText(this.iwrb.getLocalizedString("overwrite_file","Overwrite file with same name if exists")), 2, 3);
		SubmitButton createFolder = new SubmitButton(this.iwrb.getLocalizedString("upload" , "Upload") );
		table.add(createFolder, 2, 4);
		Form form = new Form();
		form.add(table);
		form.setMultiPart();
		form.maintainParameter(PRM_FOLDER);
		form.maintainParameters(this.maintainParameterNames);
		add( form);
	}
	private void presentateCreateNewFolderView(IWContext iwc) {
		Table table = new Table(2, 4);
		table.setWidth(Table.HUNDRED_PERCENT);
		table.setCellspacing(2);
		table.mergeCells(1, 1, 2, 1);
		table.add(getHeaderText(this.iwrb.getLocalizedString("create_new_folder","Create new folder")+" :"), 1, 1);
		table.setColor(1, 1, this.headerBackgroundColor);
		table.setAlignment(1, 1, Table.HORIZONTAL_ALIGN_LEFT);
		table.add(getBoldText(this.iwrb.getLocalizedString("folder_name","Folder name")+":"), 1, 2);
		TextInput folderNameInput = new TextInput("folder_name");
		folderNameInput.setLength(40);
		table.add(folderNameInput, 2, 2);
		SubmitButton createFolder = new SubmitButton(this.iwrb.getLocalizedString("create_folder","Create folder"));
		table.add(createFolder, 2, 3);
		Form form = new Form();
		form.add(table);
		form.maintainParameter(PRM_FOLDER);
		form.maintainParameters(this.maintainParameterNames);
		add( form);
	}
	private boolean isTopLevel(File file) {
		String absPath = file.getAbsolutePath().replace('\\','/');
		if(this.topLevelFolder!=null){
			int toplevel = this.topLevelFolder.lastIndexOf("/");
			String top =this.topLevelFolder.substring(toplevel);
			int indexOfTopLevel = absPath.indexOf(top);
			return absPath.lastIndexOf("/") == indexOfTopLevel;
		}
		return false;
	}
	
	private Image getFileIcon(File file) {
		return new Image(this.iconSupplier.getFileIconURLByFileName(file.getName()));
		//return iwb.getImage("shared/rugl.gif");
	}
	private Image getFolderIcon() {
		return new Image(this.iconSupplier.getFolderIconURL());
		//return iwb.getImage("shared/folder.gif");
	}
	private Image getDeleteIcon() {
		return this.iwb.getImage("shared/delete.gif","delete");
	}
	private Link getFolderLink(File file, IWContext iwc) {
		Link l = new Link(getText(file.getName()));
		l.addParameter(PRM_FOLDER, file.getAbsolutePath());
		addMaintainedParameters(iwc,l);
		return l;
	}
	private Link getSubFolderLink(String subpath, IWContext iwc) {
		String name =subpath;
		int index =subpath.lastIndexOf("/");
		if(index>-1 && subpath.length()>index+1 ){
			name =subpath.substring(index+1);
		}
		Link l = new Link(getText(name));
		l.addParameter(PRM_SUB_FOLDER, subpath);
		addMaintainedParameters(iwc,l);
		return l;
	}
	private Link getUpFolderLink(String path, IWContext iwc) {
		Text t = getText(".. ("+this.iwrb.getLocalizedString("up_one_level","Up one level")+")" );
		Link l = new Link(t);
		l.addParameter(PRM_FOLDER, path);
		addMaintainedParameters(iwc,l);
		return l;
	}
	private Link getFileLink(File file, IWContext iwc) {
		//String url = file.getAbsolutePath().substring(folder.length());
		//l.setURL(url);
	    if(this.displayFilesInFrame){
	        Link l = new Link(getText(file.getName()));
			l.addParameter(PRM_VIEW_FILE,"true");
			l.addParameter(PRM_FILE, file.getAbsolutePath());
			l.addParameter(PRM_FOLDER,this.currentFolder);
	
			addMaintainedParameters(iwc,l);
			return l;
	    }
	    else{
	        DownloadLink l = new DownloadLink(file.getName());
	        l.setAbsoluteFilePath(file.getAbsolutePath());
	        /*
	        Link l = new Link(file.getName(),iwc.getIWMainApplication().getMediaServletURI());
	        l.addParameter(MediaWritable.PRM_WRITABLE_CLASS,IWMainApplication.getEncryptedClassName(DownloadWriter.class));
	        l.addParameter(DownloadWriter.PRM_ABSOLUTE_FILE_PATH,file.getAbsolutePath());
	        */
	        /*
	        Link l = new Link(getText(file.getName()),getCurrentFileUrl(iwc,file.getAbsolutePath()));
	        l.setTarget(Link.TARGET_BLANK_WINDOW);
	        */
	        return l;
	    }
		
	}
	private Link getDeleteFileLink(File file, IWContext iwc) {
		Link l = new Link(getDeleteIcon());
		l.addParameter(PRM_DELFILE,"true");
		String msg =this.iwrb.getLocalizedString("do_you_want_to_remove_this_file","Do you really want to delete the file");
		l.setOnClick("return confirm('"+msg+"  \\'" + file.getName() + " \\'?');");
		l.addParameter(PRM_FILE, file.getAbsolutePath());
		addMaintainedParameters(iwc,l);
		return l;
	}
	/*
	 * if (confirm(" " + msg)){ return true; }else{ return false; }
	 */
	private Link getUploadLink(File folder, IWContext iwc) {
		Link l = new Link(getText(this.iwrb.getLocalizedString("upload_file","Upload file")));
		l.addParameter(PRM_UPLOAD_FILE,"true");
		l.maintainParameter(PRM_FOLDER,iwc);
		addMaintainedParameters(iwc,l);
		return l;
	}
	private Link getCreateFolderLink(File parentfolder, IWContext iwc) {
		Link l = new Link(getText(this.iwrb.getLocalizedString("create_folder","Create folder")));
		l.addParameter(PRM_CREATE_FOLDER,"true");
		l.maintainParameter(PRM_FOLDER,iwc);
		addMaintainedParameters(iwc,l);
		return l;
	}
	private Text getHeaderText(String caption) {
		Text t = new Text(caption);
		t.setFontColor(this.headerFontColor);
		return t;
	}
	private Text getBoldText(String caption) {
		Text t = new Text(caption);
		t.setFontColor(this.boldFontColor);
		t.setBold(true);
		return t;
	}
	private Text getText(String caption) {
		Text t = new Text(caption);
		t.setFontColor(this.textFontColor);
		return t;
	}
	
	public void setBundleIdentifier(String identifier){
		this.bundleIdentifier = identifier;
	}
	
	
	private void addMaintainedParameters(IWContext iwc,Link link){
		if(this.maintainParameterNames!=null){
			for (Iterator iter = this.maintainParameterNames.iterator(); iter.hasNext();) {
				String prm = (String) iter.next();
				link.maintainParameter(prm,iwc);
			}
		}
	}
	
	/**
	 * @return Returns the skipFiles.
	 */
	public String[] getSkipFiles() {
		return this.skipFiles;
	}

	/**
	 * @param skipFiles The skipFiles to set.
	 */
	public void setSkipFiles(String[] skipFiles) {
		this.skipFiles = skipFiles;
	}

	/**
	 * @return Returns the skipFolders.
	 */
	public String[] getSkipFolders() {
		return this.skipFolders;
	}

	/**
	 * @param skipFolders The skipFolders to set.
	 */
	public void setSkipFolders(String[] skipFolders) {
		this.skipFolders = skipFolders;
	}

	/**
	 * @return Returns the sTOPFOLDER.
	 */
	public String getSTOPFOLDER() {
		return this.topLevelFolder;
	}

	/**
	 * @param stopfolder The sTOPFOLDER to set.
	 */
	public void setTopLevelBrowseFolder(String toplevel) {
		if(!toplevel.startsWith("/")) {
			this.topLevelFolder = "/"+toplevel;
		}
		else {
			this.topLevelFolder = toplevel;
//		System.out.println("Toplevel folder set to "+topLevelFolder);
		}
	}

	/**
	 * @param boldFontColor The boldFontColor to set.
	 */
	public void setBoldFontColor(String boldFontColor) {
		this.boldFontColor = boldFontColor;
	}

	/**
	 * @param fileBackgroundColor The fileBackgroundColor to set.
	 */
	public void setFileBackgroundColor(String fileBackgroundColor) {
		this.fileBackgroundColor = fileBackgroundColor;
	}

	/**
	 * @param folder The folder to set.
	 */
	public void setStartingFolderRealPath(String folder) {
		this.relativePath = false;
		this.folder = folder;
		if(this.topLevelFolder.equalsIgnoreCase("/")){
			setTopLevelBrowseFolder(folder);
		}
//		System.out.println("Path set to "+folder);
	}

	/**
	 * @param folder The folder to set.
	 */
	public void setStartingFolderRelativePath(String folder) {
		this.relativePath = true;
		this.folder = folder;
		if(this.topLevelFolder.equalsIgnoreCase("/")){
			setTopLevelBrowseFolder(folder);
		}
//		System.out.println("Path set to "+folder);
	}

	public void setFilesDeletable(boolean deletable){
		this.filesDeletable = deletable;
	}

	
	/**
	 * @param headerBackgroundColor The headerBackgroundColor to set.
	 */
	public void setHeaderBackgroundColor(String headerBackgroundColor) {
		this.headerBackgroundColor = headerBackgroundColor;
	}

	/**
	 * @param headerFontColor The headerFontColor to set.
	 */
	public void setHeaderFontColor(String headerFontColor) {
		this.headerFontColor = headerFontColor;
	}

	/**
	 * @param textFontColor The textFontColor to set.
	 */
	public void setTextFontColor(String textFontColor) {
		this.textFontColor = textFontColor;
	}
	
	
	/**
	 * Adds a parameter name to be maintained from the request
	 * @param prmName
	 */
	public void addMaintainedParameter(String prmName){
		if(this.maintainParameterNames==null) {
			this.maintainParameterNames =new Vector();
		}
		this.maintainParameterNames.add(prmName);
	}
	
	private class FolderFilter implements FileFilter {
		
		private String[] refuseFolders =null;
		public FolderFilter(String[] refuseFolders){
			this.refuseFolders =refuseFolders;
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File file) {
			if ((file.isDirectory())) {
				String name = file.getName();
				if(this.refuseFolders!=null){
					for (int i = 0; i < this.refuseFolders.length; i++) {
						int index = name.indexOf(this.refuseFolders[i]);
						if (index > -1) {
							return false;
						}
					}
				}
				return true;
			}
			return false;
		}
	}
	private class ResourceFileFilter implements FileFilter {
		
		private String[] refuseFiles =null;
		public ResourceFileFilter(String[] refuseFiles){
			this.refuseFiles =refuseFiles;
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File file) {
			if (!file.isDirectory()) {
				String name = file.getName();
				if(this.refuseFiles!=null){
					for (int i = 0; i < this.refuseFiles.length; i++) {
						int index = name.indexOf(this.refuseFiles[i]);
						if (index > -1) {
							return false;
						}
					}
				}
				return true;
			}
			return false;
		}
	}

    /**
     * @return Returns the displayFilesInFrame.
     */
    public boolean isDisplayFilesInFrame() {
        return this.displayFilesInFrame;
    }
    /**
     * @param displayFilesInFrame The displayFilesInFrame to set.
     */
    public void setDisplayFilesInFrame(boolean displayFilesInFrame) {
        this.displayFilesInFrame = displayFilesInFrame;
    }
    
    public void setDownloadFiles(boolean display){
        this.displayFilesInFrame = !display;
    }
    
   
}
