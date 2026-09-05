package com.star4droid.hcrgc.assets

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class ProjectAsset(
    val id: String,
    val name: String,
    val category: String, // "Images", "Tiles", "Sounds", "Fonts"
    val localFilePath: String? = null,
    val isBuiltIn: Boolean = true
)

class AssetRepository(private val context: Context) {

    fun getProjectAssetsDir(projectName: String): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val assetsDir = File(baseDir, "projects/$projectName/assets")
        if (!assetsDir.exists()) assetsDir.mkdirs()
        return assetsDir
    }

    fun getAssetsForProject(projectName: String): List<ProjectAsset> {
        val list = mutableListOf<ProjectAsset>()

        // Add built-ins
        list.add(ProjectAsset(VectorSprites.ASSET_CAR_BUGGY, "Buggy Chassis", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_CAR_TRUCK, "Truck Chassis", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_WHEEL_RUGGED, "Rugged Tire", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_WHEEL_RACING, "Racing Tire", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_COIN_GOLD, "Gold Coin", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_FINISH_FLAG, "Finish Flag", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_LANTERN, "Lantern Light", "Images"))
        list.add(ProjectAsset(VectorSprites.ASSET_CRATE, "Wood Crate", "Images"))

        // Tiles
        list.add(ProjectAsset(VectorSprites.ASSET_TILE_GRASS, "Grass Surface", "Tiles"))
        list.add(ProjectAsset(VectorSprites.ASSET_TILE_DIRT, "Underground Dirt", "Tiles"))
        list.add(ProjectAsset(VectorSprites.ASSET_TILE_ROCK, "Solid Rock", "Tiles"))

        // Imported files from project directory
        val dir = getProjectAssetsDir(projectName)
        dir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val cat = when (file.extension.lowercase()) {
                    "png", "jpg", "jpeg", "webp" -> "Images"
                    "wav", "mp3", "ogg" -> "Sounds"
                    "ttf", "otf" -> "Fonts"
                    else -> "Files"
                }
                list.add(
                    ProjectAsset(
                        id = file.nameWithoutExtension,
                        name = file.name,
                        category = cat,
                        localFilePath = file.absolutePath,
                        isBuiltIn = false
                    )
                )
            }
        }
        return list
    }

    fun importAssetFromUri(projectName: String, uri: Uri, fileName: String): ProjectAsset? {
        return try {
            val dir = getProjectAssetsDir(projectName)
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            ProjectAsset(
                id = destFile.nameWithoutExtension,
                name = destFile.name,
                category = "Images",
                localFilePath = destFile.absolutePath,
                isBuiltIn = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
