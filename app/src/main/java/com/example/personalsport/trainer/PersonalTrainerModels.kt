package com.example.personalsport.trainer

enum class Specialty(val displayName: String) {
    ATHLETICS("Atletismo & Fondo"),
    BODYBUILDING("Musculación & Hipertrofia"),
    SWIMMING("Natación & Triatlón"),
    NUTRITION("Nutrición Deportiva"),
    FUNCTIONAL("Entrenamiento Funcional")
}

enum class SubscriptionPlanType(
    val title: String,
    val billingPeriod: String,
    val description: String
) {
    SINGLE_SESSION(
        title = "Sesión Individual",
        billingPeriod = "1 sesión puntual",
        description = "Evaluación biomecánica, análisis de técnica de carrera/natación o sesión de entrenamiento personal presencial/online."
    ),
    MONTHLY_COACHING(
        title = "Plan Mensual",
        billingPeriod = "al mes",
        description = "Planificación semanal 100% adaptativa, ajustes continuos según métricas de rendimiento y soporte por chat."
    ),
    QUARTERLY_TRANSFORMATION(
        title = "Transformación Trimestral",
        billingPeriod = "por 3 meses",
        description = "Acompañamiento integral 360°: rutina de entrenamiento, plan nutricional personalizado, análisis en vídeo y llamadas de seguimiento quincenales."
    )
}

data class SubscriptionPlan(
    val planType: SubscriptionPlanType,
    val planId: String,
    val title: String = planType.title,
    val price: Double,
    val billingPeriod: String = planType.billingPeriod,
    val description: String = planType.description,
    val features: List<String>
)

data class PersonalTrainer(
    val id: String,
    val name: String,
    val title: String,
    val specialties: List<Specialty>,
    val rating: Float,
    val reviewCount: Int,
    val hourlyRate: Double,
    val experienceYears: Int,
    val location: String,
    val bio: String,
    val plans: List<SubscriptionPlan>,
    val availableTimeSlots: List<String> = listOf("09:00", "11:30", "16:00", "18:30", "20:00")
)

data class AppointmentBooking(
    val id: String,
    val trainerId: String,
    val trainerName: String,
    val date: String,
    val timeSlot: String,
    val planTitle: String,
    val price: Double
)

data class TrainerMessage(
    val id: String,
    val sender: String, // "user" or "trainer"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
