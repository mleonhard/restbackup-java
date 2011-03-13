/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.params.ConnConnectionPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
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
	public static final String HTTP_USER_AGENT;
	protected static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	protected static final int CONNECT_TIMEOUT_MS = 60 * 1000;
	protected static final int READ_TIMEOUT_MS = 60 * 60 * 1000;
	protected static final int SOCKET_BUFFER_SIZE = 128 * 1024;
	protected static final int MAX_REQUEST_ATTEMPTS = 6;

	private static final Logger _log;
	protected static final ThreadSafeClientConnManager _clientConnectionManager;
	protected static final HttpRequestRetryHandler _retryHandler = new HttpRequestRetryHandler() {
		@Override
		public boolean retryRequest(IOException e, int executionCount, HttpContext context) {
			return false;
		}
	};

	static {
		_log = Logger.getLogger(HttpCaller.class.getName());

		String clientVersion = "restbackup-java/" + VERSION;
		String javaVendor = System.getProperty("java.vendor", ""); // "Sun Microsystems Inc."
		String shortVendorName = javaVendor.split(" ")[0]; // Sun
		String javaVersionNumber = System.getProperty("java.version", ""); // 1.6.0_21
		String javaVersion = shortVendorName + "/" + javaVersionNumber;
		String osName = System.getProperty("os.name", "").replaceAll(" ", ""); // Windows7
		String osVersionNumber = System.getProperty("os.version", ""); // 6.1
		String osArch = System.getProperty("os.arch", ""); // x86
		String osVersion = osName + "/" + osVersionNumber + "-" + osArch;
		HTTP_USER_AGENT = String.format("%s %s %s", clientVersion, javaVersion, osVersion);
		_log.config("User-Agent: " + HTTP_USER_AGENT);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
		schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
		_clientConnectionManager = new ThreadSafeClientConnManager(schemeRegistry);
		// max open connections to all endpoints
		_clientConnectionManager.setMaxTotal(200);
		// max open connections to any endpoint
		_clientConnectionManager.setDefaultMaxPerRoute(200);
	}

	protected final HttpClient _httpClient;

	protected final String _accessUrl;
	protected final String _scheme;
	protected final String _username;
	protected final String _password;
	protected final String _host;
	protected final int _port;

	protected int _maxRequestAttempts = MAX_REQUEST_ATTEMPTS;

	/**
	 * Gets the default client connection manager shared by all callers
	 */
	public static ThreadSafeClientConnManager getClientConnectionManager() {
		return _clientConnectionManager;
	}

	/**
	 * Makes a new http client, using the client connection manager and retry
	 * policy
	 * 
	 * @return a new http client object
	 */
	public static DefaultHttpClient makeHttpClient() {
		DefaultHttpClient httpClient = new DefaultHttpClient(_clientConnectionManager);
		httpClient.setHttpRequestRetryHandler(_retryHandler);
		return httpClient;
	}

	/**
	 * Returns the http client used by this caller
	 */
	public HttpClient getHttpClient() {
		return _httpClient;
	}

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

	/**
	 * The caller will retry failed requests
	 * 
	 * @return the maximum number of times the caller will attempt a request
	 */
	public int getMaxRequestAttempts() {
		return _maxRequestAttempts;
	}

	/**
	 * The caller will retry failed requests. Use this function to set the
	 * maximum number of attempts for any request.
	 * 
	 * @param maxAttempts
	 *            the new value, must be > 0
	 */
	public void setMaxRequestAttempts(int maxAttempts) {
		if (_maxRequestAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be > 0");
		}
		_maxRequestAttempts = maxAttempts;
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
		this(accessUrl, makeHttpClient());
	}

	/**
	 * Do not use this class directly. Use BackupApiCaller or
	 * ManagementApiCaller instead.
	 * 
	 * @param accessUrl
	 *            a string with the form "https://USER:PASS@host/"
	 * @param maxRequestAttempts
	 *            the maximum number of times the to attempt any failing
	 *            request, must be > 0
	 * @throws IllegalArgumentException
	 *             if the access url is malformed
	 */
	public HttpCaller(String accessUrl, int maxRequestAttempts) throws IllegalArgumentException {
		this(accessUrl);
		setMaxRequestAttempts(maxRequestAttempts);
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

		_log.config(String.format("Using access-url %s://%s:***@%s:%d/", _scheme, _username, _host,
				_port));

		_httpClient = httpClient;
		HttpParams params = _httpClient.getParams();
		params.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, false);
		params.setParameter(CoreProtocolPNames.USER_AGENT, HTTP_USER_AGENT);
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECT_TIMEOUT_MS);
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, READ_TIMEOUT_MS);
		params.setIntParameter(ConnConnectionPNames.MAX_STATUS_LINE_GARBAGE, 0);
		params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);
		params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
		Collection<Header> defaultHeaders = new LinkedList<Header>();
		String authHeaderValue = "Basic " + encodeBase64(_username + ":" + _password);
		defaultHeaders.add(new BasicHeader("Authorization", authHeaderValue));
		params.setParameter(ClientPNames.DEFAULT_HEADERS, defaultHeaders);
		HttpHost endpoint = new HttpHost(_host, _port, _scheme);
		params.setParameter(ClientPNames.DEFAULT_HOST, endpoint);

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
	 * @param failedAttempts
	 *            the number of request attempts that have failed
	 * @return the number of milliseconds to delay before retrying the request
	 */
	private static long delayMillis(int failedAttempts) {
		final long[] RETRY_DELAY_MILLIS = new long[] { 0, 100, 1000, 3000, 10000, 60000, 300000 };
		if (failedAttempts < RETRY_DELAY_MILLIS.length) {
			return RETRY_DELAY_MILLIS[failedAttempts];
		} else {
			return 600000;
		}
	}

	/**
	 * Performs the request once
	 * 
	 * @param request
	 *            the request to perform
	 * @return a response object if the response was 2xx
	 * @throws RetryableException
	 *             on network or 5xx errors
	 * @throws RestBackupException
	 *             on all other errors
	 */
	protected HttpResponse executeRequest(HttpUriRequest request) throws RetryableException,
			RestBackupException {
		HttpResponse response;
		try {
			response = _httpClient.execute(request);
		} catch (HttpHostConnectException e) {
			throw new RetryableException(e);
		} catch (NoHttpResponseException e) {
			throw new RetryableException(e);
		} catch (SocketTimeoutException e) {
			throw new RetryableException(e);
		} catch (SSLHandshakeException e) {
			throw new RetryableException(e);
		} catch (ClientProtocolException e) {
			throw new RetryableException(e);
		} catch (IOException e) {
			throw new RetryableException(e);
		}

		_log.info("Received response: " + response.getStatusLine());
		if (response.containsHeader("X-RequestID")) {
			_log.info(response.getLastHeader("X-RequestID").toString());
		}
		if (response.containsHeader("Content-Length")) {
			_log.fine(response.getLastHeader("Content-Length").toString());
		}
		if (response.containsHeader("Content-Type")) {
			_log.fine(response.getLastHeader("Content-Type").toString());
		}
		if (response.containsHeader("Location")) {
			_log.fine(response.getLastHeader("Location").toString());
		}

		int code = response.getStatusLine().getStatusCode();
		if (200 <= code && code <= 299) {
			return response; // Success
		} else if (code == 401) {
			closeResponseEntityInputStream(response);
			throw new UnauthorizedException(response);
		} else if (500 <= code && code <= 599) { // 5xx error, retry
			closeResponseEntityInputStream(response);
			throw new RetryableException(new RestBackupException("Received error response",
					response));
		} else {
			closeResponseEntityInputStream(response);
			throw new RestBackupException("Received error response", response);
		}
	}

	/**
	 * Executes the http request using the client, retrying on network and 5xx
	 * errors. Uses an increasing delay between retries.
	 * 
	 * @param request
	 *            the request to execute
	 * @return the response object, if the response status code is 2xx
	 * @throws UnauthorizedException
	 *             if the response is 401 Not Authorized
	 * @throws RestBackupException
	 *             on all errors
	 */
	protected HttpResponse executeRequestWithRetries(HttpUriRequest request)
			throws RestBackupException {
		_log.info("Executing request: " + request.getRequestLine());
		if (request instanceof HttpEntityEnclosingRequestBase) {
			HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
			if (entity.getContentLength() < 0) {
				_log.fine("Content-Length: unknown");
			} else {
				_log.fine(String.format("Content-Length: %d", entity.getContentLength()));
			}
			if (entity.getContentType() == null) {
				_log.fine("Content-Type: unknown");
			} else {
				_log.fine(entity.getContentType().toString());
			}
		}
		RetryableException lastException = null;
		for (int attempt = 0; attempt < _maxRequestAttempts; attempt++) {
			_log.fine("attempt " + attempt);

			if (attempt > 0) {
				try {
					long delayMillis = delayMillis(attempt);
					System.out.println("Delaying " + delayMillis + " milliseconds");
					Thread.sleep(delayMillis);
				} catch (InterruptedException e) {
				}
			}
			// TODO: rewind entity stream
			try {
				return executeRequest(request);
			} catch (RetryableException e) {
				_log.warning(e.toString());
				lastException = e;
			}
		}
		throw new RestBackupException("Request failed after " + _maxRequestAttempts + " attempts",
				lastException.getOriginalException());
	}

	/**
	 * Checks the response object for the expected status code
	 * 
	 * @param response
	 *            the response received from the API
	 * @param expectedCode
	 *            the expected status code
	 * @throws RestBackupException
	 *             if the expected response was not received
	 */
	protected void expectStatusCode(HttpResponse response, int expectedCode)
			throws UnauthorizedException, RestBackupException {
		int code = response.getStatusLine().getStatusCode();
		if (code != expectedCode) {
			closeResponseEntityInputStream(response);
			throw new RestBackupException(String.format("Expected status code %d, received %d",
					expectedCode, code), response);
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
		return executeRequestWithRetries(httpGet);
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
		return executeRequestWithRetries(httpPut);
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
		if (params == null) {
			params = new String[0];
		}
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
		HttpEntity entity = makeFormUrlencodedEntity(postParams);
		httpPost.setEntity(entity);
		try {
			_log.info("Post body: " + EntityUtils.toString(entity));
		} catch (IOException e) {
			throw new RestBackupException(e); // should never happen
		}
		HttpParams params = httpPost.getParams();
		params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
		params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
		return executeRequestWithRetries(httpPost);
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
		return executeRequestWithRetries(httpDelete);
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

	/**
	 * Closes the input stream of the response, releasing the http connection
	 * back to the pool
	 * 
	 * @param response
	 *            a response object whose entity is no longer needed
	 */
	public static void closeResponseEntityInputStream(HttpResponse response) {
		try {
			response.getEntity().getContent().close();
		} catch (Exception e) {
		}
	}
}
