package io.pantheist.testclient.api;

/**
 * Generated by:
 *
 * - 405 Method Not Allowed (if the management function exists but the
 * particular operation is not supported for it)
 *
 * - 501 Not Implemented (if that particular management function does not exist
 * for that resource)
 */
public class ManagementUnsupportedException extends RuntimeException
{
	private static final long serialVersionUID = -5701870026573274627L;

	public ManagementUnsupportedException(final String message)
	{
		super(message);
	}
}
