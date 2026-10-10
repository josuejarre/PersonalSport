package com.example.personalsport.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalsport.ai.LocalSportsKnowledgeBase
import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PresetAvatars
import com.example.personalsport.data.repository.UserProfileRepository
import com.example.personalsport.ui.profile.UserProfileAvatar
import com.example.personalsport.ui.theme.PersonalSportTheme

/**
 * Pantalla Principal del Asistente de Registro e Integración Inicial ([OnboardingScreen]).
 *
 * Implementa el wizard de 4 Pasos:
 * 1. Saludo & Felicitación M3
 * 2. Registro de Perfil de Usuario (Nombre, Apellidos, Sexo, Edad, Avatar)
 * 3. Sincronización de Relojes y Básculas Bioimpedancia (Huawei, Amazfit, Xiaomi, Tanita, Honor, Garmin)
 * 4. Ficha Biológica & Transferencia a Qwen IA
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Bienvenido a PersonalSport",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Horizontal Step Progress Bar & Indicators
            OnboardingStepHeader(
                currentStep = uiState.currentStep,
                onStepClick = { step -> viewModel.goToStep(step) }
            )

            // Content Area based on currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "OnboardingStepTransition"
                ) { step ->
                    when (step) {
                        1 -> Step1GreetingCard(
                            onNext = { viewModel.nextStep() }
                        )
                        2 -> Step2UserRegistrationForm(
                            uiState = uiState,
                            onFirstNameChange = { viewModel.updateFirstName(it) },
                            onLastNameChange = { viewModel.updateLastName(it) },
                            onGenderChange = { viewModel.updateGender(it) },
                            onAgeChange = { viewModel.updateAge(it) },
                            onAvatarSelect = { presetId -> viewModel.updateAvatar(presetId) },
                            onNext = { viewModel.nextStep() },
                            onBack = { viewModel.previousStep() }
                        )
                        3 -> Step3DeviceSyncGuidance(
                            pairedBrands = uiState.pairedBrands,
                            onTogglePairing = { brand -> viewModel.toggleDevicePairing(brand) },
                            onNext = { viewModel.nextStep() },
                            onBack = { viewModel.previousStep() }
                        )
                        4 -> Step4BiologicalTransferToAi(
                            uiState = uiState,
                            onWeightChange = { viewModel.updateWeight(it) },
                            onBodyFatChange = { viewModel.updateBodyFat(it) },
                            onVisceralFatChange = { viewModel.updateVisceralFat(it) },
                            onBoneMassChange = { viewModel.updateBoneMass(it) },
                            onMuscleMassChange = { viewModel.updateMuscleMass(it) },
                            onComplete = {
                                viewModel.completeOnboarding()
                                onOnboardingFinished()
                            },
                            onBack = { viewModel.previousStep() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step Progress Header with Visual Indicators.
 */
