package org.codehaus.plexus.archivers.config;

import java.util.Objects;
import java.util.function.Consumer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archivers.config.ArchiverConfigurer;
import org.codehaus.plexus.archivers.config.ArchiverSettings;
import org.codehaus.plexus.archivers.config.ReproducibleBuildConfigurer;
import org.codehaus.plexus.archivers.config.ReproducibleBuildSettings;

/**
 * Public staged API:
 *
 *   ArchiverConfiguration.config(c -> ...)
 *   ArchiverConfiguration.reproducible(r -> ...).config(c -> ...)
 *
 * Internal execution remains package-private through applyOn(Archiver).
 */
public final class ArchiverConfiguration {

    private final Consumer<Archiver> applier;

    private ArchiverConfiguration(Consumer<Archiver> applier) {
        this.applier = Objects.requireNonNull(applier, "applier");
    }

    /**
     * Standard path (no reproducible stage).
     */
    public static ArchiverConfiguration config(ArchiverConfigurer configurer) {
        Objects.requireNonNull(configurer, "configurer");
        return new ArchiverConfiguration(archiver ->
                configurer.configure(ArchiverSettings.of(archiver)));
    }

    /**
     * Reproducible staged path (must be followed by .config(...)).
     */
    public static ReproducibleBuildConfiguration reproducible(ReproducibleBuildConfigurer configurer) {
        return new ReproducibleBuildConfiguration(
                Objects.requireNonNull(configurer, "configurer"));
    }

    /**
     * Package-private: only execution wiring in this package should call this.
     */
    public void applyOn(Archiver archiver) {
        applier.accept(Objects.requireNonNull(archiver, "archiver"));
    }

    /**
     * Stage object that enforces grammar:
     * reproducible(...).config(...)
     */
    public static final class ReproducibleBuildConfiguration {
        private final ReproducibleBuildConfigurer reproducibleConfigurer;

        private ReproducibleBuildConfiguration(ReproducibleBuildConfigurer reproducibleConfigurer) {
            this.reproducibleConfigurer = reproducibleConfigurer;
        }

        /**
         * Finalizes staged configuration.
         * Order is guaranteed on the same Archiver instance:
         * 1) reproducible settings
         * 2) regular settings
         */
        public ArchiverConfiguration config(ArchiverConfigurer configurer) {
            Objects.requireNonNull(configurer, "configurer");
            return new ArchiverConfiguration(archiver -> {
                reproducibleConfigurer.configure(ReproducibleBuildSettings.of(archiver));
                configurer.configure(ArchiverSettings.of(archiver));
            });
        }
    }
}