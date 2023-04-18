import java.util.Properties

plugins {
    distribution
}

group = "com.epam.drill"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

distributions {
    main.get().contents {
        from(
            tasks.getByPath(":test-plugin-admin:jar"),
            tasks.getByPath(":test-plugin-agent:jar"),
            file("plugin_config.json")
        )
        into("/")
    }
}
