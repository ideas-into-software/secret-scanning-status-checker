package org.eclipse.cbi.secret_scanning;

/**
 * Defines constants used by
 * {@link org.eclipse.cbi.secret_scanning.SecretScanningStatusChecker} and
 * {@link org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerCLI}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Jun 9, 2025
 */
public interface SecretScanningStatusCheckerConstants {
	String GITHUB_TOKEN_NAME_DEFAULT = "GITHUB_TOKEN";

	String GITHUB_REST_API_BASE_URI = "https://api.github.com";
	String GITHUB_REST_API_VERSION = "2022-11-28";
	String GITHUB_REST_API_ACCEPT = "application/vnd.github+json";
	String GITHUB_REST_API_USER_AGENT = "secret-scanning-status-checker";
	String GITHUB_REST_API_ORGS_LIST_REPOS_PATH = "/orgs/%s/repos";

	int DEFAULT_CONNECT_TIMEOUT = 10;
	int DEFAULT_REQUEST_TIMEOUT = 10;

	String GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_FULL_NAME = "full_name";
	String GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECURITY_AND_ANALYSIS = "security_and_analysis";
	String GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECRET_SCANNING = "secret_scanning";
}
