package net.meisen.general.server.http.listener.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.BasicHttpEntity;

/**
 * Utility class when working with <code>HttpRequest</code> instances.
 * 
 * @author pmeisen
 * 
 */
public class RequestFileHandlingUtilities {

	/**
	 * The Class RequestWrapper.
	 */
	private static class RequestWrapper implements RequestContext {

		/**
		 * The request.
		 */
		private final HttpEntityEnclosingRequest request;

		/**
		 * Instantiates a new request wrapper.
		 * 
		 * @param request
		 *            the {@code HttpEntityEnclosingRequest} of the file upload
		 */
		public RequestWrapper(final HttpEntityEnclosingRequest request) {
			this.request = request;
		}

		/**
		 * Implementation of the {@code getCharacterEncoding()} method.
		 * 
		 * @see RequestContext#getCharacterEncoding()
		 */
		public String getCharacterEncoding() {
			return request.getEntity().getContentEncoding() == null ? "ISO-8859-1"
					: request.getEntity().getContentEncoding().getValue();
		}

		/**
		 * Implementation of the {@code getContentLength()} method.
		 */
		public int getContentLength() {
			return (int) request.getEntity().getContentLength();
		}

		/**
		 * Implementation of the {@code getCharacterEncoding()} method.
		 * 
		 * @see RequestContext#getContentType()
		 */
		public String getContentType() {
			return request.getEntity().getContentType().getValue();
		}

		/**
		 * Implementation of the {@code getCharacterEncoding()} method.
		 * 
		 * @see RequestContext#getInputStream()
		 */
		public InputStream getInputStream() throws IOException {
			return ((BasicHttpEntity) request.getEntity()).getContent();
		}
	}

	/**
	 * Handles a file uploaded contained in the specified {@code request}. The
	 * method uses a {@code DiskFileItemFactory} with a {@code sizeThreshold}
	 * set to {@code 0}. If you use FileUpload, please make sure to add the
	 * needed dependencies, i.e. {@code commons-fileupload} and
	 * {@code servlet-api} (later will be removed in a newer version of the
	 * {@code commons-fileupload}.
	 * 
	 * @param request
	 *            the request to handle the file upload from
	 * 
	 * @return the {@code List} of loaded files
	 * 
	 * @throws FileUploadException
	 *             if the file upload fails
	 * 
	 * @see DiskFileItemFactory
	 */
	public static List<FileItem> handleFileUpload(final HttpRequest request)
			throws FileUploadException {
		final DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(0);
		return handleFileUpload(request, factory);
	}

	/**
	 * Handles a file contained in the specified {@code request}. If you use
	 * FileUpload, please make sure to add the needed dependencies, i.e.
	 * {@code commons-fileupload} and {@code servlet-api} (later will be removed
	 * in a newer version of the {@code commons-fileupload}.
	 * 
	 * @param request
	 *            the request to handle the file upload from
	 * @param factory
	 *            the {@code FileItemFactory} used to handle the request
	 * 
	 * @return the {@code List} of loaded files
	 * 
	 * @throws FileUploadException
	 *             if the file upload fails
	 */
	public static List<FileItem> handleFileUpload(final HttpRequest request,
			final FileItemFactory factory) throws FileUploadException {
		final List<FileItem> items;
		if (request instanceof HttpEntityEnclosingRequest
				&& "POST".equals(request.getRequestLine().getMethod())) {

			// create the ServletFileUpload
			final FileUpload upload = new FileUpload(factory);

			final HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
			items = upload.parseRequest(new RequestWrapper(entityRequest));
		} else {
			items = new ArrayList<FileItem>();
		}

		return items;
	}
}
