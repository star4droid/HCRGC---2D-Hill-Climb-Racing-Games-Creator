package com.star4droid.hcrgc.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.star4droid.hcrgc.assets.AssetRepository
import com.star4droid.hcrgc.assets.ProjectAsset
import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManagerSheet(
    projectName: String,
    onDismiss: () -> Unit,
    onSelectAsset: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AssetRepository(context) }
    var assets by remember { mutableStateOf(repository.getAssetsForProject(projectName)) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Images", "Tiles", "Sounds", "Fonts")

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = "custom_${System.currentTimeMillis()}.png"
            repository.importAssetFromUri(projectName, uri, fileName)?.let {
                assets = repository.getAssetsForProject(projectName)
            }
        }
    }

    val filteredAssets = remember(assets, selectedCategory) {
        if (selectedCategory == "All") assets else assets.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .testTag("sheet_asset_manager")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = StudioAccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Project Assets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                Button(
                    onClick = { filePicker.launch("image/*") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import")
                }
            }

            // Categories
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = StudioSurface,
                divider = {}
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = (selectedCategory == cat),
                        onClick = { selectedCategory = cat },
                        text = {
                            Text(
                                text = cat,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == cat) StudioAccentBlue else StudioTextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Assets Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAssets) { asset ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectAsset(asset.id)
                                onDismiss()
                            }
                            .testTag("asset_item_${asset.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioSurfaceBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(StudioSurface, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(46.dp)) {
                                    VectorSprites.drawAsset(this, asset.id, size.width, size.height)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = StudioTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
