package com.example.personalsport.ui.profile

import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PaymentMethodType
import com.example.personalsport.data.model.UserProfile
import com.example.personalsport.data.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeUserProfileRepository : UserProfileRepository(null) {
        private val _profile = MutableStateFlow(UserProfile())
        override val userProfile: StateFlow<UserProfile> = _profile.asStateFlow()

        override fun getProfile(): UserProfile = _profile.value

        override fun updateProfile(profile: UserProfile) {
            _profile.value = profile
        }
    }

    private lateinit var fakeRepository: FakeUserProfileRepository
    private lateinit var viewModel: UserProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserProfileRepository()
        viewModel = UserProfileViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateMatchesDefaultProfile() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals("Atleta PersonalSport", state.profile.fullName)
        assertEquals(Gender.MASCULINO, state.profile.gender)
        assertEquals(PaymentMethodType.SIN_REGISTRAR, state.profile.paymentMethodType)
        assertFalse("Should start in view mode (isEditing = false)", state.isEditing)
    }

    @Test
    fun testStartEditingLoadsCurrentProfileValues() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val customProfile = UserProfile(
            fullName = "María Rodríguez",
            gender = Gender.FEMENINO,
            avatarPresetId = "avatar_runner",
            paymentMethodType = PaymentMethodType.MASTERCARD,
            paymentCardLast4 = "8888"
        )
        fakeRepository.updateProfile(customProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startEditing()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("isEditing should be true after startEditing", state.isEditing)
        assertEquals("María Rodríguez", state.draftName)
        assertEquals(Gender.FEMENINO, state.draftGender)
        assertEquals("avatar_runner", state.draftAvatarPresetId)
        assertEquals(PaymentMethodType.MASTERCARD, state.draftPaymentType)
        assertEquals("8888", state.draftPaymentLast4)
    }

    @Test
    fun testEditingProfileFlowAndSaving() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.startEditing()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNameChanged("Carlos Mendoza")
        viewModel.onGenderSelected(Gender.MASCULINO)
        viewModel.onAvatarPresetSelected("avatar_gym")
        viewModel.onPaymentTypeSelected(PaymentMethodType.VISA)
        viewModel.onPaymentLast4Changed("9911")

        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isEditing should be false after save", state.isEditing)
        assertEquals("Carlos Mendoza", state.profile.fullName)
        assertEquals(Gender.MASCULINO, state.profile.gender)
        assertEquals("avatar_gym", state.profile.avatarPresetId)
        assertEquals(PaymentMethodType.VISA, state.profile.paymentMethodType)
        assertEquals("9911", state.profile.paymentCardLast4)
        assertNotNull("Snackbar message should confirm save", state.snackbarMessage)
    }

    @Test
    fun testEmptyNameValidation() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.startEditing()
        viewModel.onNameChanged("   ")
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("isEditing should remain true when validation fails", state.isEditing)
        assertNotNull("Error message should be set for empty name", state.errorMessage)
    }

    @Test
    fun testCustomPhotoUriSelection() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.startEditing()
        viewModel.onCustomPhotoPicked("content://media/external/images/media/1234")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("content://media/external/images/media/1234", viewModel.uiState.value.draftCustomPhotoUri)

        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedProfile = fakeRepository.getProfile()
        assertEquals("content://media/external/images/media/1234", updatedProfile.customPhotoUri)
    }

    @Test
    fun testCancelEditingRevertsChanges() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val originalName = viewModel.uiState.value.profile.fullName

        viewModel.startEditing()
        viewModel.onNameChanged("Nombre Temporal Cancelado")
        viewModel.cancelEditing()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isEditing should be false after cancel", state.isEditing)
        assertEquals(originalName, state.profile.fullName)
    }
}
