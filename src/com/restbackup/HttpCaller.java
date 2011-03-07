/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnConnectionPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * Base class that performs HTTP requests to RestBackup(tm) access-urls with
 * authentication. Use BackupApiCaller and ManagementApiCaller classes instead
 * of this class.
 * 
 * @author Michael Leonhard
 */
public class HttpCaller {
	public static final String VERSION = "1.0";
	public static final String HTTP_USER_AGENT = makeHttpUserAgentString();
	public static final int MAX_ATTEMPTS = 5;
	public static final int FIRST_RETRY_DELAY_SECONDS = 1;
	protected static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	protected static final int CONNECT_TIMEOUT_MS = 60 * 1000;
	protected static final int SOCKET_BUFFER_SIZE = 128 * 1024;

	protected final HttpClient _httpClient;

	protected final String _accessUrl;
	protected final String _scheme;
	protected final String _username;
	protected final String _password;
	protected final String _host;
	protected final int _port;

	public String getAccessUrl() {
		return _accessUrl;
	}

	public String getScheme() {
		return _scheme;
	}

	public String getUsername() {
		return _username;
	}

	public String getPassword() {
		return _password;
	}

	public String getHost() {
		return _host;
	}

	public int getPort() {
		return _port;
	}

	protected static String makeHttpUserAgentString() {
		String userAgent = "restbackup-java/" + VERSION;

		String javaVendor = System.getProperty("java.vendor", ""); // "Sun Microsystems Inc."
		String shortVendorName = javaVendor.split(" ")[0]; // Sun
		String javaVersion = System.getProperty("java.version", ""); // 1.6.0_21
		userAgent = userAgent + " " + shortVendorName + "/" + javaVersion;

		String osName = System.getProperty("os.name", ""); // Windows 7
		String osNameNoSpaces = osName.replaceAll(" ", ""); // Windows7
		String osVersion = System.getProperty("os.version", ""); // 6.1
		String osArch = System.getProperty("os.arch", ""); // x86
		userAgent = userAgent + " " + osNameNoSpaces + "/" + osVersion + "-" + osArch;

		return userAgent;
	}

	/**
	 * Encodes the string as utf-8 and then base64. For example,
	 * encodeBase64("abcd") yields "YWJjZA==".
	 * 
	 * @param s
	 *            the string to encode
	 * @return the string s encoded in utf-8 and base64, with no trailing
	 *         newline
	 */
	public static String encodeBase64(String s) {
		return DatatypeConverter.printBase64Binary(s.getBytes(UTF8_CHARSET));
	}

	/**
	 * Do not use this class directly. Use BackupApiCaller or
	 * ManagementApiCaller instead.
	 * 
	 * @param accessUrl
	 *            a string with the form "https://USER:PASS@host/"
	 * @throws IllegalArgumentException
	 *             if the access url is malformed
	 */
	public HttpCaller(String accessUrl) throws IllegalArgumentException {
		this(accessUrl, new DefaultHttpClient());
	}

	/**
	 * Do not use this class directly. Use BackupApiCaller or
	 * ManagementApiCaller instead.
	 * 
	 * @param accessUrl
	 *            a string with the form "https://USER:PASS@host/"
	 * @param httpClient
	 *            the client to use for requests from this caller
	 * @throws IllegalArgumentException
	 *             if the access url is malformed
	 */
	public HttpCaller(String accessUrl, HttpClient httpClient) throws IllegalArgumentException {
		String ACCESS_URL_REGEX = "^(https?)://([a-zA-Z0-9]+):([a-zA-Z0-9]+)@([-.a-zA-Z0-9]+)(?::([0-9]+))?/$";
		Pattern pattern = Pattern.compile(ACCESS_URL_REGEX);
		Matcher matcher = pattern.matcher(accessUrl);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid access url '" + accessUrl + "'");
		}
		_accessUrl = accessUrl;
		_scheme = matcher.group(1);
		_username = matcher.group(2);
		_password = matcher.group(3);
		_host = matcher.group(4);
		String portString = matcher.group(5);
		if (portString == null && _scheme.equals("https")) {
			_port = 443;
		} else if (portString == null) {
			_port = 80;
		} else {
			_port = Integer.valueOf(portString);
		}

