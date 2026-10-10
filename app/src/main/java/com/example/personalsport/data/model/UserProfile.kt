package com.example.personalsport.data.model

enum class Gender(val displayName: String) {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro / No especificado")
}

enum class PaymentMethodType(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    GOOGLE_PAY("Google Pay"),
    PAYPAL("PayPal"),
    SIN_REGISTRAR("Sin registrar")
}

data class AvatarOption(
    val id: String,
    val title: String,
    val description: String,
)

object PresetAvatars {
    const val DEFAULT_AVATAR_ID = "avatar_athlete"

    val OPTIONS = listOf(
        AvatarOption("avatar_athlete", "Atleta Fitness", "Perfil deportivo general"),
        AvatarOption("avatar_runner", "Corredor / Runner", "Especialidad atletismo y cardio"),
        AvatarOption("avatar_gym", "Power & Gym", "Entrenamiento de fuerza y pesas"),
        AvatarOption("avatar_yoga", "Yoga & Balance", "Mente, flexibilidad y bienestar"),
        AvatarOption("avatar_cyclist", "Ciclista Pro", "Ciclismo y resistencia"),
        AvatarOption("avatar_trophy", "Atleta Élite", "Competidor y alto rendimiento"),
        AvatarOption("avatar_star", "Estrella Sport", "Atleta destacado"),
        AvatarOption("avatar_gymnastics", "Gimnasia & Agilidad", "Calistenia y agilidad"),
    )
}

data class UserProfile(
    val fullName: String = "Atleta PersonalSport",
    val lastName: String = "",
    val gender: Gender = Gender.MASCULINO,
    val avatarPresetId: String = PresetAvatars.DEFAULT_AVATAR_ID,
    val customPhotoUri: String? = null,
    val paymentMethodType: PaymentMethodType = PaymentMethodType.SIN_REGISTRAR,
    val paymentCardLast4: String = "",
    val paymentEmail: String = "",
    val subscriptionStatus: String = "Gratuito",
    val fitnessLevel: String = "Atleta",
    val age: Int = 28,
    val weightKg: Float = 70.0f,
    val bodyFatPercentage: Float = 15.0f,
    val visceralFat: Float = 5.0f,
    val boneMassKg: Float = 3.2f,
    val muscleMassKg: Float = 32.5f,
    val isOnboardingCompleted: Boolean = false,
) {
    val paymentMethodFormatted: String
        get() = when (paymentMethodType) {
            PaymentMethodType.VISA -> "Visa **** $paymentCardLast4"
            PaymentMethodType.MASTERCARD -> "Mastercard **** $paymentCardLast4"
            PaymentMethodType.GOOGLE_PAY -> "Google Pay"
            PaymentMethodType.PAYPAL -> "PayPal (${paymentEmail.ifBlank { "Conectado" }})"
            PaymentMethodType.SIN_REGISTRAR -> "Sin registrar"
        }

    val genderFormatted: String
        get() = gender.displayName

    val completeNameFormatted: String
        get() = if (lastName.isNotBlank()) "$fullName $lastName" else fullName
}
