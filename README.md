# 'secret_scanning' status checker

## Tool to check status of 'secret_scanning' security setting in repositories of a given GitHub organization

Checks status of `secret_scanning` security setting in repositories of a given GitHub organization.

If given GitHub organization exists and contains public repositories, results are output to file in JSON format.

### Building
```
$ mvn clean package
```

This will produce an executable JAR.

### Running
```
$ export GITHUB_TOKEN=[GitHub token to use]
$ java -jar ./target/secret-scanning-status-checker-1.0-SNAPSHOT.jar [GitHub organization name]
```
Results will be output to JSON file named `[GitHub organization name].secret_scanning.json`

For full list of options, run without specifying GitHub organization name, i.e.:

```
 $ java -jar ./target/secret-scanning-status-checker-1.0-SNAPSHOT.jar
```

or with help flag:

```
  $ java -jar ./target/secret-scanning-status-checker-1.0-SNAPSHOT.jar -h
```

### Additional information

`secret_scanning` is a property of `security_and_analysis` object returned by "List organization repositories" GitHub REST API endpoint, which has specific authorization requirements; see https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#list-organization-repositories for more details, specifically:
> (...) In order to see the `security_and_analysis` block for a repository you must have admin permissions for the repository or be an owner or security manager for the organization that owns the repository. (...)
