plugins {
    distribution
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
