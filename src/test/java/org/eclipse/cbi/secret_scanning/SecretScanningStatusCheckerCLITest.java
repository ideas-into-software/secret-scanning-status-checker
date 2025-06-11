package org.eclipse.cbi.secret_scanning;

import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_TOKEN_NAME_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import picocli.CommandLine;

/**
 * Tests {@link org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerCLI}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Jun 9, 2025
 */
public class SecretScanningStatusCheckerCLITest {
	private static final String GITHUB_TOKEN_TEST = "testGitHubToken";
	private static final String GITHUB_ORG_NAME_TEST = "testGitHubOrgName";

	/**
	 * org.slf4j.simple.SimpleLogger sends all enabled log messages, for all defined
	 * loggers, to the console (System.err)
	 * {@link https://www.slf4j.org/api/org/slf4j/simple/SimpleLogger.html }
	 **/
	final PrintStream originalConsoleSystemErr = System.err;
	final ByteArrayOutputStream console = new ByteArrayOutputStream();

	@BeforeEach
	public void setUpStreams() {
		console.reset();
		System.setErr(new PrintStream(console));
	}

	@AfterEach
	public void restoreStreams() {
		System.setErr(originalConsoleSystemErr);
	}

	@Test
	@SetEnvironmentVariable(key = GITHUB_TOKEN_NAME_DEFAULT, value = GITHUB_TOKEN_TEST)
	public void testOrgNameParameter() throws IOException {
		List<String> args = List.of("--test-run", GITHUB_ORG_NAME_TEST);

		int exitCode = new CommandLine(new SecretScanningStatusCheckerCLI()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(console.toString()
				.contains(String.format("Checking repositories of '%s' GitHub organisation", GITHUB_ORG_NAME_TEST)));
	}

	@Test
	public void testMissingOrgNameParameter() {
		List<String> args = List.of("--test-run");

		int exitCode = new CommandLine(new SecretScanningStatusCheckerCLI()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(console.toString().contains("Missing required parameter: 'ORG_NAME'"));
	}

	@Test
	public void testMissingGitHubTokenEnvironmentVariable() throws IOException {
		List<String> args = List.of("--test-run", GITHUB_ORG_NAME_TEST);

		int exitCode = new CommandLine(new SecretScanningStatusCheckerCLI()).execute(args.toArray(String[]::new));
		assertEquals(1, exitCode);

		assertTrue(console.toString().contains("GITHUB_TOKEN environment variable is not set!"));
	}
}
