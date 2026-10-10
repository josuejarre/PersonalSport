package com.example.personalsport.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PaymentMethodType
import com.example.personalsport.data.model.PresetAvatars
import com.example.personalsport.data.model.UserProfile
import com.example.personalsport.export.PerformanceReportExporter
import com.example.personalsport.util.AppCoilImageLoader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileDialog(
    viewModel: UserProfileViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onCustomPhotoPicked(it.toString())
        }
    }

    Dialog(
        onDismissRequest = {
            if (uiState.isEditing) {
                viewModel.cancelEditing()
            }
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header with Title and Mode Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = if (uiState.isEditing) Icons.Rounded.EditNote else Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (uiState.isEditing) "Editar Perfil" else "Perfil de Atleta",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isEditing) "Actualiza tus datos y preferencias" else "Personal Sport",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (uiState.isEditing) viewModel.cancelEditing()
                            onDismissRequest()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                HorizontalDivider()

                if (!uiState.isEditing) {
                    // READ / VIEW MODE
                    ProfileViewContent(
                        profile = uiState.profile,
                        onStartEditing = { viewModel.startEditing() },
                        onDismiss = onDismissRequest
                    )
                } else {
                    // EDIT MODE
                    ProfileEditContent(
                        uiState = uiState,
                        onNameChange = viewModel::onNameChanged,
                        onGenderSelect = viewModel::onGenderSelected,
                        onAvatarPresetSelect = viewModel::onAvatarPresetSelected,
                        onPickPhoto = { photoPickerLauncher.launch("image/*") },
                        onClearCustomPhoto = { viewModel.onCustomPhotoPicked(null) },
                        onPaymentTypeSelect = viewModel::onPaymentTypeSelected,
                        onPaymentLast4Change = viewModel::onPaymentLast4Changed,
                        onPaymentEmailChange = viewModel::onPaymentEmailChanged,
                        onSave = { viewModel.saveProfile() },
                        onCancel = { viewModel.cancelEditing() }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileViewContent(
    profile: UserProfile,
    onStartEditing: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Main Profile Badge Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserProfileAvatar(
                    presetId = profile.avatarPresetId,
                    customPhotoUri = profile.customPhotoUri,
                    size = 90.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = profile.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = profile.genderFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = profile.fitnessLevel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Profile Details Summary Cards
        Text(
            text = "INFORMACIÓN PERSONAL Y DETALLES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileDetailItem(
                icon = Icons.Rounded.Badge,
                label = "Nombre Completo",
                value = profile.fullName
            )

            ProfileDetailItem(
                icon = when (profile.gender) {
                    Gender.MASCULINO -> Icons.Rounded.Male
                    Gender.FEMENINO -> Icons.Rounded.Female
                    Gender.OTRO -> Icons.Rounded.Person
                },
                label = "Sexo / Género",
                value = profile.genderFormatted
            )

            ProfileDetailItem(
                icon = getPaymentIcon(profile.paymentMethodType),
                label = "Medio de Pago",
                value = profile.paymentMethodFormatted,
                valueColor = if (profile.paymentMethodType == PaymentMethodType.SIN_REGISTRAR)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            ProfileDetailItem(
                icon = Icons.Rounded.CheckCircle,
                label = "Estado de Suscripción",
                value = profile.subscriptionStatus,
                valueColor = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PDF Export Button
        val context = LocalContext.current
        val exporter = remember { PerformanceReportExporter(context) }

        OutlinedButton(
            onClick = {
                val pdf = exporter.generateReportPdf(profileName = profile.fullName)
                exporter.openPdfReport(pdf)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar Informe PDF", fontWeight = FontWeight.Bold)
        }

        // Actions: Edit vs Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cerrar")
            }

            Button(
                onClick = onStartEditing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Editar Perfil")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditContent(
    uiState: UserProfileUiState,
    onNameChange: (String) -> Unit,
    onGenderSelect: (Gender) -> Unit,
    onAvatarPresetSelect: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onClearCustomPhoto: () -> Unit,
    onPaymentTypeSelect: (PaymentMethodType) -> Unit,
    onPaymentLast4Change: (String) -> Unit,
    onPaymentEmailChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Current Selected Avatar Preview
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserProfileAvatar(
                    presetId = uiState.draftAvatarPresetId,
                    customPhotoUri = uiState.draftCustomPhotoUri,
                    size = 80.dp
                )

                if (uiState.draftCustomPhotoUri != null) {
                    TextButton(onClick = onClearCustomPhoto) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quitar foto y usar avatar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 1. Nombre Completo
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Nombre Completo",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = uiState.draftName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe tu nombre completo") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                isError = uiState.errorMessage != null,
                shape = RoundedCornerShape(12.dp)
            )
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // 2. Sexo / Género
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Sexo / Género",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Gender.entries.forEach { gender ->
                    val isSelected = uiState.draftGender == gender
                    FilterChip(
                        selected = isSelected,
                        onClick = { onGenderSelect(gender) },
                        label = { Text(gender.displayName) },
                        leadingIcon = {
                            val icon = when (gender) {
                                Gender.MASCULINO -> Icons.Rounded.Male
                                Gender.FEMENINO -> Icons.Rounded.Female
                                Gender.OTRO -> Icons.Rounded.Person
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // 3. Foto de Perfil & Avatar
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Foto de Perfil & Avatar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onPickPhoto,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir Foto", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                text = "O selecciona uno de los avatares prediseñados:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(PresetAvatars.OPTIONS) { option ->
                    val isSelected = uiState.draftAvatarPresetId == option.id && uiState.draftCustomPhotoUri == null
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "border"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { onAvatarPresetSelect(option.id) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarColor(option.id)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getAvatarIcon(option.id),
                                    contentDescription = option.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 4. Medio de Pago
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Medio de Pago",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodType.entries.forEach { pType ->
                    val isSelected = uiState.draftPaymentType == pType
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPaymentTypeSelect(pType) },
                        label = { Text(pType.displayName) },
                        leadingIcon = {
                            Icon(getPaymentIcon(pType), contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.draftPaymentType == PaymentMethodType.VISA ||
                        uiState.draftPaymentType == PaymentMethodType.MASTERCARD
            ) {
                OutlinedTextField(
                    value = uiState.draftPaymentLast4,
                    onValueChange = onPaymentLast4Change,
                    label = { Text("Últimos 4 dígitos de la tarjeta") },
                    placeholder = { Text("Ej. 4242") },
                    leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            AnimatedVisibility(
                visible = uiState.draftPaymentType == PaymentMethodType.PAYPAL ||
                        uiState.draftPaymentType == PaymentMethodType.GOOGLE_PAY
            ) {
                OutlinedTextField(
                    value = uiState.draftPaymentEmail,
                    onValueChange = onPaymentEmailChange,
                    label = { Text("Correo o cuenta asociada") },
                    placeholder = { Text("atleta@personalsport.app") },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Save & Cancel Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar")
            }
        }
    }
}

@Composable
fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
fun UserProfileAvatar(
    presetId: String,
    customPhotoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    if (!customPhotoUri.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(customPhotoUri)
                .crossfade(true)
                .build(),
            imageLoader = AppCoilImageLoader.get(LocalContext.current),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            getAvatarColor(presetId),
                            getAvatarColor(presetId).copy(alpha = 0.75f)
                        )
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getAvatarIcon(presetId),
                contentDescription = "Avatar de Perfil",
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

fun getAvatarIcon(presetId: String): ImageVector {
    return when (presetId) {
        "avatar_athlete" -> Icons.Rounded.Person
        "avatar_runner" -> Icons.AutoMirrored.Rounded.DirectionsRun
        "avatar_gym" -> Icons.Rounded.FitnessCenter
        "avatar_yoga" -> Icons.Rounded.SelfImprovement
        "avatar_cyclist" -> Icons.AutoMirrored.Rounded.DirectionsBike
        "avatar_trophy" -> Icons.Rounded.EmojiEvents
        "avatar_star" -> Icons.Rounded.Star
        "avatar_gymnastics" -> Icons.Rounded.SportsGymnastics
        else -> Icons.Rounded.AccountCircle
    }
}

fun getAvatarColor(presetId: String): Color {
    return when (presetId) {
        "avatar_athlete" -> Color(0xFF1E88E5) // Blue
        "avatar_runner" -> Color(0xFFE53935) // Red/Orange
        "avatar_gym" -> Color(0xFF43A047) // Green
        "avatar_yoga" -> Color(0xFF8E24AA) // Purple
        "avatar_cyclist" -> Color(0xFFFB8C00) // Orange
        "avatar_trophy" -> Color(0xFFFDD835) // Yellow/Gold
        "avatar_star" -> Color(0xFF00ACC1) // Cyan
        "avatar_gymnastics" -> Color(0xFFD81B60) // Pink
        else -> Color(0xFF3F51B5)
    }
}

fun getPaymentIcon(type: PaymentMethodType): ImageVector {
    return when (type) {
        PaymentMethodType.VISA, PaymentMethodType.MASTERCARD -> Icons.Rounded.CreditCard
        PaymentMethodType.GOOGLE_PAY -> Icons.Rounded.Smartphone
        PaymentMethodType.PAYPAL -> Icons.Rounded.AccountBalanceWallet
        PaymentMethodType.SIN_REGISTRAR -> Icons.AutoMirrored.Rounded.Help
    }
}
