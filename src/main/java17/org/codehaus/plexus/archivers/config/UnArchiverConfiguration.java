package org.codehaus.plexus.archivers.config;

import java.util.Objects;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.UnArchiver;

/**
 * Public configuration object for creating/configuring an {@link UnArchiver}.
 *
 * Usage:
 * <pre>{@code
 * UnArchiverConfiguration config = UnArchiverConfiguration.config(c -> {
 *     c.setDestinationDirectory(outputDir);
 *     c.setExistingFileHandling(ExistingFileHandling.OVERWRITE);
 *     c.setPermissionHandling(PermissionHandling.IGNORE);
 * });
 * }</pre>
 *
 * Internal execution remains package-private through applyOn(UnArchiver).
 *
 * @since 5.0.0
 */
public final class UnArchiverConfiguration {

    private final Consumer<UnArchiver> applier;

    private UnArchiverConfiguration(Consumer<UnArchiver> applier) {
        this.applier = Objects.requireNonNull(applier, "applier");
    }

    /**
     * Standard unarchiver configuration path.
     *
     * @param configurer user configuration callback
     * @return immutable configuration object
     */
    public static UnArchiverConfiguration config(UnArchiverConfigurer configurer) {
        Objects.requireNonNull(configurer, "configurer");
        return new UnArchiverConfiguration(unarchiver ->
                configurer.configure(UnArchiverSettings.of(unarchiver)));
    }

    /**
     * Package-private: only execution wiring in this package should call this.
     */
    public void applyOn(UnArchiver unarchiver) {
        applier.accept(Objects.requireNonNull(unarchiver, "unarchiver"));
    }
}
