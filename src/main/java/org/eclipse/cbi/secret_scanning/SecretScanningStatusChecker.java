package org.eclipse.cbi.secret_scanning;

import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.DEFAULT_CONNECT_TIMEOUT;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.DEFAULT_REQUEST_TIMEOUT;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_ACCEPT;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_BASE_URI;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_FULL_NAME;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECRET_SCANNING;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECURITY_AND_ANALYSIS;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_ORGS_LIST_REPOS_PATH;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_USER_AGENT;
import static org.eclipse.cbi.secret_scanning.SecretScanningStatusCheckerConstants.GITHUB_REST_API_VERSION;

import java.io.BufferedWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Checks status of 'secret_scanning' security setting in repositories of a
 * given GitHub organization.
 * 
 * If given GitHub organization exists and contains public repositories, results
 * are output to file in JSON format.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Jun 9, 2025
 */
public enum SecretScanningStatusChecker {
	INSTANCE;

	private final Logger LOG = LoggerFactory.getLogger(SecretScanningStatusChecker.class);

	private final Gson GSON = new Gson();

	public void checkStatus(String githubToken, String gitHubOrgName, String outputFileName) {
		Objects.requireNonNull(githubToken, "GitHub token is required!");
		Objects.requireNonNull(gitHubOrgName, "GitHub organisation name is required!");

		outputFileName = (outputFileName != null) ? outputFileName : constructOutputFileName(gitHubOrgName);

		Path outputFilePath = Paths.get("", outputFileName);

		if (Files.exists(outputFilePath)) {
			throw new SecretScanningStatusCheckerCLIException(String.format(
					"File %s already exists! Remove it, rename it or specify different file name to output results to.",
					outputFileName));
		}

		JsonArray results = listOrganizationRepositories(githubToken, gitHubOrgName);

		if (results != null) {
			writeResults(results, outputFilePath);
		}
	}

	/**
	 * "List organization repositories"
	 * {@link https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#list-organization-repositories}
	 * 
	 * @param githubToken
	 * @param gitHubOrgName
	 */
	private JsonArray listOrganizationRepositories(String githubToken, String gitHubOrgName) {
		JsonArray results = new JsonArray();

		try {
			HttpClient httpClient = initializeHttpClient(DEFAULT_CONNECT_TIMEOUT);

			URI orgReposRequestURI = constructGitHubListOrganizationRepositoriesUri(gitHubOrgName);

			boolean fetchData = true;

			while (fetchData) {

				//@formatter:off
				HttpRequest orgReposRequest = HttpRequest.newBuilder().GET().uri(orgReposRequestURI)
						.timeout(Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT))
						.header("Authorization", "Bearer " + githubToken)
						.header("User-Agent", GITHUB_REST_API_USER_AGENT)
						.header("X-GitHub-Api-Version", GITHUB_REST_API_VERSION)
						.header("Accept", GITHUB_REST_API_ACCEPT).build();
				//@formatter:on

				HttpResponse<String> orgReposResponse = httpClient.send(orgReposRequest, BodyHandlers.ofString());

				HttpHeaders orgReposResponseHeaders = orgReposResponse.headers();

				if (orgReposResponse.statusCode() == 200) {

					JsonElement orgReposResponseBodyJsonElement = JsonParser.parseString(orgReposResponse.body());

					if (orgReposResponseBodyJsonElement.isJsonArray()) {

						JsonArray orgReposResponseBodyJsonArray = orgReposResponseBodyJsonElement.getAsJsonArray();

						for (JsonElement orgReposResponseJsonElement : orgReposResponseBodyJsonArray.asList()) {
							if (orgReposResponseJsonElement.isJsonObject()) {

								JsonObject result = new JsonObject();

								JsonObject orgRepoJsonObject = orgReposResponseJsonElement.getAsJsonObject();

								if (orgRepoJsonObject.has(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_FULL_NAME)) {
									result.add(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_FULL_NAME,
											orgRepoJsonObject.get(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_FULL_NAME));
								}

								if (orgRepoJsonObject
										.has(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECURITY_AND_ANALYSIS)) {
									JsonObject orgRepoSecurityAndAnalysisJsonObject = orgRepoJsonObject.getAsJsonObject(
											GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECURITY_AND_ANALYSIS);

									if (orgRepoSecurityAndAnalysisJsonObject
											.has(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECRET_SCANNING)) {
										result.add(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECRET_SCANNING,
												orgRepoSecurityAndAnalysisJsonObject
														.get(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECRET_SCANNING));
									}
								}

								results.add(result);
							}
						}
					}

					Optional<String> nextPageOptional = getNextPage(orgReposResponseHeaders.firstValue("link"));

					fetchData = nextPageOptional.isPresent();

					if (fetchData) {
						orgReposRequestURI = getNextPageURI(nextPageOptional.get());
					}

				} else if (orgReposResponse.statusCode() == 404) {

					throw new SecretScanningStatusCheckerCLIException(
							String.format("GitHub organization '%s' could not be found!", gitHubOrgName));

				} else {

					Optional<String> rateLimitRemainingHeaderOptional = orgReposResponseHeaders
							.firstValue("x-ratelimit-remaining");
					if (rateLimitRemainingHeaderOptional.isPresent()) {
						if (Integer.parseInt(rateLimitRemainingHeaderOptional.get()) == 0) {
							throw new SecretScanningStatusCheckerCLIException("Rate limit reached!");
						}
					}

					throw new SecretScanningStatusCheckerCLIException(String
							.format("Response returned non-successful error code: %d", orgReposResponse.statusCode()));
				}
			}

			int orgReposCount = results.size();

			if (orgReposCount > 0) {
				if (orgReposCount == 1) {
					LOG.info(String.format("Found '%d' repository", orgReposCount));
				} else {
					LOG.info(String.format("Found '%d' repositories", orgReposCount));
				}
			} else {
				LOG.info("No repositories found");
			}

		} catch (Throwable t) {
			throw new SecretScanningStatusCheckerCLIException(
					"Exception was thrown while listing organization repositories!", t);
		}

