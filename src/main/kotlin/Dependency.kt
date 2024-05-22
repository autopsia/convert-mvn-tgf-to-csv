data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val scope: String,
    val fileType: String,
    val dependencies: MutableList<Dependency> = mutableListOf(),
)