@Composable
private fun OnboardingStepHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paso $currentStep de 4",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val stepTitles = listOf("Bienvenida", "Registro", "Sincronización", "Qwen IA")
            Text(
                text = stepTitles.getOrElse(currentStep - 1) { "" },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { currentStep / 4f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (step in 1..4) {
                val isSelected = step == currentStep
                val isPassed = step < currentStep

                val containerColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isPassed -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }

                val contentColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isPassed -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onStepClick(step) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPassed) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Completado",
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$step",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Paso 1: Saludo & Felicitación.
 */
@Composable
private fun Step1GreetingCard(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Main Visual Header Hero Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🎉",
                                    fontSize = 36.sp
                                )
                            }
                        }

                        Text(
                            text = "¡Te damos la bienvenida!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "🎉 ¡Bienvenido/a a PersonalSport! Te felicitamos por dar el paso hacia una vida más sana, activa y en forma. Estamos aquí para guiarte en cada paso.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Highlights Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "¿Qué encontrarás en tu app?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                GreetingFeatureRow(
                    icon = Icons.Rounded.Psychology,
                    title = "Entrenador Local Qwen IA",
                    subtitle = "Modelos 100% privados y locales adaptados a tu fisiología"
                )

                GreetingFeatureRow(
                    icon = Icons.Rounded.Devices,
                    title = "Sincronización Multimarca",
                    subtitle = "Soporte directo para Huawei, Amazfit, Xiaomi, Tanita, Honor y Garmin"
                )

                GreetingFeatureRow(
                    icon = Icons.Rounded.Restaurant,
                    title = "Nutrición & Dieta Flexible IIFYM",
                    subtitle = "Recetarios de alta proteína, macro friendly y rendimiento vegano"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Comenzar Mi Registro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun GreetingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Paso 2: Registro de Usuario.
 */
@Composable
private fun Step2UserRegistrationForm(
    uiState: OnboardingUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onAgeChange: (Int) -> Unit,
    onAvatarSelect: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Paso 2: Registro de Usuario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ingresa tus datos personales para personalizar tu plan y avatar de entrenamiento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Form Inputs
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Nombre") },
                placeholder = { Text("Ej. Alex") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Apellidos") },
                placeholder = { Text("Ej. Rodríguez") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Gender Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sexo / Género Biological",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Gender.entries.forEach { gender ->
                        val isSelected = uiState.gender == gender
                        FilterChip(
                            selected = isSelected,
                            onClick = { onGenderChange(gender) },
                            label = {
                                Text(
                                    text = gender.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Age Input
            OutlinedTextField(
                value = if (uiState.age > 0) uiState.age.toString() else "",
                onValueChange = { input ->
                    val ageVal = input.filter { it.isDigit() }.toIntOrNull() ?: 28
                    onAgeChange(ageVal)
                },
                label = { Text("Edad (Años)") },
                placeholder = { Text("28") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Rounded.Cake, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Avatar Preset Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Selecciona tu Foto / Avatar Deportivo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(PresetAvatars.OPTIONS) { option ->
                        val isSelected = uiState.selectedAvatarPresetId == option.id
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .width(120.dp)
                                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                                .clickable { onAvatarSelect(option.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UserProfileAvatar(
                                    presetId = option.id,
                                    customPhotoUri = null,
                                    size = 48.dp
                                )
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Atrás", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
            ) {
                Text("Guardar y Continuar", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

/**
 * Paso 3: Sincronización de Dispositivos & Básculas.
 */
@Composable
private fun Step3DeviceSyncGuidance(
    pairedBrands: Set<String>,
    onTogglePairing: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val supportedDevices = listOf(
        Pair("Huawei Watch GT", "Relojes Huawei GT / Band & Huawei Health"),
        Pair("Amazfit", "Relojes Amazfit Bip, GTS, GTR & Zepp Life"),
        Pair("Xiaomi", "Básculas Mi Body Composition & Mi Band"),
        Pair("Tanita", "Básculas de Bioimpedancia Tanita RD / BC"),
        Pair("Honor", "Honor MagicWatch & Básculas Honor Choice"),
        Pair("Garmin", "Garmin Forerunner, Fenix & Index Smart Scale")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Paso 3: Dispositivos & Básculas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Vincula tus dispositivos wearables y básculas inteligentes para sincronizar de forma automática tu frecuencia cardíaca, peso y % de grasa corporal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Brand Sync Grid / List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                supportedDevices.forEach { (brand, description) ->
                    val isPaired = pairedBrands.contains(brand)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPaired) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePairing(brand) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isPaired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (brand.contains("Báscula") || brand == "Tanita" || brand == "Xiaomi") Icons.Rounded.MonitorWeight else Icons.Rounded.Watch,
                                        contentDescription = null,
                                        tint = if (isPaired) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = brand,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onTogglePairing(brand) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isPaired) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isPaired) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Sincronizado", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Vincular", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Atrás", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
            ) {
                Text("Ficha Biológica", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

/**
 * Paso 4: Bienvenida Final & Transferencia Biológica a Qwen IA.
 */
@Composable
private fun Step4BiologicalTransferToAi(
    uiState: OnboardingUiState,
    onWeightChange: (Float) -> Unit,
    onBodyFatChange: (Float) -> Unit,
    onVisceralFatChange: (Float) -> Unit,
    onBoneMassChange: (Float) -> Unit,
    onMuscleMassChange: (Float) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Paso 4: Ficha Biológica & Qwen IA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Athlete Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserProfileAvatar(
                            presetId = uiState.selectedAvatarPresetId,
                            customPhotoUri = uiState.customPhotoUri,
                            size = 54.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.firstName.ifBlank { "Atleta PersonalSport" } + if (uiState.lastName.isNotBlank()) " ${uiState.lastName}" else "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Atleta • ${uiState.gender.displayName} • ${uiState.age} Años",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Ajusta tus parámetros biológicos de inicio:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Biological Parameters Input Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BioParamChip(
                            label = "Peso (kg)",
                            value = "${uiState.weightKg} kg",
                            onIncrement = { onWeightChange(uiState.weightKg + 0.5f) },
                            onDecrement = { onWeightChange(uiState.weightKg - 0.5f) },
                            modifier = Modifier.weight(1f)
                        )
                        BioParamChip(
                            label = "% Grasa",
                            value = "${uiState.bodyFatPercentage} %",
                            onIncrement = { onBodyFatChange(uiState.bodyFatPercentage + 0.5f) },
                            onDecrement = { onBodyFatChange(uiState.bodyFatPercentage - 0.5f) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BioParamChip(
                            label = "Grasa Visceral",
                            value = "Nivel ${uiState.visceralFat.toInt()}",
                            onIncrement = { onVisceralFatChange(uiState.visceralFat + 1f) },
                            onDecrement = { onVisceralFatChange(uiState.visceralFat - 1f) },
                            modifier = Modifier.weight(1f)
                        )
                        BioParamChip(
                            label = "Masa Ósea",
                            value = "${uiState.boneMassKg} kg",
                            onIncrement = { onBoneMassChange(uiState.boneMassKg + 0.1f) },
                            onDecrement = { onBoneMassChange(uiState.boneMassKg - 0.1f) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    BioParamChip(
                        label = "Masa Muscular",
                        value = "${uiState.muscleMassKg} kg",
                        onIncrement = { onMuscleMassChange(uiState.muscleMassKg + 0.5f) },
                        onDecrement = { onMuscleMassChange(uiState.muscleMassKg - 0.5f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Qwen AI Confirmation Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Qwen Local AI Carga Completada",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Qwen Local AI y LocalSportsKnowledgeBase se han cargado con tus métricas biológicas (Edad: ${uiState.age}, Sexo: ${uiState.gender.displayName}, Peso: ${uiState.weightKg}kg, Grasa: ${uiState.bodyFatPercentage}%). ¡Listos para generar tus rutinas y dietas!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Complete Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Atrás", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(2f)
                    .height(54.dp)
            ) {
                Text(
                    text = "🚀 ¡Comenzar PersonalSport!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BioParamChip(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Disminuir", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    val dummyVm = remember {
        OnboardingViewModel(UserProfileRepository(null))
    }

    PersonalSportTheme {
        OnboardingScreen(
            viewModel = dummyVm
        )
    }
}