		return results;
	}

	private void writeResults(JsonArray results, Path outputFilePath) {
		LOG.info(String.format("Writing results to '%s'", outputFilePath.toFile().getAbsolutePath()));

		try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {

			GSON.toJson(results, writer);

		} catch (Throwable t) {
			throw new SecretScanningStatusCheckerCLIException("Exception was thrown while writing results!", t);
		}
	}

	private Optional<String> getNextPage(Optional<String> linkHeaderOptional) {
		if (linkHeaderOptional.isPresent()) {
			//@formatter:off
			return Arrays.stream(linkHeaderOptional.get().split(", "))
					.filter(t -> t.endsWith("rel=\"next\""))
					.findFirst();
			//@formatter:on
		}

		return Optional.empty();
	}

	private URI getNextPageURI(String nextPageRawUrl) {
		String nextPageUrl = nextPageRawUrl.split(";")[0];

		nextPageUrl = nextPageUrl.substring(1, nextPageUrl.indexOf(">"));

		return URI.create(nextPageUrl);
	}

	private URI constructGitHubListOrganizationRepositoriesUri(String gitHubOrgName) {
		StringBuilder sb = new StringBuilder();
		sb.append(GITHUB_REST_API_BASE_URI);
		sb.append(String.format(GITHUB_REST_API_ORGS_LIST_REPOS_PATH, gitHubOrgName));
		return URI.create(sb.toString());
	}

	private HttpClient initializeHttpClient(int connectTimeout) {
		//@formatter:off
		return HttpClient.newBuilder()
				.version(Version.HTTP_2)
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(connectTimeout))
				.build();
		//@formatter:on
	}

	private String constructOutputFileName(String gitHubOrgName) {
		StringBuilder sb = new StringBuilder();
		sb.append(gitHubOrgName);
		sb.append(".");
		sb.append(GITHUB_REST_API_ORGS_LIST_REPOS_MEMBER_SECRET_SCANNING);
		sb.append(".");
		sb.append("json");
		return sb.toString();
	}
}
