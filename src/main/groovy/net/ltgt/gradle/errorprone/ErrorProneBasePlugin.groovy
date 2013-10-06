package net.ltgt.gradle.errorprone

import org.gradle.api.Plugin
import org.gradle.api.Project

class ErrorProneBasePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.configurations {
            errorprone {
                visible = false
            }
        }
        project.dependencies {
            errorprone 'com.google.errorprone:error_prone_core:1.0.3'
        }
    }
}
