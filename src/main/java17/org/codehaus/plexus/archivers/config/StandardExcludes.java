package org.codehaus.plexus.archivers.config;

public sealed interface StandardExcludes {

	// org.codehaus.plexus.util.AbstractScanner.DEFAULTEXCLUDES
	ExcludeGroup PLEXUS = new PredefinedExcludes("plexus-utils");

	ExcludeMode NONE = new NamedExcludeMode("none");

	sealed interface ExcludeMode extends StandardExcludes permits NamedExcludeMode {}
	
	sealed interface ExcludeGroup extends StandardExcludes permits PredefinedExcludes {}
}
record PredefinedExcludes(String name) implements StandardExcludes.ExcludeGroup {}

record NamedExcludeMode(String name) implements StandardExcludes.ExcludeMode {}