package com.example.personalsport.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.health.HealthSyncManager
import com.example.personalsport.ui.achievements.AchievementsScreen
import com.example.personalsport.ui.dashboard.DashboardScreen
import com.example.personalsport.ui.devices.DevicesManagerScreen
import com.example.personalsport.ui.diet.DietScreen
import com.example.personalsport.ui.equipment.EquipmentScreen
import com.example.personalsport.ui.manualentry.ManualEntryScreen
import com.example.personalsport.ui.planner.PlannerScreen
import com.example.personalsport.ui.onboarding.OnboardingScreen
import com.example.personalsport.ui.onboarding.OnboardingViewModel
import com.example.personalsport.ui.profile.UserProfileAvatar
import com.example.personalsport.ui.profile.UserProfileDialog
import com.example.personalsport.ui.profile.UserProfileViewModel
import com.example.personalsport.ui.progress.ProgressPhotoScreen
import com.example.personalsport.ui.supplement.SupplementHubScreen
import com.example.personalsport.ui.trainer.PersonalTrainerScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigationSuite(
    healthSyncManager: HealthSyncManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val profileViewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModel.Factory(app.userProfileRepository)
    )

    val profileUiState by profileViewModel.uiState.collectAsState()
    val currentProfile = profileUiState.profile

    if (!currentProfile.isOnboardingCompleted) {
        val onboardingViewModel: OnboardingViewModel = viewModel(
            factory = OnboardingViewModel.Factory(
                userProfileRepository = app.userProfileRepository,
                bodyMetricRepository = app.bodyMetricRepository
            )
        )
        OnboardingScreen(
            viewModel = onboardingViewModel,
            onOnboardingFinished = {
                // Force state refresh
            },
            modifier = modifier
        )
        return
    }

    var selectedTab by rememberSaveable { mutableStateOf(TopLevelRoute.DASHBOARD) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profileUiState.snackbarMessage) {
        profileUiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            profileViewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = modifier.padding(innerPadding),
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(320.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    // Header section inside Left Navigation Drawer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            // App Brand & Version Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Personal Sport",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "v1.0.0",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Fitness & Health Intelligence",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive User Profile Header Card inside Drawer
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        showProfileDialog = true
                                        scope.launch { drawerState.close() }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        UserProfileAvatar(
                                            presetId = currentProfile.avatarPresetId,
                                            customPhotoUri = currentProfile.customPhotoUri,
                                            size = 44.dp
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        ) {}
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentProfile.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${currentProfile.genderFormatted} • ${currentProfile.paymentMethodFormatted}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "Editar Perfil",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // List of main navigation screens in Drawer
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MENÚ PRINCIPAL DE NAVEGACIÓN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        TopLevelRoute.entries.forEach { route ->
                            val isSelected = selectedTab == route
                            val icon = when (route) {
                                TopLevelRoute.DASHBOARD -> Icons.Rounded.Dashboard
                                TopLevelRoute.PLANNER -> Icons.AutoMirrored.Rounded.ListAlt
                                TopLevelRoute.EQUIPMENT -> Icons.Rounded.FitnessCenter
                                TopLevelRoute.MANUAL_ENTRY -> Icons.Rounded.AddCircleOutline
                                TopLevelRoute.PROGRESS -> Icons.Rounded.PhotoCamera
                                TopLevelRoute.SUPPLEMENTS -> Icons.Rounded.LocalPharmacy
                                TopLevelRoute.TRAINERS -> Icons.Rounded.PersonSearch
                                TopLevelRoute.DIET -> Icons.Rounded.Restaurant
                                TopLevelRoute.DEVICES_MANAGER -> Icons.Rounded.Devices
                                TopLevelRoute.ACHIEVEMENTS -> Icons.Rounded.EmojiEvents
                            }

                            NavigationDrawerItem(
                                label = {
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = route.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                        Text(
                                            text = route.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = route.title
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    selectedTab = route
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Footer section in Left Navigation Drawer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Personal Sport v1.0.0 • Profile Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        ) {
            val onOpenDrawer: () -> Unit = {
                scope.launch { drawerState.open() }
            }
            val onOpenProfile: () -> Unit = {
                showProfileDialog = true
            }

            when (selectedTab) {
                TopLevelRoute.DASHBOARD -> DashboardScreen(
                    healthSyncManager = healthSyncManager,
                    onNavigateToPlanner = { selectedTab = TopLevelRoute.PLANNER },
                    onNavigateToEquipment = { selectedTab = TopLevelRoute.EQUIPMENT },
                    onNavigateToLog = { selectedTab = TopLevelRoute.MANUAL_ENTRY },
                    onNavigateToProgress = { selectedTab = TopLevelRoute.PROGRESS },
                    onNavigateToSupplements = { selectedTab = TopLevelRoute.SUPPLEMENTS },
                    onNavigateToTrainers = { selectedTab = TopLevelRoute.TRAINERS },
                    onNavigateToDiet = { selectedTab = TopLevelRoute.DIET },
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.PLANNER -> PlannerScreen(
                    onNavigateToEquipment = { selectedTab = TopLevelRoute.EQUIPMENT },
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.EQUIPMENT -> EquipmentScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.SUPPLEMENTS -> SupplementHubScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.TRAINERS -> PersonalTrainerScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.PROGRESS -> ProgressPhotoScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.MANUAL_ENTRY -> ManualEntryScreen(
                    onSaveComplete = { selectedTab = TopLevelRoute.DASHBOARD },
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.DIET -> DietScreen(
                    onNavigateToSupplements = { selectedTab = TopLevelRoute.SUPPLEMENTS },
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.DEVICES_MANAGER -> DevicesManagerScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
                TopLevelRoute.ACHIEVEMENTS -> AchievementsScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    }

    if (showProfileDialog) {
        UserProfileDialog(
            viewModel = profileViewModel,
            onDismissRequest = { showProfileDialog = false }
        )
    }
}
