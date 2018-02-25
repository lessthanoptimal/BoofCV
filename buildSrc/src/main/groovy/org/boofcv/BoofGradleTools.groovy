package org.boofcv

import org.gradle.api.*
import org.gradle.api.plugins.*;
import org.gradle.api.tasks.testing.*;

class BoofGradleToolsExtension {
    List javadoc_links = []
    String javadoc_bottom_path = "misc/bottom.txt"
    String gversion_file_path
    String gversion_package = ""
}

class BoofGradleTools implements Plugin<Project> {

    void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class)
//        project.getPluginManager().apply(org.gradle.api.tasks.javadoc.JavaDoc.class)

        // Add the 'greeting' extension object
        def extension = project.extensions.create('booftools', BoofGradleToolsExtension)

//        project.tasks.create('alljavadoc',JavaDoc){
////        task alljavadoc(type: Javadoc) {
//            if( extension.length == 0 ) {
//                throw new RuntimeException("javadoc_links has not been specified")
//            }
//            if( extension.size() == 0 ) {
//                throw new RuntimeException("project_name needs to be specified")
//            }
//
//            // only include source code in src directory to avoid including 3rd party code which some projects do as a hack
//            source = javadocProjects.collect { project(it).fileTree('src').include('**/*.java') }
////    source = javadocProjects.collect { project(it).sourceSets.main.allJava }
//            classpath = files(javadocProjects.collect { project(it).sourceSets.main.compileClasspath })
//
//            destinationDir = file("docs/api")
//
//            // Hack for Java 8u121 and beyond. Comment out if running an earlier version of Java
//            options.addBooleanOption("-allow-script-in-comments", true)
//
//            // Add a list of uses of a class to javadoc
//            options.use = true
//
//            configure(options) {
//                failOnError = false
//                docTitle = extension.project_name+" JavaDoc ($project.version)"
//                links = extension.javadoc_links
//            }
//
//            // Work around a Gradle design flaw. It won't copy over files in doc-files
//            doLast {
//                copy {
//                    from javadocProjects.collect { project(it).fileTree('src').include('**/doc-files/*') }
//                    into destinationDir
//                }
//            }
//
//        }
//
//        task alljavadocWeb() {
//            doFirst {
//                alljavadoc.options.bottom = file(extension.javadoc_bottom_path).text
//                alljavadoc.destinationDir = file("docs/api-web")
//            }
//        }
//        alljavadocWeb.finalizedBy(alljavadoc)


        project.ext.checkProjectExistsAddToList = { whichProject , list ->
            try {
                project.project(whichProject)
                list.add(whichProject)
            } catch( UnknownProjectException ignore ) {}
        }

        // Force the release build to fail if it depends on a SNAPSHOT
        project.tasks.create('checkDependsOnSNAPSHOT'){
            doLast {
                if (project.version.endsWith("SNAPSHOT"))
                    return

                project.configurations.compile.each {
                    if (it.toString().contains("SNAPSHOT"))
                        throw new Exception("Release build contains snapshot dependencies: " + it)
                }
            }
        }

        // Creates a resource file containing build information
        project.task('createVersionFile'){
            doLast {
                println("createVersionFile called")
                if(extension.gversion_file_path == null )
                    throw new RuntimeException("Must set gversion_file_path")

                def git_revision = "UNKNOWN"
                def git_sha = "UNKNOWN"

                try {
                    git_revision = 'git rev-list --count HEAD'.execute().text.trim()
                    git_sha = 'git rev-parse HEAD'.execute().text.trim()
                } catch (IOException ignore) {
                }

                def f = new File(extension.gversion_file_path,"GVersion.java")
                f.write("")
                if( extension.gversion_package.size() > 0 ) {
                    f << "package $extension.gversion_package;\n"
                    f << "\n\n"
                }
                f << "/**\n"
                f << " * Automatically generated file containing build version information.\n"
                f << " */\n"
                f << "class GVersion {\n"
                f << "\tpublic static final String MAVEN_GROUP = \"$project.group\";\n"
                f << "\tpublic static final String MAVEN_NAME = \"$project.name\";\n"
                f << "\tpublic static final String VERSION = \"$project.version\";\n"
                f << "\tpublic static final int GIT_REVISION = $git_revision;\n"
                f << "\tpublic static final String GIT_SHA = \"$git_sha\";\n"
                f << "}"
            }
        }

//        project.tasks.create('testReport',TestReport.class) {
//            doLast {
//                destinationDir = project.file("$project.buildDir/reports/allTests")
//                reportOn project.subprojects*test
//            }
//        }
    }
}