		_httpClient = httpClient;
		HttpParams params = _httpClient.getParams();
		params.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, false);
		params.setParameter(CoreProtocolPNames.USER_AGENT, HTTP_USER_AGENT);
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECT_TIMEOUT_MS);
		params.setIntParameter(ConnConnectionPNames.MAX_STATUS_LINE_GARBAGE, 0);

		Collection<Header> defaultHeaders = new LinkedList<Header>();
		String authHeaderValue = "Basic " + encodeBase64(_username + ":" + _password);
		defaultHeaders.add(new BasicHeader("Authorization", authHeaderValue));
		params.setParameter(ClientPNames.DEFAULT_HEADERS, defaultHeaders);
		params.setParameter(ClientPNames.DEFAULT_HOST, new HttpHost(_host, _port, _scheme));

		// TODO: Make sure that certificates and hostnames are verified, see
		// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
	}

	/**
	 * Shut down the api caller and release resources.
	 */
	public void shutdown() {
		_httpClient.getConnectionManager().shutdown();
	}

	/**
	 * Execute the request using the client. Re-throws expected exceptions as
	 * RestBackupException.
	 * 
	 * @param request
	 *            the request to execute
	 * @return the response object
	 * @throws RestBackupException
	 */
	protected HttpResponse executeRequest(HttpUriRequest request) throws RestBackupException {
		// TODO: Make sure that retries are performed correctly
		try {
			return _httpClient.execute(request);
		} catch (ClientProtocolException e) {
			throw new RestBackupException(e);
		} catch (IOException e) {
			throw new RestBackupException(e);
		}
	}

	/**
	 * Checks the response object for the expected status code
	 * 
	 * @param response
	 *            the response received from the API
	 * @param expectedCode
	 *            the expected status code
	 * @throws UnauthorizedException
	 *             if the response is 401 Not Authorized
	 * @throws RestBackupException
	 *             if the expected response was not received
	 */
	protected void expectStatusCode(HttpResponse response, int expectedCode)
			throws UnauthorizedException, RestBackupException {
		int code = response.getStatusLine().getStatusCode();
		if (code == expectedCode) {
			return;
		} else if (200 <= code && code <= 299) { // Success
			throw new RestBackupException("Unexpected response", response);
		} else if (code == 401) { // Unauthorized
			throw new UnauthorizedException(response);
		} else {
			throw new RestBackupException(response);
		}
	}

	/**
	 * Performs an HTTP GET request of the specified resource
	 * 
	 * @param uri
	 *            a string of the form "/path/to/resource"
	 * @param extraHeader
	 *            an extra HTTP header to send with the request, may be null
	 * @return the response object
	 * @throws IllegalArgumentException
	 *             if the uri is malformed
	 * @throws RestBackupException
	 *             on error
	 */
	protected HttpResponse doGet(String uri, Header extraHeader) throws RestBackupException,
			IllegalArgumentException {
		if (uri == null || uri.length() < 1 || uri.charAt(0) != '/') {
			throw new IllegalArgumentException("Uri is mal-formed '" + uri + "'");
		}
		HttpGet httpGet = new HttpGet(uri);
		HttpParams params = httpGet.getParams();
		params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
		params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, SOCKET_BUFFER_SIZE);
		if (extraHeader != null) {
			httpGet.addHeader(extraHeader);
		}
		return executeRequest(httpGet);
	}

	/**
	 * Performs an HTTP PUT request, uploading the supplied entity to the
	 * specified resource name
	 * 
	 * @param uri
	 *            a string of the form "/path/of/new/resource"
	 * @param entity
	 *            an entity containing the data to upload
	 * @return a response object
	 * @throws RestBackupException
	 */
	protected HttpResponse doPut(String uri, HttpEntity entity) throws RestBackupException {
		if (uri == null || uri.length() < 1 || uri.charAt(0) != '/') {
			throw new IllegalArgumentException("Uri is mal-formed '" + uri + "'");
		}
		HttpPut httpPut = new HttpPut(uri);
		httpPut.setEntity(entity);
		HttpParams params = httpPut.getParams();
		params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
		params.setIntParameter(CoreProtocolPNames.WAIT_FOR_CONTINUE, 10 * 1000);
		params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, false);
		params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, SOCKET_BUFFER_SIZE);
		return executeRequest(httpPut);
	}

	/**
	 * Creates an entity by converting the supplied parameters to UTF-8 and
	 * url-encoding
	 * 
	 * @param params
	 *            an array of parameter name and value pairs, such as
	 *            {"param1","value1","param2","value2"}
	 * @returns an entity with Content-Type
	 *          "application/x-www-form-urlencoded; charset=UTF-8" and the
	 *          provided parameters
	 */
	protected static HttpEntity makeFormUrlencodedEntity(String[] params)
			throws RestBackupException {
		if (params.length % 2 == 1) { // odd number of items
			throw new IllegalArgumentException("Params must have an even number of strings");
		}
		List<NameValuePair> pairList = new ArrayList<NameValuePair>();
		for (int n = 0; n < params.length; n += 2) {
			pairList.add(new BasicNameValuePair(params[n], params[n + 1]));
		}
		try {
			return new UrlEncodedFormEntity(pairList, "UTF-8");
		} catch (UnsupportedEncodingException e) { // should never happen
			throw new RestBackupException(e);
		}
	}

	/**
	 * Performs an HTTP POST request, sending the supplied entity to the
	 * specified resource
	 * 
	 * @param uri
	 *            the path of the resource which is the target of the post, such
	 *            as "/path/to/resource/receiving/upload"
	 * @param postParams
	 *            an array of strings of the form
	 *            {"param1","value1","param2","value2"}
	 * @throws RestBackupException
	 */
	protected HttpResponse doPost(String uri, String[] postParams) throws RestBackupException {
		if (uri == null || uri.length() < 1 || uri.charAt(0) != '/') {
			throw new IllegalArgumentException("Uri is mal-formed '" + uri + "'");
		}
		HttpPost httpPost = new HttpPost(uri);
		if (postParams != null) {
			HttpEntity entity = makeFormUrlencodedEntity(postParams);
			httpPost.setEntity(entity);
		}
		HttpParams params = httpPost.getParams();
		params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
		params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
		return executeRequest(httpPost);
	}

	/**
	 * Performs an HTTP DELETE request of the specified resource
	 * 
	 * @param uri
	 *            a string of the form "/path/to/resource"
	 * @return the response object
	 * @throws IllegalArgumentException
	 *             if the uri is malformed
	 * @throws RestBackupException
	 *             on error
	 */
	protected HttpResponse doDelete(String uri) throws RestBackupException,
			IllegalArgumentException {
		if (uri == null || uri.length() < 1 || uri.charAt(0) != '/') {
			throw new IllegalArgumentException("Uri is mal-formed '" + uri + "'");
		}
		HttpDelete httpDelete = new HttpDelete(uri);
		HttpParams params = httpDelete.getParams();
		params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
		return executeRequest(httpDelete);
	}

	/**
	 * Returns a string representation of the object, such as
	 * "HttpCaller(accessUrl=" https://Y21:Mj313x@us.restbackup.com/")"
	 */
	public String toString() {
		return String.format("HttpCaller(accessUrl=\"%s\")", _accessUrl);
	}

	/**
	 * Reads the response body entity and returns it as a string
	 * 
	 * @param response
	 *            a response object containing the entity to read
	 * @return a string with the body of the response
	 * @throws RestBackupException
	 *             on error
	 */
	public static String readEntity(HttpResponse response) throws RestBackupException {
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			throw new RestBackupException("Response contains no body.");
		}
		try {
			return EntityUtils.toString(entity);
		} catch (Exception e) {
			throw new RestBackupException("Error reading response body", e, response);
		}
	}
}

