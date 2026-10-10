package com.example.personalsport.ui.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.personalsport.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.data.local.entity.Equipment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreen(
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val factory = remember { EquipmentViewModelFactory(app.equipmentRepository) }
    val viewModel: EquipmentViewModel = viewModel(factory = factory)

    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedWidth = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                "PersonalSport",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Gym Equipment Manager (${state.savedEquipment.size} items)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Custom Equipment")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCustomDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Custom Entry") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .perforatedMeshBackground()
        ) {
            if (isExpandedWidth) {
                // Wide / Tablet 2-pane adaptive view
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("My Gym Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SavedEquipmentList(
                            savedList = state.savedEquipment,
                            onDelete = {
                                viewModel.deleteEquipment(it)
                                snackbarMessage = "Removed ${it.name}"
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Brand Catalog Selector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        BrandCatalogContent(
                            state = state,
                            onSelectBrand = { viewModel.selectBrandFilter(it) },
                            onSearchQuery = { viewModel.updateSearchQuery(it) },
                            onAddCatalogItem = {
                                viewModel.addCatalogItemToGym(it)
                                snackbarMessage = "Added ${it.name} to your gym!"
                            }
                        )
                    }
                }
            } else {
                // Phone layout with Tabs
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("My Gym (${state.savedEquipment.size})") },
                            icon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Brand Catalog") },
                            icon = { Icon(Icons.Rounded.Storefront, contentDescription = null) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (selectedTab == 0) {
                            SavedEquipmentList(
                                savedList = state.savedEquipment,
                                onDelete = {
                                    viewModel.deleteEquipment(it)
                                    snackbarMessage = "Removed ${it.name}"
                                }
                            )
                        } else {
                            BrandCatalogContent(
                                state = state,
                                onSelectBrand = { viewModel.selectBrandFilter(it) },
                                onSearchQuery = { viewModel.updateSearchQuery(it) },
                                onAddCatalogItem = {
                                    viewModel.addCatalogItemToGym(it)
                                    snackbarMessage = "Added ${it.name} to your gym!"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AddCustomEquipmentDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, brand, category, notes ->
                viewModel.addCustomEquipment(name, brand, category, notes)
                snackbarMessage = "Added custom equipment: $name"
                showCustomDialog = false
            }
        )
    }
}

@Composable
fun SavedEquipmentList(
    savedList: List<Equipment>,
    onDelete: (Equipment) -> Unit
) {
    if (savedList.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Text("No Equipment in Your Gym Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Browse the Brand Catalog tab or add custom equipment so the Workout Planner can generate routines tailored to your gear.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(savedList, key = { it.id }) { eq ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(eq.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (!eq.description.isNullOrBlank()) {
                                Text(eq.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(onClick = { onDelete(eq) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Equipment", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrandCatalogContent(
    state: EquipmentUiState,
    onSelectBrand: (String?) -> Unit,
    onSearchQuery: (String) -> Unit,
    onAddCatalogItem: (CatalogItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQuery,
            placeholder = { Text("Search Technogym, Matrix, Decathlon...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQuery("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // Horizontal Brand Filter Chips
        Text("Brands Catalog:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = state.selectedBrandFilter == null,
                    onClick = { onSelectBrand(null) },
                    label = { Text("All Brands") }
                )
            }
            items(BrandCatalog.supportedBrands) { brand ->
                FilterChip(
                    selected = state.selectedBrandFilter == brand,
                    onClick = {
                        if (state.selectedBrandFilter == brand) onSelectBrand(null) else onSelectBrand(brand)
                    },
                    label = { Text(brand) }
                )
            }
        }

        // Catalog List
        if (state.catalogItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No items match your search/filter.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.catalogItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(item.brand, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(item.category, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = { onAddCatalogItem(item) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomEquipmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, brand: String, category: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf("Technogym") }
    var customBrand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Strength") }
    var notes by remember { mutableStateOf("") }
    var isCustomBrandMode by remember { mutableStateOf(false) }

    val categories = listOf("Strength", "Cardio", "Free Weights", "Machine", "Cable", "Calisthenics", "Accessories")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Equipment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Equipment Name *") },
                    placeholder = { Text("e.g. Olympic Barbell, Leg Press") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Brand:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BrandCatalog.supportedBrands) { b ->
                        FilterChip(
                            selected = !isCustomBrandMode && selectedBrand == b,
                            onClick = {
                                isCustomBrandMode = false
                                selectedBrand = b
                            },
                            label = { Text(b) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = isCustomBrandMode,
                            onClick = { isCustomBrandMode = true },
                            label = { Text("Other / Custom") }
                        )
                    }
                }

                if (isCustomBrandMode) {
                    OutlinedTextField(
                        value = customBrand,
                        onValueChange = { customBrand = it },
                        label = { Text("Custom Brand Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Category:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Specifications (Optional)") },
                    placeholder = { Text("e.g. Max weight 200kg, red handles") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalBrand = if (isCustomBrandMode) customBrand else selectedBrand
                        onConfirm(name, finalBrand, category, notes)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add to Gym")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
