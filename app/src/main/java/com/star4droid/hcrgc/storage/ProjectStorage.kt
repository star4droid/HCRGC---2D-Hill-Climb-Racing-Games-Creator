package com.star4droid.hcrgc.storage

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.star4droid.hcrgc.model.*
import java.io.File

class ProjectStorage(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val projectAdapter = moshi.adapter(ProjectConfig::class.java).indent("  ")
    private val levelAdapter = moshi.adapter(LevelData::class.java).indent("  ")
    private val reusableAdapter = moshi.adapter(ReusableElement::class.java).indent("  ")

    fun getProjectsRoot(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val projectsDir = File(baseDir, "projects")
        if (!projectsDir.exists()) projectsDir.mkdirs()
        return projectsDir
    }

    private fun getProjectDir(projectId: String): File {
        val dir = File(getProjectsRoot(), projectId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getLevelsDir(projectId: String): File {
        val dir = File(getProjectDir(projectId), "levels")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getLibraryDir(projectId: String): File {
        val dir = File(getProjectDir(projectId), "library")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listProjects(): List<ProjectConfig> {
        val root = getProjectsRoot()
        val list = mutableListOf<ProjectConfig>()
        root.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val configFile = File(dir, "project.json")
                if (configFile.exists()) {
                    try {
                        val json = configFile.readText()
                        projectAdapter.fromJson(json)?.let { list.add(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return list.sortedBy { it.name }
    }

    fun saveProject(project: ProjectConfig) {
        val dir = getProjectDir(project.id)
        val file = File(dir, "project.json")
        file.writeText(projectAdapter.toJson(project))
    }

    fun loadProject(projectId: String): ProjectConfig? {
        val file = File(getProjectDir(projectId), "project.json")
        if (!file.exists()) return null
        return try {
            projectAdapter.fromJson(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteProject(projectId: String) {
        val dir = getProjectDir(projectId)
        dir.deleteRecursively()
    }

    fun saveLevel(projectId: String, level: LevelData) {
        val levelsDir = getLevelsDir(projectId)
        val file = File(levelsDir, "${level.id}.json")
        file.writeText(levelAdapter.toJson(level))
    }

    fun loadLevel(projectId: String, levelId: String): LevelData? {
        val file = File(getLevelsDir(projectId), "$levelId.json")
        if (!file.exists()) return null
        return try {
            levelAdapter.fromJson(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listLevels(projectId: String): List<String> {
        val levelsDir = getLevelsDir(projectId)
        return levelsDir.listFiles()?.filter { it.extension == "json" }?.map { it.nameWithoutExtension } ?: emptyList()
    }

    fun deleteLevel(projectId: String, levelId: String) {
        val file = File(getLevelsDir(projectId), "$levelId.json")
        if (file.exists()) file.delete()
    }

    fun saveReusableElement(projectId: String, element: ReusableElement) {
        val libDir = getLibraryDir(projectId)
        val file = File(libDir, "${element.id}.json")
        file.writeText(reusableAdapter.toJson(element))
    }

    fun listReusableElements(projectId: String): List<ReusableElement> {
        val libDir = getLibraryDir(projectId)
        val list = mutableListOf<ReusableElement>()
        libDir.listFiles()?.forEach { file ->
            if (file.extension == "json") {
                try {
                    reusableAdapter.fromJson(file.readText())?.let { list.add(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }
}
