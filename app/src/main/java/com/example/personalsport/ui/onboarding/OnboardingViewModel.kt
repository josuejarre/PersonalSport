package com.example.personalsport.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PresetAvatars
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado UI del Asistente de Registro Inicial u Onboarding ([OnboardingUiState]).
 */
data class OnboardingUiState(
    val currentStep: Int = 1,
    val firstName: String = "",
    val lastName: String = "",
    val gender: Gender = Gender.MASCULINO,
    val age: Int = 28,
    val selectedAvatarPresetId: String = PresetAvatars.DEFAULT_AVATAR_ID,
    val customPhotoUri: String? = null,
    val weightKg: Float = 70.0f,
    val bodyFatPercentage: Float = 15.0f,
    val visceralFat: Float = 5.0f,
    val boneMassKg: Float = 3.2f,
    val muscleMassKg: Float = 32.5f,
    val pairedBrands: Set<String> = emptySet(),
    val isAiTransferred: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * ViewModel que gestiona la lógica del flujo de Onboarding de 4 Pasos ([OnboardingViewModel]).
 */
class OnboardingViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val bodyMetricRepository: BodyMetricRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            firstName = userProfileRepository.getProfile().fullName.takeIf { it != "Atleta PersonalSport" } ?: "",
            lastName = userProfileRepository.getProfile().lastName,
            gender = userProfileRepository.getProfile().gender,
            age = userProfileRepository.getProfile().age,
            selectedAvatarPresetId = userProfileRepository.getProfile().avatarPresetId,
            customPhotoUri = userProfileRepository.getProfile().customPhotoUri,
            weightKg = userProfileRepository.getProfile().weightKg,
            bodyFatPercentage = userProfileRepository.getProfile().bodyFatPercentage,
            visceralFat = userProfileRepository.getProfile().visceralFat,
            boneMassKg = userProfileRepository.getProfile().boneMassKg,
            muscleMassKg = userProfileRepository.getProfile().muscleMassKg,
            isOnboardingCompleted = userProfileRepository.getProfile().isOnboardingCompleted
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        val next = (_uiState.value.currentStep + 1).coerceAtMost(4)
        _uiState.value = _uiState.value.copy(currentStep = next)
    }

    fun previousStep() {
        val prev = (_uiState.value.currentStep - 1).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(currentStep = prev)
    }

    fun goToStep(step: Int) {
        val validStep = step.coerceIn(1, 4)
        _uiState.value = _uiState.value.copy(currentStep = validStep)
    }

    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName)
    }

    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName)
    }

    fun updateGender(gender: Gender) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }

    fun updateAge(age: Int) {
        _uiState.value = _uiState.value.copy(age = age.coerceIn(10, 120))
    }

    fun updateAvatar(presetId: String, customPhotoUri: String? = null) {
        _uiState.value = _uiState.value.copy(
            selectedAvatarPresetId = presetId,
            customPhotoUri = customPhotoUri
        )
    }

    fun updateWeight(weightKg: Float) {
        _uiState.value = _uiState.value.copy(weightKg = weightKg.coerceIn(30f, 300f))
    }

    fun updateBodyFat(fatPercentage: Float) {
        _uiState.value = _uiState.value.copy(bodyFatPercentage = fatPercentage.coerceIn(3f, 60f))
    }

    fun updateVisceralFat(visceralFat: Float) {
        _uiState.value = _uiState.value.copy(visceralFat = visceralFat.coerceIn(1f, 30f))
    }

    fun updateBoneMass(boneMassKg: Float) {
        _uiState.value = _uiState.value.copy(boneMassKg = boneMassKg.coerceIn(1f, 10f))
    }

    fun updateMuscleMass(muscleMassKg: Float) {
        _uiState.value = _uiState.value.copy(muscleMassKg = muscleMassKg.coerceIn(10f, 150f))
    }

    fun toggleDevicePairing(brandName: String) {
        val current = _uiState.value.pairedBrands.toMutableSet()
        val isNowPaired = if (current.contains(brandName)) {
            current.remove(brandName)
            false
        } else {
            current.add(brandName)
            true
        }
        val msg = if (isNowPaired) "Dispositivo $brandName vinculado con éxito" else "Dispositivo $brandName desvinculado"
        _uiState.value = _uiState.value.copy(
            pairedBrands = current,
            snackbarMessage = msg
        )
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val fullName = state.firstName.ifBlank { "Atleta PersonalSport" }

        val currentProfile = userProfileRepository.getProfile()
        val updatedProfile = currentProfile.copy(
            fullName = fullName,
            lastName = state.lastName,
            gender = state.gender,
            avatarPresetId = state.selectedAvatarPresetId,
            customPhotoUri = state.customPhotoUri,
            age = state.age,
            weightKg = state.weightKg,
            bodyFatPercentage = state.bodyFatPercentage,
            visceralFat = state.visceralFat,
            boneMassKg = state.boneMassKg,
            muscleMassKg = state.muscleMassKg,
            isOnboardingCompleted = true
        )

        userProfileRepository.updateProfile(updatedProfile)

        bodyMetricRepository?.let { repo ->
            viewModelScope.launch {
                repo.insert(
                    BodyMetric(
                        date = System.currentTimeMillis(),
                        weightKg = state.weightKg,
                        bodyFatPercentage = state.bodyFatPercentage
                    )
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            isAiTransferred = true,
            isOnboardingCompleted = true,
            snackbarMessage = "🎉 ¡Onboarding completado! Parámetros biológicos transferidos a Qwen IA."
        )
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    class Factory(
        private val userProfileRepository: UserProfileRepository,
        private val bodyMetricRepository: BodyMetricRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(userProfileRepository, bodyMetricRepository) as T
        }
    }
}
