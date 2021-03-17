package io.snyk.snyk_maven_plugin;

import io.snyk.snyk_maven_plugin.command.Command;
import io.snyk.snyk_maven_plugin.command.CommandLine;
import io.snyk.snyk_maven_plugin.command.CommandRunner;
import io.snyk.snyk_maven_plugin.download.CLIVersions;
import io.snyk.snyk_maven_plugin.download.ExecutableDownloader;
import io.snyk.snyk_maven_plugin.download.Installer;
import io.snyk.snyk_maven_plugin.download.Platform;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.logging.MessageUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public abstract class AbstractSnykMojo extends AbstractMojo {

    @Parameter
    protected String apiToken;

    @Parameter
    protected CLI cli;

    @Parameter
    protected List<String> args;

    @Parameter(property = "snyk.skip")
    protected boolean skip;

    public void execute() throws MojoFailureException, MojoExecutionException {
        if (skip) {
            getLog().info("snyk " + getCommand().commandName() + " skipped");
            return;
        }

        int exitCode = executeCommand();
        if (exitCode != 0) {
            throw new MojoFailureException("snyk command exited with non-zero exit code (" + exitCode + "). See output for details.");
        }
    }

    public int executeCommand() throws MojoExecutionException {
        try {
            ProcessBuilder commandLine = CommandLine.asProcessBuilder(
                getExecutable().getAbsolutePath(),
                getCommand(),
                Optional.ofNullable(apiToken),
                Optional.ofNullable(args).orElse(emptyList()),
                MessageUtils.isColorEnabled()
            );
            Log log = getLog();
            return CommandRunner.run(commandLine::start, log::info, log::error);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private File getExecutable() {
        return Optional.ofNullable(cli)
            .map(CLI::getExecutable)
            .orElseGet(this::downloadExecutable);
    }

    private File downloadExecutable() {
        Platform platform = Platform.current();
        String version = Optional.ofNullable(cli)
            .map(CLI::getVersion)
            .orElse(CLIVersions.LATEST_VERSION_KEYWORD);
        Path destination = Installer.getInstallLocation(
            platform,
            Optional.ofNullable(System.getProperty("user.home")).map(Paths::get),
            System.getenv()
        );
        return ExecutableDownloader.download(destination, platform, version);
    }

    public abstract Command getCommand();

    public static class CLI {

        @Parameter
        private File executable;

        @Parameter
        private String version;

        public File getExecutable() {
            return executable;
        }

        public String getVersion() {
            return version;
        }

    }

}
