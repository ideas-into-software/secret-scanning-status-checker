package org.eclipse.cbi.secret_scanning;

import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_TOKEN_NAME_DEFAULT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * CLI for {@link org.eclipse.cbi.secret_scanning.SecretScanningStatusChecker}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Jun 9, 2025
 */
//@formatter:off
@Command(
		name = "", 
		description = SecretScanningStatusCheckerCLI.DESCRIPTION, 
		mixinStandardHelpOptions = true, 
		version = SecretScanningStatusCheckerCLI.VERSION, 
		header = SecretScanningStatusCheckerCLI.HEADING, 
		headerHeading = "@|bold,underline Usage|@:%n%n", 
		descriptionHeading = "%n@|bold,underline Description|@:%n%n", 
		parameterListHeading = "%n@|bold,underline Parameters|@:%n", 
		optionListHeading = "%n@|bold,underline Options|@:%n", 
		sortOptions = false, 
		abbreviateSynopsis = true)
//@formatter:on
public class SecretScanningStatusCheckerCLI implements Runnable {
	static final Logger LOG = LoggerFactory.getLogger(SecretScanningStatusCheckerCLI.class);

	static final String HEADING = "'secret_scanning' status checker";
	static final String DESCRIPTION = "Tool to check status of 'secret_scanning' "
			+ "security setting in repositories of a given GitHub organization.";
	static final String VERSION = HEADING + " 1.0";
	static final int EXITCODE_SUCCESS = 0;

	// (required) parameter: GitHub organisation name
	@Parameters(arity = "1", paramLabel = "ORG_NAME", description = "Name of GitHub organisation whose repositories should be checked.")
	private String gitHubOrgName;

	// (optional) option: name of GitHub API token environment variable
	@Option(names = { "-t",
			"--tokenName:env" }, defaultValue = GITHUB_TOKEN_NAME_DEFAULT, description = "Name of environment variable which stores GitHub token. Default: ${DEFAULT-VALUE}")
	private String gitHubTokenName;

	// (optional) option: JSON file name ( defaults to
	// '[ORG_NAME].secret_scanning.json' )
	@Option(names = { "-o",
			"--outputFileName" }, description = "Name of JSON file to which results are output. Default: '[ORG_NAME].secret_scanning.json'")
	private String outputFileName;

	@Option(names = { "--test-run" }, description = "Hidden option used for testing", hidden = true)
	private boolean testRun;

	@Spec
	private CommandSpec commandSpec;

	private String githubToken;

	public void run() {
		if (commandSpec.commandLine().getParseResult().expandedArgs().isEmpty()) {
			commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
			return;
		}

		gitHubTokenName = (gitHubTokenName != null) ? gitHubTokenName : GITHUB_TOKEN_NAME_DEFAULT;

		githubToken = System.getenv(gitHubTokenName);

		if (githubToken == null) {
			throw new SecretScanningStatusCheckerCLIException(
					String.format("%s environment variable is not set!", gitHubTokenName));
		}

		LOG.info("------------------------------------------------------------------------");
		LOG.info(String.format("Checking repositories of '%s' GitHub organisation", gitHubOrgName));
		LOG.info("------------------------------------------------------------------------");

		if (!testRun) {
			SecretScanningStatusChecker.INSTANCE.checkStatus(githubToken, gitHubOrgName, outputFileName);
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new SecretScanningStatusCheckerCLI()).execute(args);

		System.exit(exitCode);
	}
}
