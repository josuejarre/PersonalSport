package com.example.personalsport.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PaymentMethodType
import com.example.personalsport.data.model.UserProfile
import com.example.personalsport.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class UserProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isEditing: Boolean = false,
    val draftName: String = "",
    val draftGender: Gender = Gender.MASCULINO,
    val draftAvatarPresetId: String = "avatar_athlete",
    val draftCustomPhotoUri: String? = null,
    val draftPaymentType: PaymentMethodType = PaymentMethodType.SIN_REGISTRAR,
    val draftPaymentLast4: String = "",
    val draftPaymentEmail: String = "",
    val isPaymentEditorOpen: Boolean = false,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
)

class UserProfileViewModel(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _isEditing = MutableStateFlow(false)
    private val _draftName = MutableStateFlow("")
    private val _draftGender = MutableStateFlow(Gender.MASCULINO)
    private val _draftAvatarPresetId = MutableStateFlow("avatar_athlete")
    private val _draftCustomPhotoUri = MutableStateFlow<String?>(null)
    private val _draftPaymentType = MutableStateFlow(PaymentMethodType.SIN_REGISTRAR)
    private val _draftPaymentLast4 = MutableStateFlow("")
    private val _draftPaymentEmail = MutableStateFlow("")
    private val _isPaymentEditorOpen = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UserProfileUiState> = combine(
        repository.userProfile,
        _isEditing,
        _draftName,
        _draftGender,
        _draftAvatarPresetId,
        _draftCustomPhotoUri,
        _draftPaymentType,
        _draftPaymentLast4,
        _draftPaymentEmail,
        _isPaymentEditorOpen,
        _snackbarMessage,
        _errorMessage
    ) { args ->
        val profile = args[0] as UserProfile
        val isEditing = args[1] as Boolean
        val draftName = args[2] as String
        val draftGender = args[3] as Gender
        val draftAvatarPresetId = args[4] as String
        val draftCustomPhotoUri = args[5] as String?
        val draftPaymentType = args[6] as PaymentMethodType
        val draftPaymentLast4 = args[7] as String
        val draftPaymentEmail = args[8] as String
        val isPaymentEditorOpen = args[9] as Boolean
        val snackbarMessage = args[10] as String?
        val errorMessage = args[11] as String?

        UserProfileUiState(
            profile = profile,
            isEditing = isEditing,
            draftName = draftName,
            draftGender = draftGender,
            draftAvatarPresetId = draftAvatarPresetId,
            draftCustomPhotoUri = draftCustomPhotoUri,
            draftPaymentType = draftPaymentType,
            draftPaymentLast4 = draftPaymentLast4,
            draftPaymentEmail = draftPaymentEmail,
            isPaymentEditorOpen = isPaymentEditorOpen,
            snackbarMessage = snackbarMessage,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfileUiState(profile = repository.getProfile())
    )

    fun startEditing() {
        val current = repository.getProfile()
        _draftName.value = current.fullName
        _draftGender.value = current.gender
        _draftAvatarPresetId.value = current.avatarPresetId
        _draftCustomPhotoUri.value = current.customPhotoUri
        _draftPaymentType.value = current.paymentMethodType
        _draftPaymentLast4.value = current.paymentCardLast4
        _draftPaymentEmail.value = current.paymentEmail
        _errorMessage.value = null
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
        _isPaymentEditorOpen.value = false
        _errorMessage.value = null
    }

    fun onNameChanged(newName: String) {
        _draftName.value = newName
        if (newName.isNotBlank()) {
            _errorMessage.value = null
        }
    }

    fun onGenderSelected(gender: Gender) {
        _draftGender.value = gender
    }

    fun onAvatarPresetSelected(presetId: String) {
        _draftAvatarPresetId.value = presetId
        _draftCustomPhotoUri.value = null
    }

    fun onCustomPhotoPicked(uriString: String?) {
        if (!uriString.isNullOrBlank()) {
            _draftCustomPhotoUri.value = uriString
        } else {
            _draftCustomPhotoUri.value = null
        }
    }

    fun onPaymentTypeSelected(type: PaymentMethodType) {
        _draftPaymentType.value = type
    }

    fun onPaymentLast4Changed(last4: String) {
        val filtered = last4.filter { it.isDigit() }.take(4)
        _draftPaymentLast4.value = filtered
    }

    fun onPaymentEmailChanged(email: String) {
        _draftPaymentEmail.value = email
    }

    fun togglePaymentEditor(open: Boolean) {
        _isPaymentEditorOpen.value = open
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun saveProfile() {
        val name = _draftName.value.trim()
        if (name.isBlank()) {
            _errorMessage.value = "El nombre no puede estar vacío"
            return
        }

        val updatedProfile = UserProfile(
            fullName = name,
            gender = _draftGender.value,
            avatarPresetId = _draftAvatarPresetId.value,
            customPhotoUri = _draftCustomPhotoUri.value,
            paymentMethodType = _draftPaymentType.value,
            paymentCardLast4 = _draftPaymentLast4.value,
            paymentEmail = _draftPaymentEmail.value,
            subscriptionStatus = repository.getProfile().subscriptionStatus,
            fitnessLevel = repository.getProfile().fitnessLevel
        )

        repository.updateProfile(updatedProfile)
        _isEditing.value = false
        _isPaymentEditorOpen.value = false
        _errorMessage.value = null
        _snackbarMessage.value = "¡Perfil de $name actualizado!"
    }

    class Factory(private val repository: UserProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
                return UserProfileViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
