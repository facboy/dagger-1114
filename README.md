# dagger-1114

https://github.com/google/dagger/issues/1114

Compilation under vanilla Maven works fine.

To reproduce bug in Eclipse Oxygen:

1. Run a `mvn clean install -pl proc` first
1. Import into eclipse

*NOTE:* this project won't work in IDEA either.  IDEA is happy if the annotation processor is defined on the 'normal' classpath (ie from a jar). In general IDEs don't like to have the annotation processor in the same reactor project as other modules that depend on it.  It's just convenient to lay it out this way for an example project.
