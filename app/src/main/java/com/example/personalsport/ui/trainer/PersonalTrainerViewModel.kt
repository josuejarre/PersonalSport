package com.example.personalsport.ui.trainer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalsport.trainer.AppointmentBooking
import com.example.personalsport.trainer.PersonalTrainer
import com.example.personalsport.trainer.Specialty
import com.example.personalsport.trainer.SubscriptionPlan
import com.example.personalsport.trainer.SubscriptionPlanType
import com.example.personalsport.trainer.TrainerMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Immutable
data class PersonalTrainerUiState(
    val trainers: List<PersonalTrainer> = emptyList(),
    val filteredTrainers: List<PersonalTrainer> = emptyList(),
    val selectedSpecialty: Specialty? = null,
    val searchQuery: String = "",
    val selectedTrainer: PersonalTrainer? = null,
    val selectedPlan: SubscriptionPlan? = null,
    val deepLinkUrl: String? = null,
    val isBookingDialogOpen: Boolean = false,
    val bookingSuccessMessage: String? = null,
    val bookingsHistory: List<AppointmentBooking> = emptyList(),
    val isMessagingDialogOpen: Boolean = false,
    val messages: List<TrainerMessage> = emptyList(),
    val messageInputText: String = ""
)

class PersonalTrainerViewModel(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalTrainerUiState())
    val uiState: StateFlow<PersonalTrainerUiState> = _uiState.asStateFlow()

    private val sampleTrainers = listOf(
        PersonalTrainer(
            id = "trainer-01",
            name = "Carlos Benítez",
            title = "Entrenador Nacional de Atletismo & Fondo",
            specialties = listOf(Specialty.ATHLETICS, Specialty.NUTRITION),
            rating = 4.9f,
            reviewCount = 128,
            hourlyRate = 40.0,
            experienceYears = 10,
            location = "Madrid / Presencial & Online",
            bio = "Ex-atleta profesional de 1500m y 5000m. Graduado en Ciencias de la Actividad Física y del Deporte (CCAFYD). Especialista en periodización táctica para corredores de media y larga distancia.",
            plans = listOf(
                SubscriptionPlan(
                    planType = SubscriptionPlanType.SINGLE_SESSION,
                    planId = "single-session",
                    price = 40.0,
                    features = listOf("1h de evaluación biomecánica", "Grabación y análisis de zancada en pista", "Informe de corrección de técnica")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.MONTHLY_COACHING,
                    planId = "monthly-coaching",
                    price = 90.0,
                    features = listOf("Planificación semanal personalizada", "Feedback 24/7 por chat", "Ajuste de ritmos por lactato/FC")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.QUARTERLY_TRANSFORMATION,
                    planId = "quarterly-transform",
                    price = 240.0,
                    features = listOf("3 meses de coaching integral", "Pauta de suplementación para carrera", "Llamadas quincenales 1-a-1")
                )
            )
        ),
        PersonalTrainer(
            id = "trainer-02",
            name = "Elena Morales",
            title = "IFBB Pro Coach Hipertrofia & Fuerza",
            specialties = listOf(Specialty.BODYBUILDING, Specialty.NUTRITION),
            rating = 4.95f,
            reviewCount = 210,
            hourlyRate = 45.0,
            experienceYears = 8,
            location = "Barcelona / Presencial & Online",
            bio = "Especialista en desarrollo de masa muscular y acondicionamiento estético. Amplia experiencia en programación RPE, biomecánica aplicada a máquinas y estrategias de recomposición corporal.",
            plans = listOf(
                SubscriptionPlan(
                    planType = SubscriptionPlanType.SINGLE_SESSION,
                    planId = "single-session",
                    price = 45.0,
                    features = listOf("1h de entrenamiento guiado en gimnasio", "Corrección de técnica en básicos (Press/Sentadilla/Peso Muerto)", "Cálculo de macros")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.MONTHLY_COACHING,
                    planId = "monthly-coaching",
                    price = 105.0,
                    features = listOf("Rutina de hipertrofia personalizada", "Pauta nutricional flexible IIFYM", "Revisión en vídeo de levantamientos")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.QUARTERLY_TRANSFORMATION,
                    planId = "quarterly-transform",
                    price = 280.0,
                    features = listOf("Transformación física 12 semanas", "Ajustes semanales de cargas y macros", "Soporte WhatsApp diario direct")
                )
            )
        ),
        PersonalTrainer(
            id = "trainer-03",
            name = "Marc Santamaria",
            title = "Entrenador de Natación & Triatlón FINA",
            specialties = listOf(Specialty.SWIMMING, Specialty.ATHLETICS),
            rating = 4.85f,
            reviewCount = 95,
            hourlyRate = 38.0,
            experienceYears = 7,
            location = "Valencia / Online & Piscina",
            bio = "Entrenador superior de natación y preparador físico de triatletas de aguas abiertas. Centrado en la optimización del hidrodinamismo, virajes y economización del esfuerzo.",
            plans = listOf(
                SubscriptionPlan(
                    planType = SubscriptionPlanType.SINGLE_SESSION,
                    planId = "single-session",
                    price = 38.0,
                    features = listOf("1h de clase técnica en piscina", "Análisis subacuático en vídeo de cromo/craw", "Ejercicios de respiración y rolido")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.MONTHLY_COACHING,
                    planId = "monthly-coaching",
                    price = 85.0,
                    features = listOf("Sesiones semanales de natación y cardio", "Workouts para reloj Garmin/Apple", "Estrategia para aguas abiertas")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.QUARTERLY_TRANSFORMATION,
                    planId = "quarterly-transform",
                    price = 220.0,
                    features = listOf("Preparación de triatlón/travesía 3 meses", "Plan de ritmo por SWOLF", "Coaching táctico pre-competición")
                )
            )
        ),
        PersonalTrainer(
            id = "trainer-04",
            name = "Dra. Lucía Vega",
            title = "Nutricionista Deportiva & Coach Funcional",
            specialties = listOf(Specialty.NUTRITION, Specialty.FUNCTIONAL),
            rating = 5.0f,
            reviewCount = 340,
            hourlyRate = 50.0,
            experienceYears = 12,
            location = "Online Global",
            bio = "Doctora en Nutrición y Ciencias de la Salud. Especializada en optimización metabólica, nutrición clínica deportiva y rutinas de entrenamiento funcional de alta intensidad.",
            plans = listOf(
                SubscriptionPlan(
                    planType = SubscriptionPlanType.SINGLE_SESSION,
                    planId = "single-session",
                    price = 50.0,
                    features = listOf("Consulta de valoración nutricional 1h", "Análisis de bioimpedancia/Antropometría ISAK", "Menú semanal personalizado")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.MONTHLY_COACHING,
                    planId = "monthly-coaching",
                    price = 120.0,
                    features = listOf("Plan nutricional evolutivo", "Planificación de entrenamiento funcional", "Consultas ilimitadas en la app")
                ),
                SubscriptionPlan(
                    planType = SubscriptionPlanType.QUARTERLY_TRANSFORMATION,
                    planId = "quarterly-transform",
                    price = 320.0,
                    features = listOf("Programa 360° Recomposición y Salud", "Suplementación clínica avanzada", "Llamadas de revisión semanales")
                )
            )
        )
    )

    init {
        _uiState.update {
            it.copy(
                trainers = sampleTrainers,
                filteredTrainers = sampleTrainers,
                selectedTrainer = sampleTrainers.first(),
                selectedPlan = sampleTrainers.first().plans.first(),
                deepLinkUrl = generateDeepLink(sampleTrainers.first().id, sampleTrainers.first().plans.first().planId)
            )
        }
    }

    fun selectSpecialtyFilter(specialty: Specialty?) {
        _uiState.update { it.copy(selectedSpecialty = specialty) }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun selectTrainer(trainer: PersonalTrainer) {
        val defaultPlan = trainer.plans.firstOrNull()
        val link = defaultPlan?.let { generateDeepLink(trainer.id, it.planId) }

        val initialMessages = listOf(
            TrainerMessage(
                id = "m1",
                sender = "trainer",
                content = "¡Hola! Soy ${trainer.name}. He visto tus objetivos en PersonalSport. ¿En qué prueba o meta deportiva estás enfocado ahora mismo?",
                timestamp = System.currentTimeMillis() - 3600000
            )
        )

        _uiState.update {
            it.copy(
                selectedTrainer = trainer,
                selectedPlan = defaultPlan,
                deepLinkUrl = link,
                messages = initialMessages
            )
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        val trainer = _uiState.value.selectedTrainer
        val link = trainer?.let { generateDeepLink(it.id, plan.planId) }
        _uiState.update {
            it.copy(
                selectedPlan = plan,
                deepLinkUrl = link
            )
        }
    }

    fun generateDeepLink(trainerId: String, planId: String): String {
        return "https://personalsport.app/trainers/$trainerId/hire?plan=$planId"
    }

    fun openBookingDialog() {
        _uiState.update { it.copy(isBookingDialogOpen = true) }
    }

    fun closeBookingDialog() {
        _uiState.update { it.copy(isBookingDialogOpen = false) }
    }

    fun confirmAppointmentBooking(date: String, timeSlot: String) {
        val trainer = _uiState.value.selectedTrainer ?: return
        val plan = _uiState.value.selectedPlan ?: return

        val newBooking = AppointmentBooking(
            id = UUID.randomUUID().toString(),
            trainerId = trainer.id,
            trainerName = trainer.name,
            date = date,
            timeSlot = timeSlot,
            planTitle = plan.title,
            price = plan.price
        )

        _uiState.update { currentState ->
            currentState.copy(
                isBookingDialogOpen = false,
                bookingsHistory = currentState.bookingsHistory + newBooking,
                bookingSuccessMessage = "¡Cita reservada con ${trainer.name} para el $date a las $timeSlot!"
            )
        }
    }

    fun clearBookingSuccessMessage() {
        _uiState.update { it.copy(bookingSuccessMessage = null) }
    }

    fun openMessagingDialog() {
        _uiState.update { it.copy(isMessagingDialogOpen = true) }
    }

    fun closeMessagingDialog() {
        _uiState.update { it.copy(isMessagingDialogOpen = false) }
    }

    fun setMessageInputText(text: String) {
        _uiState.update { it.copy(messageInputText = text) }
    }

    fun sendUserMessage() {
        val text = _uiState.value.messageInputText.trim()
        if (text.isEmpty()) return

        val userMessage = TrainerMessage(
            id = UUID.randomUUID().toString(),
            sender = "user",
            content = text,
            timestamp = System.currentTimeMillis()
        )

        val autoReply = TrainerMessage(
            id = UUID.randomUUID().toString(),
            sender = "trainer",
            content = "¡Recibido! Revisaré tu mensaje y te responderé lo antes posible. Si deseas agendar una sesión directa, puedes hacerlo mediante la app o el enlace web.",
            timestamp = System.currentTimeMillis() + 1000
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage + autoReply,
                messageInputText = ""
            )
        }
    }

    private fun applyFilters() {
        viewModelScope.launch(defaultDispatcher) {
            val q = _uiState.value.searchQuery.trim().lowercase(Locale.ROOT)
            val specialty = _uiState.value.selectedSpecialty

            val filtered = sampleTrainers.filter { trainer ->
                val matchesQuery = q.isEmpty() ||
                        trainer.name.lowercase(Locale.ROOT).contains(q) ||
                        trainer.title.lowercase(Locale.ROOT).contains(q) ||
                        trainer.bio.lowercase(Locale.ROOT).contains(q)

                val matchesSpecialty = specialty == null || trainer.specialties.contains(specialty)

                matchesQuery && matchesSpecialty
            }

            _uiState.update {
                it.copy(filteredTrainers = filtered)
            }
        }
    }
}
