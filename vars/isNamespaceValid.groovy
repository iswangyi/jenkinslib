def call(namespace) {
    if (!namespace?.trim()) {
        return false
    }

    if (namespace.contains("default") ||
        namespace.contains("kube") ||
        namespace.contains("test") ||
        namespace.contains("standard") ||
        namespace.contains("system") ||
        namespace.contains("rightcloud") ||
        namespace.contains("project")) {
        return false
    }

    return true
}
