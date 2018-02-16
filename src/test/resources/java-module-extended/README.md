This is the same `module-info.class` from `java-module` directory,
extended with version and main class using the JDK jar tool:

    jar -c -f extended-modular.jar -e com.example.app.Main --module-version 1.0 .
