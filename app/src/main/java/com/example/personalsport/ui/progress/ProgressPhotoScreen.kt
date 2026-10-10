package com.example.personalsport.ui.progress

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.util.AppCoilImageLoader
import com.example.personalsport.ai.TransformationOutcome
import com.example.personalsport.ai.VisualAnalysisResult
import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Pantalla de Captura, Galería Comparativa y Análisis Antropométrico 3D por IA ([ProgressPhotoScreen]).
 *
 * Muestra el registro visual del cambio corporal del usuario:
 * - Visor de cámara en vivo con CameraX para captura guiada de fotos (Frontal, Perfil, Espalda).
 * - Galería de fotos históricas con filtros por tipo de ángulo.
 * - Modo comparativo Antes vs Después con análisis antropométrico visual (V-Taper, Pecho-Cintura, Definición).
 * - Análisis local por IA combinando las imágenes con los datos de peso y grasa de Room DB.
 *
 * **Manejo de Estado y Despacho de Hilos**:
 * - Recolecta el estado ProgressPhotoUiState de ProgressPhotoViewModel en el hilo principal de UI.
 * - La captura de cámara y el análisis matricial de píxeles se ejecutan en corrutinas en hilos secundarios de E/S.
 *
 * @param onOpenDrawer Callback para desplegar el menú de navegación lateral.
 * @param onOpenProfile Callback para abrir la ventana del perfil de usuario.
 * @param modifier Modificador de diseño Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPhotoScreen(
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val factory = remember {
        ProgressPhotoViewModelFactory(
            repository = app.progressPhotoRepository,
            bodyMetricRepository = app.bodyMetricRepository
        )
    }
    val viewModel: ProgressPhotoViewModel = viewModel(factory = factory)

    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Gallery & Compare, 1: Camera
    var showComparisonDialog by remember { mutableStateOf(false) }
    var noteEditingPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }
    var editNotesText by remember { mutableStateOf("") }

    LaunchedEffect(state.cameraState.isCameraMode) {
        if (state.cameraState.isCameraMode && selectedTab != 1) {
            selectedTab = 1
        } else if (!state.cameraState.isCameraMode && selectedTab == 1 && state.cameraState.temporaryPhotoUri == null) {
            selectedTab = 0
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
                                if (selectedTab == 0) "Progress Photos (${state.photos.size})" else "Capture Progress",
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
                    if (selectedTab == 0 && state.selectedForComparison.size == 2) {
                        IconButton(onClick = { showComparisonDialog = true }) {
                            BadgeBox(state.selectedForComparison.size) {
                                Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = "Compare Selected")
                            }
                        }
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
            if (selectedTab == 0 && !state.cameraState.isCameraMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.setCameraMode(true)
                        selectedTab = 1
                    },
                    icon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                    text = { Text("Take Photo") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .perforatedMeshBackground()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        viewModel.setCameraMode(false)
                    },
                    text = { Text("History & Compare") },
                    icon = { Icon(Icons.Rounded.Collections, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.setCameraMode(true)
                    },
                    text = { Text("Camera") },
                    icon = { Icon(Icons.Rounded.CameraAlt, contentDescription = null) }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                label = "ProgressTabTransition"
            ) { tab ->
                when (tab) {
                    0 -> ProgressGalleryContent(
                        photos = state.photos,
                        selectedForComparison = state.selectedForComparison,
                        onToggleCompare = { viewModel.togglePhotoForComparison(it) },
                        onPhotoClick = { viewModel.setSelectedPhotoForDetail(it) },
                        onOpenComparison = { showComparisonDialog = true },
                        onClearComparison = { viewModel.clearComparisonSelection() },
                        onTakePhotoClick = {
                            viewModel.setCameraMode(true)
                            selectedTab = 1
                        }
                    )
                    1 -> ProgressCameraFlow(
                        cameraState = state.cameraState,
                        onPhotoCaptured = { viewModel.onPhotoCaptured(it) },
                        onSavePhoto = { notes -> viewModel.saveCapturedPhoto(notes) },
                        onCancelCaptured = { viewModel.cancelCapturedPhoto() },
                        onCloseCamera = {
                            viewModel.setCameraMode(false)
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }
}

    // Detail Dialog for single photo
    state.selectedPhotoForDetail?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { viewModel.setSelectedPhotoForDetail(null) },
            onEditNotes = {
                noteEditingPhoto = photo
                editNotesText = photo.notes
            },
            onDelete = {
                viewModel.deletePhoto(photo)
            }
        )
    }

    // Edit Notes Dialog
    noteEditingPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { noteEditingPhoto = null },
            title = { Text("Edit Photo Notes") },
            text = {
                OutlinedTextField(
                    value = editNotesText,
                    onValueChange = { editNotesText = it },
                    label = { Text("Notes") },
                    placeholder = { Text("e.g. Morning check-in, 78kg") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePhotoNotes(photo, editNotesText)
                        noteEditingPhoto = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteEditingPhoto = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Comparison Dialog / Screen Overlay
    if (showComparisonDialog && state.selectedForComparison.size == 2) {
        SideBySideComparisonDialog(
            photo1 = state.selectedForComparison[0],
            photo2 = state.selectedForComparison[1],
            isAnalyzing = state.isAnalyzingProgress,
            analysisResult = state.visualAnalysisResult,
            onAnalyzeClick = { viewModel.runVisualAiAnalysis(context) },
            onDismiss = { showComparisonDialog = false },
            onClear = {
                viewModel.clearComparisonSelection()
                showComparisonDialog = false
            }
        )
    }
}

@Composable
fun BadgeBox(
    count: Int,
    content: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        content()
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier.size(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProgressGalleryContent(
    photos: List<ProgressPhoto>,
    selectedForComparison: List<ProgressPhoto>,
    onToggleCompare: (ProgressPhoto) -> Unit,
    onPhotoClick: (ProgressPhoto) -> Unit,
    onOpenComparison: () -> Unit,
    onClearComparison: () -> Unit,
    onTakePhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (selectedForComparison.isNotEmpty()) {
            ComparisonBar(
                selectedCount = selectedForComparison.size,
                onOpenComparison = onOpenComparison,
                onClearComparison = onClearComparison,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        "No Progress Photos Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Take regular photos to visually track your fitness transformation over time.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onTakePhotoClick,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take First Photo")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos, key = { it.id }) { photo ->
                    val isSelected = selectedForComparison.any { it.id == photo.id }
                    val selectedIndex = selectedForComparison.indexOfFirst { it.id == photo.id }

                    ProgressPhotoCard(
                        photo = photo,
                        isSelectedForCompare = isSelected,
                        compareIndex = if (selectedIndex >= 0) selectedIndex + 1 else null,
                        onToggleCompare = { onToggleCompare(photo) },
                        onClick = { onPhotoClick(photo) }
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonBar(
    selectedCount: Int,
    onOpenComparison: () -> Unit,
    onClearComparison: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        "$selectedCount / 2 Selected to Compare",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (selectedCount == 1) "Tap another photo to compare" else "Ready for side-by-side view!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedCount == 2) {
                    Button(
                        onClick = onOpenComparison,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Compare")
                    }
                }
                IconButton(onClick = onClearComparison) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear Selection",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressPhotoCard(
    photo: ProgressPhoto,
    isSelectedForCompare: Boolean,
    compareIndex: Int?,
    onToggleCompare: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = photo.photoUri,
                imageLoader = AppCoilImageLoader.get(LocalContext.current),
                contentDescription = "Progress Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top gradient overlay for action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )

            // Comparison Selection Checkbox
            IconButton(
                onClick = onToggleCompare,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                if (isSelectedForCompare && compareIndex != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = compareIndex.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "Select to compare",
                        tint = Color.White
                    )
                }
            }

            // Bottom gradient overlay with date and notes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        formatDate(photo.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (photo.notes.isNotBlank()) {
                        Text(
                            photo.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressCameraFlow(
    cameraState: CameraCaptureState,
    onPhotoCaptured: (Uri) -> Unit,
    onSavePhoto: (String) -> Unit,
    onCancelCaptured: () -> Unit,
    onCloseCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Rounded.CameraEnhance,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Camera Access Needed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "PersonalSport needs access to your camera so you can take and save progress photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Camera Permission")
                }
            }
        }
    } else {
        if (cameraState.temporaryPhotoUri != null) {
            CapturedPhotoSaveDialog(
                photoUri = cameraState.temporaryPhotoUri,
                initialNotes = cameraState.photoNotesInput,
                onSave = onSavePhoto,
                onRetake = onCancelCaptured
            )
        } else {
            CameraXCaptureView(
                onPhotoCaptured = onPhotoCaptured,
                onClose = onCloseCamera,
                modifier = modifier
            )
        }
    }
}

@Composable
fun CameraXCaptureView(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showGridOverlay by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()
    }

    LaunchedEffect(lensFacing, flashMode) {
        imageCapture.flashMode = flashMode
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraXCaptureView", "Error binding camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun triggerPhotoCapture() {
        if (isCapturing) return
        isCapturing = true

        val photoDir = File(context.filesDir, "progress_photos")
        if (!photoDir.exists()) photoDir.mkdirs()

        val photoFile = File(photoDir, "progress_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    onPhotoCaptured(Uri.fromFile(photoFile))
                }

                override fun onError(exc: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(context, "Error saving photo: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (showGridOverlay) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }

        // Top controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Grid Toggle
                IconButton(onClick = { showGridOverlay = !showGridOverlay }) {
                    Icon(
                        if (showGridOverlay) Icons.Rounded.GridOn else Icons.Rounded.GridOff,
                        contentDescription = "Toggle Grid",
                        tint = Color.White
                    )
                }

                // Flash Mode Toggle
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    }
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                        else -> Icons.Rounded.FlashOff
                    }
                    Icon(icon, contentDescription = "Flash Mode", tint = Color.White)
                }

                // Lens Switch Toggle
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                }
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { triggerPhotoCapture() },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(76.dp)
                    .background(Color.White, CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        contentDescription = "Capture Photo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val gridColor = Color.White.copy(alpha = 0.35f)
        val strokeWidth = 1.dp.toPx()

        // Vertical lines
        drawLine(
            color = gridColor,
            start = Offset(width / 3f, 0f),
            end = Offset(width / 3f, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(width * 2f / 3f, 0f),
            end = Offset(width * 2f / 3f, height),
            strokeWidth = strokeWidth
        )

        // Horizontal lines
        drawLine(
            color = gridColor,
            start = Offset(0f, height / 3f),
            end = Offset(width, height / 3f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, height * 2f / 3f),
            end = Offset(width, height * 2f / 3f),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun CapturedPhotoSaveDialog(
    photoUri: Uri,
    initialNotes: String,
    onSave: (String) -> Unit,
    onRetake: () -> Unit
) {
    var notesText by remember { mutableStateOf(initialNotes) }

    Dialog(
        onDismissRequest = onRetake,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Review Progress Photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = photoUri,
                        imageLoader = AppCoilImageLoader.get(LocalContext.current),
                        contentDescription = "Captured Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Progress Notes (Optional)") },
                    placeholder = { Text("e.g. Morning check-in, 80kg, post-workout") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retake")
                    }

                    Button(
                        onClick = { onSave(notesText) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Photo")
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoDetailDialog(
    photo: ProgressPhoto,
    onDismiss: () -> Unit,
    onEditNotes: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Progress Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatDate(photo.date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = photo.photoUri,
                        imageLoader = AppCoilImageLoader.get(LocalContext.current),
                        contentDescription = "Progress Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = onEditNotes) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Notes")
                            }
                        }
                        Text(
                            text = photo.notes.ifBlank { "No notes attached." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (photo.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Photo")
                    }
                }
            }
        }
    }
}

@Composable
fun SideBySideComparisonDialog(
    photo1: ProgressPhoto,
    photo2: ProgressPhoto,
    isAnalyzing: Boolean = false,
    analysisResult: VisualAnalysisResult? = null,
    onAnalyzeClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    // Sort so photo1 is earlier and photo2 is later
    val (earlierPhoto, laterPhoto) = if (photo1.date <= photo2.date) {
        Pair(photo1, photo2)
    } else {
        Pair(photo2, photo1)
    }

    val daysDiffText = calculateDaysDifference(earlierPhoto.date, laterPhoto.date)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Side-by-Side Visual Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Time interval: $daysDiffText",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Side-by-side images
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComparisonPhotoCard(
                        title = "BEFORE",
                        photo = earlierPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    ComparisonPhotoCard(
                        title = "AFTER",
                        photo = laterPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                Spacer(Modifier.height(16.dp))

                // AI Analysis Trigger Button / Card
                if (isAnalyzing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Column {
                                Text(
                                    "Analizando silueta y antropometría...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Normalizando alineación de fotos y procesando métricas Room DB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else if (analysisResult == null) {
                    Button(
                        onClick = onAnalyzeClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Analizar Progreso con IA Local",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    AIVisualAnalysisCard(
                        result = analysisResult,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onClear) {
                        Text("Clear Selection")
                    }

                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun AIVisualAnalysisCard(
    result: VisualAnalysisResult,
    modifier: Modifier = Modifier
) {
    val outcome = result.outcome
    val (badgeBgColor, badgeTextColor) = when (outcome) {
        TransformationOutcome.FAT_LOSS, TransformationOutcome.BODY_RECOMPOSITION ->
            Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        TransformationOutcome.MUSCLE_GAIN ->
            Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        TransformationOutcome.MAINTENANCE ->
            Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Análisis Antropométrico IA Local",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Visión Computacional & Fusión Multi-Modal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = { },
                        icon = {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "${result.confidenceScore}% Confianza IA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Outcome Classification Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBgColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = outcome.titleEs,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "Procesamiento 100% On-Device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = outcome.descriptionEs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Ratio Changes Summary Row
            Text(
                "Cambios Antropométricos de Silueta:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatioMetricChip(
                    label = "V-Taper Ratio",
                    value = formatSignedPct(result.ratioChanges.vTaperChangePct),
                    modifier = Modifier.weight(1f)
                )
                RatioMetricChip(
                    label = "Pecho / Cintura",
                    value = formatSignedPct(result.ratioChanges.chestToWaistChangePct),
                    modifier = Modifier.weight(1f)
                )
                RatioMetricChip(
                    label = "Definición",
                    value = formatSignedPct(result.ratioChanges.definitionChangePct),
                    modifier = Modifier.weight(1f)
                )
            }

            // Multi-Modal Weight & Body Fat Shift (if available in Room DB)
            val multiModal = result.multiModalData
            if (multiModal.weightChangeKg != null || multiModal.fatPercentageChangePct != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Fusión de Datos Room DB:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (multiModal.beforeWeightKg != null && multiModal.afterWeightKg != null) {
                                Text(
                                    "Peso: ${multiModal.beforeWeightKg}kg ➔ ${multiModal.afterWeightKg}kg (${formatSignedKg(multiModal.weightChangeKg!!)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (multiModal.beforeFatPct != null && multiModal.afterFatPct != null) {
                                Text(
                                    "Grasa: ${multiModal.beforeFatPct}% ➔ ${multiModal.afterFatPct}% (${formatSignedPct(multiModal.fatPercentageChangePct!!)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Explanation Breakdown text
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = result.explanationBreakdown,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Actionable Training & Nutrition Advice
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdviceBox(
                    icon = Icons.Rounded.FitnessCenter,
                    title = "Consejo de Entrenamiento IA",
                    content = result.trainingAdvice
                )
                AdviceBox(
                    icon = Icons.Rounded.Restaurant,
                    title = "Consejo de Nutrición IA",
                    content = result.nutritionAdvice
                )
            }

            // Local privacy badge
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Procesamiento 100% privado en tu dispositivo (Sin servidores externos)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RatioMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (value.startsWith("+")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AdviceBox(
    icon: ImageVector,
    title: String,
    content: String
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Text(content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ComparisonPhotoCard(
    title: String,
    photo: ProgressPhoto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Surface(
                color = if (title == "BEFORE") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = if (title == "BEFORE") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = photo.photoUri,
                    imageLoader = AppCoilImageLoader.get(LocalContext.current),
                    contentDescription = "$title progress photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = formatDate(photo.date),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            if (photo.notes.isNotBlank()) {
                Text(
                    text = photo.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun calculateDaysDifference(date1: Long, date2: Long): String {
    val diffMillis = abs(date2 - date1)
    val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    val hours = ((diffMillis / (1000 * 60 * 60)) % 24).toInt()
    return when {
        days > 0 -> "$days days apart"
        hours > 0 -> "$hours hours apart"
        else -> "Same day"
    }
}

private fun formatSignedPct(valPct: Float): String {
    return String.format(Locale.US, "%+.1f%%", valPct)
}

private fun formatSignedKg(valKg: Float): String {
    return String.format(Locale.US, "%+.1f kg", valKg)
}
