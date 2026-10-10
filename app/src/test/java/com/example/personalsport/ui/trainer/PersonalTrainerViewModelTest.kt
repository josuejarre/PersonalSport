package com.example.personalsport.ui.trainer

import com.example.personalsport.trainer.Specialty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalTrainerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PersonalTrainerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PersonalTrainerViewModel(defaultDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue("Initial trainers directory should not be empty", state.trainers.isNotEmpty())
        assertEquals(state.trainers.size, state.filteredTrainers.size)

        assertNotNull("Initial selected trainer should not be null", state.selectedTrainer)
        assertNotNull("Initial selected plan should not be null", state.selectedPlan)
        assertNotNull("Deep link URL should not be null", state.deepLinkUrl)
        assertTrue("Deep link should follow web app format", state.deepLinkUrl!!.startsWith("https://personalsport.app/trainers/"))
    }

    @Test
    fun testSpecialtyFiltering() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.selectSpecialtyFilter(Specialty.ATHLETICS)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Specialty.ATHLETICS, state.selectedSpecialty)
        assertTrue("Filtered trainers should contain Athletics coaches", state.filteredTrainers.isNotEmpty())
        assertTrue("All filtered trainers must have Athletics specialty", state.filteredTrainers.all { trainer ->
            trainer.specialties.contains(Specialty.ATHLETICS)
        })
    }

    @Test
    fun testSearchQueryFiltering() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSearchQuery("Carlos")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Carlos", state.searchQuery)
        assertTrue("Search results should contain Carlos", state.filteredTrainers.any { it.name.contains("Carlos") })
    }

    @Test
    fun testTrainerAndPlanSelectionAndDeepLink() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val trainer = viewModel.uiState.value.trainers.last()
        viewModel.selectTrainer(trainer)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(trainer.id, state.selectedTrainer?.id)

        val plan = trainer.plans.last()
        viewModel.selectPlan(plan)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(plan.planId, state.selectedPlan?.planId)

        val expectedLink = "https://personalsport.app/trainers/${trainer.id}/hire?plan=${plan.planId}"
        assertEquals(expectedLink, state.deepLinkUrl)
    }

    @Test
    fun testAppointmentBookingFlow() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openBookingDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Booking dialog should be open", viewModel.uiState.value.isBookingDialogOpen)

        viewModel.confirmAppointmentBooking("Mañana, 15 de Octubre", "16:00")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.bookingsHistory.size)
        assertEquals("Mañana, 15 de Octubre", state.bookingsHistory.first().date)
        assertEquals("16:00", state.bookingsHistory.first().timeSlot)
        assertNotNull("Success message should be set", state.bookingSuccessMessage)
    }

    @Test
    fun testTrainerMessaging() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val trainer = viewModel.uiState.value.trainers.first()
        viewModel.selectTrainer(trainer)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openMessagingDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Messaging dialog should be open", viewModel.uiState.value.isMessagingDialogOpen)

        viewModel.setMessageInputText("Hola, ¿cuál es tu disponibilidad para comenzar?")
        viewModel.sendUserMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Messages list should contain initial message, user message and trainer reply", state.messages.size >= 3)
        val lastUserMessage = state.messages.find { it.sender == "user" && it.content.contains("disponibilidad") }
        assertNotNull("User message should be stored", lastUserMessage)
    }
}
