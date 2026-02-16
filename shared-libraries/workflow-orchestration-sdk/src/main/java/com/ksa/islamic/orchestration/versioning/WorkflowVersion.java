package com.ksa.islamic.orchestration.versioning;

import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilities for workflow versioning
 *
 * Provides helper methods for managing workflow versions and
 * ensuring backward compatibility during workflow evolution.
 */
@Slf4j
public class WorkflowVersion {

    // Version constants for common changes
    public static final int INITIAL_VERSION = Workflow.DEFAULT_VERSION;
    public static final int VERSION_1 = 1;
    public static final int VERSION_2 = 2;
    public static final int VERSION_3 = 3;

    /**
     * Gets the version for a specific change
     *
     * @param changeId Unique identifier for the change
     * @param minSupported Minimum version still supported
     * @param maxSupported Current maximum version
     * @return The version to use for this workflow execution
     */
    public static int getVersion(String changeId, int minSupported, int maxSupported) {
        int version = Workflow.getVersion(changeId, minSupported, maxSupported);
        log.debug("Workflow version for change '{}': {}", changeId, version);
        return version;
    }

    /**
     * Checks if a workflow is running at a specific version or higher
     *
     * @param changeId Unique identifier for the change
     * @param targetVersion The version to check against
     * @param maxVersion Current maximum version
     * @return true if the workflow is at targetVersion or higher
     */
    public static boolean isAtLeastVersion(String changeId, int targetVersion, int maxVersion) {
        int version = getVersion(changeId, INITIAL_VERSION, maxVersion);
        return version >= targetVersion;
    }

    /**
     * Checks if a workflow is running at exactly a specific version
     *
     * @param changeId Unique identifier for the change
     * @param targetVersion The version to check
     * @param maxVersion Current maximum version
     * @return true if the workflow is at exactly targetVersion
     */
    public static boolean isExactlyVersion(String changeId, int targetVersion, int maxVersion) {
        int version = getVersion(changeId, INITIAL_VERSION, maxVersion);
        return version == targetVersion;
    }

    /**
     * Checks if a workflow is running at an old version (before a specific version)
     *
     * @param changeId Unique identifier for the change
     * @param cutoffVersion The version cutoff
     * @param maxVersion Current maximum version
     * @return true if the workflow is before cutoffVersion
     */
    public static boolean isOlderThan(String changeId, int cutoffVersion, int maxVersion) {
        int version = getVersion(changeId, INITIAL_VERSION, maxVersion);
        return version < cutoffVersion;
    }

    /**
     * Helper to execute different logic based on version
     *
     * @param changeId Unique identifier for the change
     * @param maxVersion Current maximum version
     * @param versionExecutors Executors for each version
     */
    public static void executeVersioned(String changeId, int maxVersion, VersionExecutor... versionExecutors) {
        int version = getVersion(changeId, INITIAL_VERSION, maxVersion);

        for (VersionExecutor executor : versionExecutors) {
            if (executor.appliesTo(version)) {
                log.debug("Executing version {} logic for change '{}'", version, changeId);
                executor.execute();
                return;
            }
        }

        log.warn("No executor found for version {} of change '{}'", version, changeId);
    }

    /**
     * Interface for version-specific execution logic
     */
    public interface VersionExecutor {
        boolean appliesTo(int version);
        void execute();
    }

    /**
     * Creates a version executor for a specific version
     */
    public static VersionExecutor forVersion(int targetVersion, Runnable logic) {
        return new VersionExecutor() {
            @Override
            public boolean appliesTo(int version) {
                return version == targetVersion;
            }

            @Override
            public void execute() {
                logic.run();
            }
        };
    }

    /**
     * Creates a version executor for a range of versions
     */
    public static VersionExecutor forVersionRange(int minVersion, int maxVersion, Runnable logic) {
        return new VersionExecutor() {
            @Override
            public boolean appliesTo(int version) {
                return version >= minVersion && version <= maxVersion;
            }

            @Override
            public void execute() {
                logic.run();
            }
        };
    }

    /**
     * Creates a version executor for versions at or above a threshold
     */
    public static VersionExecutor forVersionAndAbove(int minVersion, Runnable logic) {
        return new VersionExecutor() {
            @Override
            public boolean appliesTo(int version) {
                return version >= minVersion;
            }

            @Override
            public void execute() {
                logic.run();
            }
        };
    }
}