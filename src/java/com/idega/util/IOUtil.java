package com.idega.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWModuleLoader;
import com.idega.io.ZipInstaller;

public class IOUtil {

	public static final void close(Closeable closeable) {
		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (IOException e) {}
	}

	public static final void closeInputStream(InputStream stream) {
		close(stream);
	}

	public static final void closeOutputStream(OutputStream stream) {
		close(stream);
	}

	public static final void closeReader(Reader reader) {
		close(reader);
	}

	public static final void closeWriter(Writer writer) {
		close(writer);
	}

	public static final InputStream getStreamFromCurrentZipEntry(ZipInputStream zipStream) throws IOException {
		if (zipStream == null) {
			throw new IOException("Unable to read from provided zip stream");
		}

		ZipInstaller zipInstaller = new ZipInstaller();

		ByteArrayOutputStream memory = new ByteArrayOutputStream();
		zipInstaller.writeFromStreamToStream(zipStream, memory);
		return new ByteArrayInputStream(memory.toByteArray());
	}

	@SuppressWarnings("unchecked")
	public static final InputStream getStreamFromJar(String bundleIdentifier, String filePathWithinJar) {
		Set<String> paths = IWMainApplication.getDefaultIWMainApplication().getResourcePaths(IWModuleLoader.DEFAULT_LIB_PATH);
		if (ListUtil.isEmpty(paths)) {
			return null;
		}

		String expectedBundleJar = new StringBuilder(bundleIdentifier).append("-").toString();
		String bundleJar = null;
		for (Iterator<String> pathsIter = paths.iterator(); (pathsIter.hasNext() && bundleJar == null);) {
			bundleJar = pathsIter.next();

			if (bundleJar.indexOf(expectedBundleJar) == -1) {
				bundleJar = null;	//	Not bundle's JAR file
			}
		}
		if (StringUtil.isEmpty(bundleJar)) {
			return null;
		}

		if (bundleJar.startsWith(File.separator)) {
			bundleJar = bundleJar.replaceFirst(File.separator, CoreConstants.EMPTY);
		}

		String jarPath = IWMainApplication.getDefaultIWMainApplication().getApplicationRealPath() + bundleJar;
		JarInputStream jarStream = null;
		try {
			jarStream = new JarInputStream(new FileInputStream(new File(jarPath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (jarStream == null) {
			return null;
		}

		try {
			for (ZipEntry entry = null; (entry = jarStream.getNextEntry()) != null;) {
				if (entry.getName().equals(filePathWithinJar) && !entry.isDirectory()) {
					return getStreamFromCurrentZipEntry(jarStream);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static final boolean isStreamValid(InputStream stream) {
		try {
			return stream != null && stream.available() >= 0;
		} catch (Exception e) {}
		return false;
	}

	public static byte[] getBytesFromInputStream(InputStream stream) {
		if (!isStreamValid(stream)) {
			return null;
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			FileUtil.streamToOutputStream(stream, output);
			return output.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(stream);
			close(output);
		}

		return null;
	}

	public static byte[] getBytesFromObject(Serializable object) {
		if (object == null)
			return null;

		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			return baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(oos);
			close(baos);
		}

		return null;
	}

	public static <T extends Serializable> T getObjectFromInputStream(InputStream stream) {
		if (stream == null)
			return null;

		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(stream);
			@SuppressWarnings("unchecked")
			T object = (T) ois.readObject();
			return object;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(ois);
			close(stream);
		}

		return null;
	}

	public static final long getRequestSize(HttpServletRequest request) {
		if (request == null)
			return 0;

		Long requestSize = null;
		try {
			requestSize = Long.valueOf(request.getHeader("Content-Length"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return requestSize == null ? 0 : requestSize;
	}

	public static final boolean isUploadExceedingLimits(HttpServletRequest request, long maxUploadSize) {
		Long requestSize = getRequestSize(request);
		return requestSize == null ? false : requestSize > maxUploadSize;
	}
}