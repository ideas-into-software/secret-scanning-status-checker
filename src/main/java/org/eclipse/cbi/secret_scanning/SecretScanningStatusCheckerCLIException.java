package org.eclipse.cbi.secret_scanning;

/**
 * Exception thrown by the {@link SecretScanningStatusCheckerCLI}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Jun 9, 2025
 */
public class SecretScanningStatusCheckerCLIException extends RuntimeException {
	private static final long serialVersionUID = 6862444816341978423L;

	/**
	 * Create exception with the supplied error message
	 * 
	 * @param message
	 */
	public SecretScanningStatusCheckerCLIException(String message) {
		super(message);
	}

	/**
	 * Create exception with the supplied error message and cause
	 * 
	 * @param message
	 * @param cause
	 */
	public SecretScanningStatusCheckerCLIException(String message, Throwable cause) {
		super(message, cause);
	}
}