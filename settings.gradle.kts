include(":kwava")


rootProject.children.forEach(::setUpChildProject)

/*
 * Instead of every file being named build.gradle.kts we instead use the name ${project.name}.gradle.kts.
 * This is much nicer for searching for the file in your IDE.
 */
fun setUpChildProject(project: ProjectDescriptor) {
    val groovyName = "${project.name}.gradle"
    val kotlinName = "${project.name}.gradle.kts"
    project.buildFileName = groovyName
    if (!project.buildFile.isFile) {
        project.buildFileName = kotlinName
    }
    assert (project.buildFile.isFile) {"File named $groovyName or $kotlinName must exist."}
    project.children.forEach(::setUpChildProject)
}
