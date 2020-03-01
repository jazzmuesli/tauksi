package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AnalyseTestcasesPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create("analyse-testcases", AnalyseTestcasesPluginTask.class, (task) -> { 
            task.setProject(project);
        });
    }
}
