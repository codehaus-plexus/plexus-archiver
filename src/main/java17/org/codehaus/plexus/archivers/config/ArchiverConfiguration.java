/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.plexus.archivers.config;

import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.Archiver;

/**
 * Public staged API:
 *
 * <pre>{@code
 * ArchiverConfiguration.config(c -> ...)
 * ArchiverConfiguration.config(c -> ...).withFinalizer(...)
 * ArchiverConfiguration.reproducible(r -> ...).config(c -> ...)
 * ArchiverConfiguration.reproducible(r -> ...).config(c -> ...).withFinalizer(...)
 * }</pre>
 *
 * <p>The common case is {@link #config(ArchiverConfigurer)} only.</p>
 *
 * @since 5.0.0
 */
public interface ArchiverConfiguration {

    /**
     * Standard path (no reproducible stage).
     */
    static ConfiguredConfiguration config(ArchiverConfigurer configurer) {
        Objects.requireNonNull(configurer, "configurer");
        return new ConfiguredConfigurationImpl(archiver ->
                configurer.configure(ArchiverSettings.of(archiver)));
    }

    /**
     * Reproducible staged path (must be followed by .config(...)).
     */
    static ReproducibleBuildConfiguration reproducible(ReproducibleBuildConfigurer configurer) {
        return new ReproducibleBuildConfiguration(Objects.requireNonNull(configurer, "configurer"));
    }

    /**
     * Package-private style execution hook used by wiring in this package.
     */
    void applyOn(Archiver archiver);

    /**
     * Stage object that enforces grammar:
     * reproducible(...).config(...)
     */
    final class ReproducibleBuildConfiguration {
        private final ReproducibleBuildConfigurer reproducibleConfigurer;

        private ReproducibleBuildConfiguration(ReproducibleBuildConfigurer reproducibleConfigurer) {
            this.reproducibleConfigurer = reproducibleConfigurer;
        }

        /**
         * Finalizes staged configuration. Order is guaranteed:
         * 1) reproducible settings
         * 2) regular settings
         */
        public ConfiguredConfiguration config(ArchiverConfigurer configurer) {
            Objects.requireNonNull(configurer, "configurer");
            return new ConfiguredConfigurationImpl(archiver -> {
                reproducibleConfigurer.configure(ReproducibleBuildSettings.of(archiver));
                configurer.configure(ArchiverSettings.of(archiver));
            });
        }
    }

    /**
     * Returned by {@link ArchiverConfiguration#config(ArchiverConfigurer)} and
     * {@link ReproducibleBuildConfiguration#config(ArchiverConfigurer)}.
     *
     * <p>It is already a valid {@link ArchiverConfiguration} for the common case.
     * {@link #withFinalizer(ArchiverFinalizationPolicy)} is an optional terminal step.</p>
     *
     * @since 5.0.0
     */
    interface ConfiguredConfiguration extends ArchiverConfiguration {

        /**
         * Optional terminal finalization policy.
         *
         * <p>Returns plain {@link ArchiverConfiguration}, so this method cannot be chained twice.</p>
         */
        ArchiverConfiguration withFinalizer(ArchiverFinalizationPolicy... finalizationPolicy);
    }

    final class ConfiguredConfigurationImpl implements ConfiguredConfiguration {
        private final ArchiverApplier applier;

        private ConfiguredConfigurationImpl(ArchiverApplier applier) {
            this.applier = Objects.requireNonNull(applier, "applier");
        }

        @Override
        public ArchiverConfiguration withFinalizer(ArchiverFinalizationPolicy... finalizationPolicy) {
            Objects.requireNonNull(finalizationPolicy, "finalizationPolicy");
            return new FinalizedConfigurationImpl(applier, List.of(finalizationPolicy));
        }

        @Override
        public void applyOn(Archiver archiver) {
            applier.apply(Objects.requireNonNull(archiver, "archiver"));
        }
    }

    final class FinalizedConfigurationImpl implements ArchiverConfiguration {
        private final ArchiverApplier applier;
        private final List<ArchiverFinalizationPolicy> finalizationPolicies;

        private FinalizedConfigurationImpl(ArchiverApplier applier, List<ArchiverFinalizationPolicy> finalizationPolicies) {
            this.applier = Objects.requireNonNull(applier, "applier");
            this.finalizationPolicies = Objects.requireNonNull(finalizationPolicies, "finalizationPolicy");
        }

        @Override
        public void applyOn(Archiver archiver) {
            Objects.requireNonNull(archiver, "archiver");
            applier.apply(archiver);
            for(ArchiverFinalizationPolicy policy : finalizationPolicies) {
                applyFinalizationPolicy(archiver, policy);
            }
        }

        private static void applyFinalizationPolicy(Archiver archiver, ArchiverFinalizationPolicy policy) {
        	if (policy instanceof DotFileDirectoryFinalizationPolicy dot) {
                archiver.setDotFileDirectory(dot.directory().toFile());
                return;
            }
            throw new IncompatibleClassChangeError(
                    "Unsupported ArchiverFinalizationPolicy implementation: " + policy.getClass().getName());
        }
    }

    @FunctionalInterface
    interface ArchiverApplier {
        void apply(Archiver archiver);
    }
}