//    def call(self, method, uri, body=None, extra_headers={}):
//        """Performs an HTTP request, retrying on 5xx error, with
//        exponential backoff.  Body may be None, a string, or a
//        RewindableSizedInputStream.
//        
//        Raises RestBackupException on error.
//        """
//        encoded_uri = uri.encode('utf-8')
//        retry_delay_seconds = FIRST_RETRY_DELAY_SECONDS
//        for attempt in xrange(0, MAX_ATTEMPTS):
//            try:
//                h = self.get_http_connection()
//                h.request(method, encoded_uri, body, headers)
//                response = h.getresponse()
//            except Exception, e:
//                raise RestBackupException(e)
//            if response.status >= 200 and response.status < 300:
//                return response # 2xx success
//            elif response.status >= 500 and response.status < 600:
//                pass # 5xx retry
//            else: # 4xx fail
//                description = "%s %s" % (response.status, response.reason)
//                raise RestBackupException(description)
//            
//            time.sleep(retry_delay_seconds)
//            retry_delay_seconds *= 2 # exponential backoff
//            if hasattr(body, read):
//                body.rewind()
//        
//        # raise last 5xx
//        raise RestBackupException("Gave up after attempt %s failed: %s %s"
//                                  % (attempt, response.status, response.reason))
//
//
//
//class InputStream(object):
//    """Interface for sized input stream classes.  Subclasses should
//    inherit from this class and override read_once(size).  This class
//    provides a default read(size) method that calls read_once(size)
//    and performs minimal buffering.
//    """
//    def __init__(self):
//        self.parent_read_buffer = ''
//    
//    def read(self, size=-1):
//        """Reads the stream's data source and returns a non-unicode
//        string up to size bytes.  Returns less than size bytes or ''
//        on EOF.  Reads all data up to EOF if size is negative or
//        omitted.  Raises IOError on error."""
//        if size == 0:
//            return ''
//        if size < 0:
//            chunks = [self.parent_read_buffer]
//            self.parent_read_buffer = ''
//            while True:
//                chunk = self.read_once(128*1024)
//                if not chunk:
//                    break
//                chunks.append(chunk)
//            return ''.join(chunks)
//        else:
//            while(len(self.parent_read_buffer) < size):
//                bytes_needed = size - len(self.parent_read_buffer)
//                chunk = self.read_once(bytes_needed)
//                if not chunk:
//                    break
//                self.parent_read_buffer = self.parent_read_buffer + chunk
//            chunk = self.parent_read_buffer[:size]
//            self.parent_read_buffer = self.parent_read_buffer[size:]
//            return chunk
//    
//    def read_once(self, size):
//        """Reads the stream's data source and returns a non-unicode
//        string up to size bytes.  May return less than size bytes.
//        Returns '' on EOF.  Size must be an integer greater than zero.
//        Raises IOError on error.
//        
//        Subclasses should override this method.
//        """
//        raise NotImplementedError()
//
//    def close(self):
//        """Closes the stream and releases resources.  This method may
//        be called multiple times with no negative effects.  Do not
//        call any other methods on the object after calling this
//        method."""
//        pass
//
//
//class SizedInputStream(InputStream):
//    """Interface for input streams with a known size.  Raises IOError
//    on read if the bytes are not available."""
//    def __init__(self, stream_length):
//        InputStream.__init__(self)
//        self.stream_length = stream_length
//    
//    def __len__(self):
//        """Returns the total number of bytes contained in the stream.
//        This must not change over the lifetime of the object."""
//        return self.stream_length
//    
//    def __nonzero__(self):
//        return True
//
//
//class RewindableSizedInputStream(SizedInputStream):
//    """Interface for rewindable input streams with a known size."""
//    def __init__(self, stream_length):
//        SizedInputStream.__init__(self, stream_length)
//    
//    def rewind(self):
//        """Rewinds the stream back to the beginning.  This method
//        allows us to retry network requests."""
//        raise NotImplementedError()
//
//
//class StringReader(RewindableSizedInputStream):
//    """Rewindable sized Input stream that sources its data from a string."""
//    def __init__(self, data):
//        """Data must be a byte string"""
//        if not isinstance(data, str):
//            raise TypeError('StringReader supports only str data')
//        stream_length = len(data)
//        RewindableSizedInputStream.__init__(self, stream_length)
//        self.data = data
//        self.rewind()
//    
//    def read(self, size=-1):
//        if size < 0:
//            size = len(self.data)
//        first_byte_index = self.next_byte_index
//        self.next_byte_index += size
//        return self.data[first_byte_index:self.next_byte_index]
//    
//    def read_once(self, size):
//        return self.read(size)
//    
//    def rewind(self):
//        self.next_byte_index = 0
//
//
//class FileObjectReader(RewindableSizedInputStream):
//    """Rewindable sized input stream that sources its data from a file
//    object.  The file object must support the seek(0) method."""
//    def __init__(self, f, size):
//        self.file = f
//        RewindableSizedInputStream.__init__(self, size)
//    
//    def read(self, size=-1):
//        return self.file.read(size)
//    
//    def read_once(self, size):
//        return self.read(size)
//    
//    def close(self):
//        self.file.close()
//        self.file = None
//    
//    def rewind(self):
//        self.file.seek(0)
//
//
//class FileReader(FileObjectReader):
//    """Rewindable sized input stream that sources its data from a file
//    with the specified name."""
//    def __init__(self, filename):
//        f = open(filename, 'rb')
//        size = os.path.getsize(filename)
//        FileObjectReader.__init__(self, f, size)
//
//
//class HttpResponseReader(SizedInputStream):
//    """Sized input stream that sources its data from an
//    http.HTTPResponse object."""
//    def __init__(self, http_response):
//        content_length = http_response.getheader('Content-Length')
//        stream_size = int(content_length)
//        SizedInputStream.__init__(self, stream_size)
//        self.http_response = http_response
//    
//    def read_once(self, size=-1):
//        return self.http_response.read(size)

