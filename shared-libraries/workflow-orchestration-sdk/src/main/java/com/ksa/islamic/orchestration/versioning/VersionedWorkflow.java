package com.ksa.islamic.orchestration.versioning;

import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for versioned workflows
 *
 * Provides a framework for implementing workflows that need to
 * support multiple versions running concurrently.
 */
@Slf4j
public abstract class VersionedWorkflow {

    /**
     * Get the current workflow version
     */
    protected int getCurrentVersion() {
        return getMaxSupportedVersion();
    }

    /**
     * Get the maximum supported version
     * Subclasses should override this to indicate their current version
     */
    protected abstract int getMaxSupportedVersion();

    /**
     * Get the minimum supported version
     * Subclasses should override this to indicate the oldest version they support
     */
    protected int getMinSupportedVersion() {
        return Workflow.DEFAULT_VERSION;
    }

    /**
     * Execute versioned logic with automatic version detection
     */
    protected void executeVersioned(String changeId, Runnable... versionLogic) {
        int version = Workflow.getVersion(changeId, getMinSupportedVersion(), getMaxSupportedVersion());

        if (version < 0 || version >= versionLogic.length) {
            throw new IllegalStateException(
                    String.format("No logic defined for version %d of change '%s'", version, changeId)
            );
        }

        log.info("Executing version {} for change '{}'", version, changeId);
        versionLogic[version].run();
    }

    /**
     * Execute versioned logic with explicit version mapping
     */
    protected <T> T executeVersionedWithResult(String changeId, VersionedSupplier<T>... versionSuppliers) {
        int version = Workflow.getVersion(changeId, getMinSupportedVersion(), getMaxSupportedVersion());

        for (VersionedSupplier<T> supplier : versionSuppliers) {
            if (supplier.appliesTo(version)) {
                log.info("Executing version {} logic for change '{}'", version, changeId);
                return supplier.get();
            }
        }

        throw new IllegalStateException(
                String.format("No supplier found for version %d of change '%s'", version, changeId)
        );
    }

    /**
     * Check if workflow should use new behavior
     */
    protected boolean shouldUseNewBehavior(String changeId, int introducedInVersion) {
        int version = Workflow.getVersion(changeId, getMinSupportedVersion(), getMaxSupportedVersion());
        return version >= introducedInVersion;
    }

    /**
     * Interface for version-specific suppliers
     */
    public interface VersionedSupplier<T> {
        boolean appliesTo(int version);
        T get();
    }

    /**
     * Create a versioned supplier for a specific version
     */
    protected static <T> VersionedSupplier<T> forVersion(int targetVersion, java.util.function.Supplier<T> supplier) {
        return new VersionedSupplier<T>() {
            @Override
            public boolean appliesTo(int version) {
                return version == targetVersion;
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }

    /**
     * Create a versioned supplier for a range of versions
     */
    protected static <T> VersionedSupplier<T> forVersionRange(
            int minVersion,
            int maxVersion,
            java.util.function.Supplier<T> supplier) {
        return new VersionedSupplier<T>() {
            @Override
            public boolean appliesTo(int version) {
                return version >= minVersion && version <= maxVersion;
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }

    /**
     * Log version information when workflow starts
     */
    protected void logVersionInfo() {
        log.info("Workflow {} starting - Min version: {}, Max version: {}",
                this.getClass().getSimpleName(),
                getMinSupportedVersion(),
                getMaxSupportedVersion());
    }
}