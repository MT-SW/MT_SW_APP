/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    alias(libs.plugins.meshtastic.kmp.feature)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
}

kotlin {
    // No withHostTest: commonTest holds Compose UI tests, which NPE on the host-test stubs' null Build.FINGERPRINT.
    android {
        namespace = "org.meshtastic.feature.docs"
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.navigation)
            implementation(projects.core.resources)
            implementation(projects.core.repository)
            implementation(projects.core.ui)
            implementation(projects.core.di)

            implementation(libs.jetbrains.compose.material3.adaptive)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation3)
            implementation(libs.coil)
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)
        }

        commonTest.dependencies { implementation(libs.compose.multiplatform.ui.test) }

        jvmTest.dependencies { implementation(compose.desktop.currentOs) }
    }
}

/**
 * Builds this module's whole commonMain compose-resources tree from the canonical `docs/` directory: English pages and
 * the shared screenshots under `files/docs/`, Crowdin translations under `files/{locale}/docs/` (CMP has no qualifiers
 * on `files/`, so the locale is a directory).
 */
val docsComposeResources = layout.buildDirectory.dir("generated/docsComposeResources")

// Refreshes the assistant's knowledge about the firmware, integration and protobufs from GitHub; offline it changes nothing.
val refreshAssistantKnowledge =
    tasks.register<Exec>("refreshAssistantKnowledge") {
        description = "Downloads the newest README files of the other MT_SW repositories for the assistant."
        group = "docs"
        val script = isolated.rootProject.projectDirectory.file("scripts/update-knowledge.sh").asFile
        commandLine("sh", script.absolutePath)
        isIgnoreExitValue = true
        // Windows has no sh: the saved copies are used.
        onlyIf { !System.getProperty("os.name").lowercase().contains("windows") && script.exists() }
        outputs.upToDateWhen { false }
    }

val syncDocsToComposeResources =
    tasks.register<Sync>("syncDocsToComposeResources") {
        description = "Bundles docs/ markdown, screenshots and translations as compose resources for the in-app reader."
        group = "docs"
        dependsOn(refreshAssistantKnowledge)

        // isolated.rootProject: Isolated Projects forbids reaching through rootProject.layout.
        val docsDir = isolated.rootProject.projectDirectory.dir("docs")

        from(docsDir.dir("en")) {
            include("user/**/*.md")
            include("developer/**/*.md")
            into("files/docs")
        }

        // The assistant's knowledge base: the change list of the fork (Polish first, English second) is packaged
        // next to the guide, so the offline assistant can answer "what is new" without internet.
        from(isolated.rootProject.projectDirectory.file("CHANGELOG.md")) { into("files/knowledge") }
        // Short copies of the READMEs of the firmware, integration and protobufs: only the assistant reads them.
        from(isolated.rootProject.projectDirectory.dir("knowledge")) { include("*.md"); into("files/knowledge") }
        // The technical knowledge base (knowledge/base/*.md, plain text edited by hand); the build adds base/index.txt
        // with the file names, because the app cannot list a resource folder.
        from(isolated.rootProject.projectDirectory.dir("knowledge/base")) { include("*.md"); into("files/knowledge/base") }
        // The assistant's own texts in English and Polish: an answer is written in the language of the question even
        // when the app is set to the other one. Only the lines the assistant uses are kept.
        val resourcesDir = isolated.rootProject.projectDirectory.dir("core/resources/src/commonMain/composeResources")
        for ((folder, lang) in listOf("values" to "en", "values-pl" to "pl")) {
            from(resourcesDir.file("$folder/strings.xml")) {
                into("files/assistant")
                rename { "strings-$lang.xml" }
                filter { line ->
                    val keep =
                        Regex(
                            "name=\"(ai_[^\"]*|assistant_[^\"]*|schema_[^\"]*|node_color_[^\"]*|dictation_[^\"]*|bottom_nav_settings|lora|device|display|" +
                                "bluetooth|network|position|power|mqtt|telemetry|mt_sw_features_title|clean_node_database_title|" +
                                "filter_settings|debug_panel|about|help_and_documentation|nodes|map|messages|channels|connections|" +
                                "firmware|discovery_local_mesh|quick_chat|ui_scale|auto_load_chat_images|import_menu_in_top_bar|" +
                                "theme|preferences_language|log_retention_days|translator_title)\"",
                        )
                    if (keep.containsMatchIn(line)) line else ""
                }
            }
        }

        // FR-038: docs/assets/screenshots/ is the tracked screenshot set shared with the Jekyll site, refreshed from
        // the CST references by copyDocsScreenshots, so the site and the in-app reader serve identical images.
        from(docsDir.dir("assets/screenshots")) {
            include("**/*.png")
            into("files/docs/assets/screenshots")
        }

        // Crowdin names these like the values-* dirs (fr, pt-rBR, b+sr+Latn).
        from(docsDir) {
            include("*/user/**/*.md")
            exclude("en/**")
            exclude("_*/**")
            exclude("assets/**")
            exclude("screenshots/**")
            eachFile {
                path = "files/${relativePath.segments[0]}/docs/${relativePath.segments.drop(1).joinToString("/")}"
            }
        }

        includeEmptyDirs = false
        into(docsComposeResources)

        val baseSource = isolated.rootProject.projectDirectory.dir("knowledge/base").asFile
        val baseTarget = docsComposeResources.get().asFile.resolve("files/knowledge/base")
        doLast {
            val names = baseSource.listFiles { f -> f.extension == "md" }?.map { it.name }?.sorted().orEmpty()
            baseTarget.mkdirs()
            baseTarget.resolve("index.txt").writeText(names.joinToString("\n"))
        }
    }

// Mapping the task provider carries the task dependency into every compose-resources task that reads the directory.
compose.resources { customDirectory("commonMain", syncDocsToComposeResources.map { docsComposeResources.get() }) }
