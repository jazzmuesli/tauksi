package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CombineMetricsPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create("combine-metrics", CombineMetricsPluginTask.class, (task) -> { 
            task.setProject(project);
        });
    }
}
