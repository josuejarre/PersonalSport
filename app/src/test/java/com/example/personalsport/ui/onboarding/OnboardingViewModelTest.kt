package com.example.personalsport.ui.onboarding

import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.repository.UserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        userProfileRepository = UserProfileRepository(null)
        viewModel = OnboardingViewModel(userProfileRepository, null)
    }

    @Test
    fun testInitialStateFromRepository() {
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(1, state.currentStep)
        assertFalse(state.isOnboardingCompleted)
        assertFalse(state.isAiTransferred)
        assertTrue(state.pairedBrands.isEmpty())
    }

    @Test
    fun testStepNavigationBounds() {
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // Upper bound check (cannot exceed 4)
        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // Lower bound check
        viewModel.previousStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.goToStep(1)
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(1, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testUserRegistrationFieldUpdates() {
        viewModel.updateFirstName("Carlos")
        viewModel.updateLastName("Alcaraz")
        viewModel.updateGender(Gender.MASCULINO)
        viewModel.updateAge(21)
        viewModel.updateAvatar("avatar_runner")

        val state = viewModel.uiState.value
        assertEquals("Carlos", state.firstName)
        assertEquals("Alcaraz", state.lastName)
        assertEquals(Gender.MASCULINO, state.gender)
        assertEquals(21, state.age)
        assertEquals("avatar_runner", state.selectedAvatarPresetId)
    }

    @Test
    fun testDevicePairingToggles() {
        assertTrue(viewModel.uiState.value.pairedBrands.isEmpty())

        viewModel.toggleDevicePairing("Huawei Watch GT")
        assertTrue(viewModel.uiState.value.pairedBrands.contains("Huawei Watch GT"))

        viewModel.toggleDevicePairing("Tanita")
        assertTrue(viewModel.uiState.value.pairedBrands.contains("Tanita"))
        assertEquals(2, viewModel.uiState.value.pairedBrands.size)

        // Untoggle device
        viewModel.toggleDevicePairing("Huawei Watch GT")
        assertFalse(viewModel.uiState.value.pairedBrands.contains("Huawei Watch GT"))
        assertEquals(1, viewModel.uiState.value.pairedBrands.size)
    }

    @Test
    fun testBiologicalParameterUpdates() {
        viewModel.updateWeight(78.5f)
        viewModel.updateBodyFat(14.2f)
        viewModel.updateVisceralFat(4.0f)
        viewModel.updateBoneMass(3.4f)
        viewModel.updateMuscleMass(38.0f)

        val state = viewModel.uiState.value
        assertEquals(78.5f, state.weightKg, 0.01f)
        assertEquals(14.2f, state.bodyFatPercentage, 0.01f)
        assertEquals(4.0f, state.visceralFat, 0.01f)
        assertEquals(3.4f, state.boneMassKg, 0.01f)
        assertEquals(38.0f, state.muscleMassKg, 0.01f)
    }

    @Test
    fun testCompleteOnboarding() {
        viewModel.updateFirstName("Rafael")
        viewModel.updateLastName("Nadal")
        viewModel.updateGender(Gender.MASCULINO)
        viewModel.updateAge(38)
        viewModel.updateWeight(85.0f)
        viewModel.updateBodyFat(10.5f)

        viewModel.completeOnboarding()

        val state = viewModel.uiState.value
        assertTrue(state.isOnboardingCompleted)
        assertTrue(state.isAiTransferred)
        assertNotNull(state.snackbarMessage)

        // Verify persistence in repository
        val profileInRepo = userProfileRepository.getProfile()
        assertTrue(profileInRepo.isOnboardingCompleted)
        assertEquals("Rafael", profileInRepo.fullName)
        assertEquals("Nadal", profileInRepo.lastName)
        assertEquals(38, profileInRepo.age)
        assertEquals(85.0f, profileInRepo.weightKg, 0.01f)
        assertEquals(10.5f, profileInRepo.bodyFatPercentage, 0.01f)
    }
}
