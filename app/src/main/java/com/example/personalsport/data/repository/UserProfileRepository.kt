package com.example.personalsport.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PaymentMethodType
import com.example.personalsport.data.model.PresetAvatars
import com.example.personalsport.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class UserProfileRepository(context: Context? = null) {

    private val prefs: SharedPreferences? = try {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val _userProfile = MutableStateFlow(loadProfileFromPrefs())
    open val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    open fun getProfile(): UserProfile {
        return _userProfile.value
    }

    open fun updateProfile(profile: UserProfile) {
        saveProfileToPrefs(profile)
        _userProfile.value = profile
    }

    open fun updateName(name: String) {
        val updated = _userProfile.value.copy(fullName = name)
        updateProfile(updated)
    }

    open fun updateLastName(lastName: String) {
        val updated = _userProfile.value.copy(lastName = lastName)
        updateProfile(updated)
    }

    open fun updateGender(gender: Gender) {
        val updated = _userProfile.value.copy(gender = gender)
        updateProfile(updated)
    }

    open fun updateAge(age: Int) {
        val updated = _userProfile.value.copy(age = age)
        updateProfile(updated)
    }

    open fun updateAvatar(presetId: String, customPhotoUri: String?) {
        val updated = _userProfile.value.copy(
            avatarPresetId = presetId,
            customPhotoUri = customPhotoUri
        )
        updateProfile(updated)
    }

    open fun updatePaymentMethod(type: PaymentMethodType, last4: String = "", email: String = "") {
        val updated = _userProfile.value.copy(
            paymentMethodType = type,
            paymentCardLast4 = last4,
            paymentEmail = email
        )
        updateProfile(updated)
    }

    open fun updateBiologicalMetrics(
        weightKg: Float,
        bodyFatPercentage: Float,
        visceralFat: Float,
        boneMassKg: Float,
        muscleMassKg: Float
    ) {
        val updated = _userProfile.value.copy(
            weightKg = weightKg,
            bodyFatPercentage = bodyFatPercentage,
            visceralFat = visceralFat,
            boneMassKg = boneMassKg,
            muscleMassKg = muscleMassKg
        )
        updateProfile(updated)
    }

    open fun setOnboardingCompleted(completed: Boolean) {
        val updated = _userProfile.value.copy(isOnboardingCompleted = completed)
        updateProfile(updated)
    }

    private fun loadProfileFromPrefs(): UserProfile {
        val prefs = this.prefs ?: return UserProfile()

        val fullName = prefs.getString(KEY_FULL_NAME, "Atleta PersonalSport") ?: "Atleta PersonalSport"
        val lastName = prefs.getString(KEY_LAST_NAME, "") ?: ""
        
        val genderStr = prefs.getString(KEY_GENDER, Gender.MASCULINO.name) ?: Gender.MASCULINO.name
        val gender = try {
            Gender.valueOf(genderStr)
        } catch (_: Exception) {
            Gender.MASCULINO
        }

        val avatarPresetId = prefs.getString(KEY_AVATAR_PRESET, PresetAvatars.DEFAULT_AVATAR_ID) ?: PresetAvatars.DEFAULT_AVATAR_ID
        val customPhotoUri = prefs.getString(KEY_CUSTOM_PHOTO_URI, null)

        val paymentTypeStr = prefs.getString(KEY_PAYMENT_TYPE, PaymentMethodType.SIN_REGISTRAR.name) ?: PaymentMethodType.SIN_REGISTRAR.name
        val paymentType = try {
            PaymentMethodType.valueOf(paymentTypeStr)
        } catch (_: Exception) {
            PaymentMethodType.SIN_REGISTRAR
        }

        val paymentLast4 = prefs.getString(KEY_PAYMENT_LAST4, "") ?: ""
        val paymentEmail = prefs.getString(KEY_PAYMENT_EMAIL, "") ?: ""
        val subscriptionStatus = prefs.getString(KEY_SUBSCRIPTION, "Gratuito") ?: "Gratuito"
        val fitnessLevel = prefs.getString(KEY_FITNESS_LEVEL, "Atleta") ?: "Atleta"

        val age = prefs.getInt(KEY_AGE, 28)
        val weightKg = prefs.getFloat(KEY_WEIGHT, 70.0f)
        val bodyFatPercentage = prefs.getFloat(KEY_BODY_FAT, 15.0f)
        val visceralFat = prefs.getFloat(KEY_VISCERAL_FAT, 5.0f)
        val boneMassKg = prefs.getFloat(KEY_BONE_MASS, 3.2f)
        val muscleMassKg = prefs.getFloat(KEY_MUSCLE_MASS, 32.5f)
        val isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        return UserProfile(
            fullName = fullName,
            lastName = lastName,
            gender = gender,
            avatarPresetId = avatarPresetId,
            customPhotoUri = customPhotoUri,
            paymentMethodType = paymentType,
            paymentCardLast4 = paymentLast4,
            paymentEmail = paymentEmail,
            subscriptionStatus = subscriptionStatus,
            fitnessLevel = fitnessLevel,
            age = age,
            weightKg = weightKg,
            bodyFatPercentage = bodyFatPercentage,
            visceralFat = visceralFat,
            boneMassKg = boneMassKg,
            muscleMassKg = muscleMassKg,
            isOnboardingCompleted = isOnboardingCompleted
        )
    }

    private fun saveProfileToPrefs(profile: UserProfile) {
        prefs?.edit {
            putString(KEY_FULL_NAME, profile.fullName)
            putString(KEY_LAST_NAME, profile.lastName)
            putString(KEY_GENDER, profile.gender.name)
            putString(KEY_AVATAR_PRESET, profile.avatarPresetId)
            putString(KEY_CUSTOM_PHOTO_URI, profile.customPhotoUri)
            putString(KEY_PAYMENT_TYPE, profile.paymentMethodType.name)
            putString(KEY_PAYMENT_LAST4, profile.paymentCardLast4)
            putString(KEY_PAYMENT_EMAIL, profile.paymentEmail)
            putString(KEY_SUBSCRIPTION, profile.subscriptionStatus)
            putString(KEY_FITNESS_LEVEL, profile.fitnessLevel)
            putInt(KEY_AGE, profile.age)
            putFloat(KEY_WEIGHT, profile.weightKg)
            putFloat(KEY_BODY_FAT, profile.bodyFatPercentage)
            putFloat(KEY_VISCERAL_FAT, profile.visceralFat)
            putFloat(KEY_BONE_MASS, profile.boneMassKg)
            putFloat(KEY_MUSCLE_MASS, profile.muscleMassKg)
            putBoolean(KEY_ONBOARDING_COMPLETED, profile.isOnboardingCompleted)
        }
    }

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_GENDER = "gender"
        private const val KEY_AVATAR_PRESET = "avatar_preset"
        private const val KEY_CUSTOM_PHOTO_URI = "custom_photo_uri"
        private const val KEY_PAYMENT_TYPE = "payment_type"
        private const val KEY_PAYMENT_LAST4 = "payment_last4"
        private const val KEY_PAYMENT_EMAIL = "payment_email"
        private const val KEY_SUBSCRIPTION = "subscription_status"
        private const val KEY_FITNESS_LEVEL = "fitness_level"
        private const val KEY_AGE = "age"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_BODY_FAT = "body_fat_pct"
        private const val KEY_VISCERAL_FAT = "visceral_fat"
        private const val KEY_BONE_MASS = "bone_mass_kg"
        private const val KEY_MUSCLE_MASS = "muscle_mass_kg"
        private const val KEY_ONBOARDING_COMPLETED = "is_onboarding_completed"
    }
}
