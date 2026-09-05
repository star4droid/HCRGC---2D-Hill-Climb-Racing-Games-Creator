package com.star4droid.hcrgc.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star4droid.hcrgc.assets.VectorSprites
import com.star4droid.hcrgc.model.*
import com.star4droid.hcrgc.ui.theme.*

private data class AddItemDescriptor(
    val title: String,
    val type: ObjectType,
    val icon: ImageVector,
    val category: String,
    val defaultObj: GameObject
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectDialog(
    onDismiss: () -> Unit,
    onAddObject: (GameObject) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Shapes", "Vehicles", "Terrain", "Gameplay", "UI", "Lighting", "Element")

    val items = remember {
        listOf(
            AddItemDescriptor(
                title = "Box",
                type = ObjectType.BOX,
                icon = Icons.Default.Square,
                category = "Shapes",
                defaultObj = GameObject(
                    name = "Box",
                    type = ObjectType.BOX,
                    width = 80f,
                    height = 80f,
                    tintColor = 0xFF38BDF8,
                    physics = PhysicsProperties(bodyType = BodyType.DYNAMIC)
                )
            ),
            AddItemDescriptor(
                title = "Circle",
                type = ObjectType.CIRCLE,
                icon = Icons.Default.Circle,
                category = "Shapes",
                defaultObj = GameObject(
                    name = "Circle",
                    type = ObjectType.CIRCLE,
                    width = 70f,
                    height = 70f,
                    tintColor = 0xFFF59E0B,
                    physics = PhysicsProperties(bodyType = BodyType.DYNAMIC)
                )
            ),
            AddItemDescriptor(
                title = "Custom Shape",
                type = ObjectType.CUSTOM_SHAPE,
                icon = Icons.Default.Polyline,
                category = "Shapes",
                defaultObj = GameObject(
                    name = "Road / Polygon",
                    type = ObjectType.CUSTOM_SHAPE,
                    width = 300f,
                    height = 150f,
                    tintColor = 0xFF16A34A,
                    physics = PhysicsProperties(bodyType = BodyType.STATIC),
                    customShape = CustomShapeConfig()
                )
            ),
            AddItemDescriptor(
                title = "Car Body",
                type = ObjectType.CAR_BODY,
                icon = Icons.Default.DirectionsCar,
                category = "Vehicles",
                defaultObj = GameObject(
                    name = "Car Body",
                    type = ObjectType.CAR_BODY,
                    width = 130f,
                    height = 55f,
                    imageAsset = VectorSprites.ASSET_CAR_BUGGY,
                    physics = PhysicsProperties(bodyType = BodyType.DYNAMIC, density = 1.3f),
                    carBody = CarBodyConfig()
                )
            ),
            AddItemDescriptor(
                title = "Wheel",
                type = ObjectType.WHEEL,
                icon = Icons.Default.TireRepair,
                category = "Vehicles",
                defaultObj = GameObject(
                    name = "Wheel",
                    type = ObjectType.WHEEL,
                    width = 46f,
                    height = 46f,
                    imageAsset = VectorSprites.ASSET_WHEEL_RUGGED,
                    physics = PhysicsProperties(bodyType = BodyType.DYNAMIC, friction = 1.0f),
                    wheel = WheelConfig(radius = 23f)
                )
            ),
            AddItemDescriptor(
                title = "TileMap",
                type = ObjectType.TILEMAP,
                icon = Icons.Default.GridOn,
                category = "Terrain",
                defaultObj = GameObject(
                    name = "TileMap",
                    type = ObjectType.TILEMAP,
                    width = 240f,
                    height = 96f,
                    physics = PhysicsProperties(bodyType = BodyType.STATIC),
                    tileMap = TileMapConfig(
                        cols = 5,
                        rows = 2,
                        tiles = mapOf("0_0" to "tile_grass", "1_0" to "tile_grass", "2_0" to "tile_grass")
                    )
                )
            ),
            AddItemDescriptor(
                title = "Gold Coin",
                type = ObjectType.COIN,
                icon = Icons.Default.MonetizationOn,
                category = "Gameplay",
                defaultObj = GameObject(
                    name = "Coin",
                    type = ObjectType.COIN,
                    width = 34f,
                    height = 34f,
                    imageAsset = VectorSprites.ASSET_COIN_GOLD,
                    physics = PhysicsProperties(bodyType = BodyType.STATIC, isSensor = true),
                    coin = CoinConfig()
                )
            ),
            AddItemDescriptor(
                title = "Finish Flag",
                type = ObjectType.FINISH_FLAG,
                icon = Icons.Default.Flag,
                category = "Gameplay",
                defaultObj = GameObject(
                    name = "Finish Flag",
                    type = ObjectType.FINISH_FLAG,
                    width = 70f,
                    height = 110f,
                    imageAsset = VectorSprites.ASSET_FINISH_FLAG,
                    physics = PhysicsProperties(bodyType = BodyType.STATIC, isSensor = true),
                    finishFlag = FinishFlagConfig()
                )
            ),
            AddItemDescriptor(
                title = "Light",
                type = ObjectType.LIGHT,
                icon = Icons.Default.Lightbulb,
                category = "Lighting",
                defaultObj = GameObject(
                    name = "Light",
                    type = ObjectType.LIGHT,
                    width = 40f,
                    height = 40f,
                    imageAsset = VectorSprites.ASSET_LANTERN,
                    physics = PhysicsProperties(bodyType = BodyType.STATIC, isSensor = true),
                    light = LightConfig()
                )
            ),
            AddItemDescriptor(
                title = "UI Button",
                type = ObjectType.UI_BUTTON,
                icon = Icons.Default.SmartButton,
                category = "UI",
                defaultObj = GameObject(
                    name = "Gas Button",
                    type = ObjectType.UI_BUTTON,
                    width = 110f,
                    height = 70f,
                    zIndex = 100,
                    uiButton = UIButtonConfig(label = "GAS", role = ButtonRole.ACCELERATE)
                )
            ),
            AddItemDescriptor(
                title = "UI Text",
                type = ObjectType.UI_TEXT,
                icon = Icons.Default.TextFields,
                category = "UI",
                defaultObj = GameObject(
                    name = "Text Label",
                    type = ObjectType.UI_TEXT,
                    width = 120f,
                    height = 40f,
                    zIndex = 100,
                    uiText = UITextConfig(text = "Hello")
                )
            ),
            AddItemDescriptor(
                title = "UI Progress Bar",
                type = ObjectType.UI_PROGRESS_BAR,
                icon = Icons.Default.LinearScale,
                category = "UI",
                defaultObj = GameObject(
                    name = "Progress Bar",
                    type = ObjectType.UI_PROGRESS_BAR,
                    width = 300f,
                    height = 18f,
                    zIndex = 100,
                    uiProgressBar = UIProgressBarConfig()
                )
            ),
            AddItemDescriptor(
                title = "ELEMENT (Non-Physical)",
                type = ObjectType.ELEMENT,
                icon = Icons.Default.Widgets,
                category = "Element",
                defaultObj = GameObject(
                    name = "Decorative Element",
                    type = ObjectType.ELEMENT,
                    width = 60f,
                    height = 40f,
                    physics = PhysicsProperties(bodyType = BodyType.NONE)
                )
            )
        )
    }

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == "All") items else items.filter { it.category == selectedCategory }
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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add Game Object",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Category Chips
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

            // Items Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 105.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onAddObject(item.defaultObj.copy(id = java.util.UUID.randomUUID().toString().take(8)))
                                onDismiss()
                            }
                            .testTag("add_item_${item.title.lowercase().replace(' ', '_')}"),
                        shape = RoundedCornerShape(14.dp),
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
                                    .size(42.dp)
                                    .background(StudioAccentBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = StudioAccentBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
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
