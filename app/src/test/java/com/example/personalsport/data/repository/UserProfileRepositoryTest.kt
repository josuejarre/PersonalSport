package com.example.personalsport.data.repository

import com.example.personalsport.data.model.Gender
import com.example.personalsport.data.model.PaymentMethodType
import com.example.personalsport.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class UserProfileRepositoryTest {

    private lateinit var repository: UserProfileRepository

    @Before
    fun setUp() {
        repository = UserProfileRepository(null)
    }

    @Test
    fun testDefaultProfileValues() {
        val profile = repository.getProfile()
        assertNotNull(profile)
        assertEquals("Atleta PersonalSport", profile.fullName)
        assertEquals(Gender.MASCULINO, profile.gender)
        assertEquals(PaymentMethodType.SIN_REGISTRAR, profile.paymentMethodType)
        assertEquals("Sin registrar", profile.paymentMethodFormatted)
    }

    @Test
    fun testUpdateProfileInMemory() {
        val updated = UserProfile(
            fullName = "Sofia Vergara",
            gender = Gender.FEMENINO,
            avatarPresetId = "avatar_yoga",
            customPhotoUri = null,
            paymentMethodType = PaymentMethodType.PAYPAL,
            paymentEmail = "sofia@paypal.com"
        )

        repository.updateProfile(updated)

        val loaded = repository.getProfile()
        assertEquals("Sofia Vergara", loaded.fullName)
        assertEquals(Gender.FEMENINO, loaded.gender)
        assertEquals("avatar_yoga", loaded.avatarPresetId)
        assertEquals(PaymentMethodType.PAYPAL, loaded.paymentMethodType)
        assertEquals("PayPal (sofia@paypal.com)", loaded.paymentMethodFormatted)
    }

    @Test
    fun testIndividualFieldUpdates() {
        repository.updateName("Diego Maradona")
        assertEquals("Diego Maradona", repository.getProfile().fullName)

        repository.updateGender(Gender.MASCULINO)
        assertEquals(Gender.MASCULINO, repository.getProfile().gender)

        repository.updatePaymentMethod(PaymentMethodType.MASTERCARD, last4 = "1122")
        assertEquals(PaymentMethodType.MASTERCARD, repository.getProfile().paymentMethodType)
        assertEquals("Mastercard **** 1122", repository.getProfile().paymentMethodFormatted)
    }
}
