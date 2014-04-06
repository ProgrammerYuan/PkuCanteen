package edu.pku.sxt.pkuc.client.util;

/**
 * Exception happens in HttpManager class
 * @author songxintong
 *
 */
public class HttpManagerException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public HttpManagerException () { super(); }	
	public HttpManagerException (String msg) { super(msg); }
	
}
