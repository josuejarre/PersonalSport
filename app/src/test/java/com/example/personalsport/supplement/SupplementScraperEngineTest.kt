package com.example.personalsport.supplement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplementScraperEngineTest {

    private val scraperEngine = SupplementScraperEngine

    @Test
    fun testAiRecommendationsForGoalMuscleGain() {
        val recs = scraperEngine.getAiRecommendedProductsForGoal(SupplementGoal.MUSCLE_GAIN)
        
        assertTrue("Should return AI recommended products for muscle gain", recs.isNotEmpty())
        val productNames = recs.map { it.name.lowercase() }
        assertTrue("Muscle gain stack should include Whey Protein", productNames.any { it.contains("whey") })
        assertTrue("Muscle gain stack should include Creatine", productNames.any { it.contains("creatina") })
    }

    @Test
    fun testAiRecommendationsForGoalFatLoss() {
        val recs = scraperEngine.getAiRecommendedProductsForGoal(SupplementGoal.FAT_LOSS)
        
        assertTrue("Should return AI recommended products for fat loss", recs.isNotEmpty())
        val productNames = recs.map { it.name.lowercase() }
        assertTrue("Fat loss stack should include L-Carnitine", productNames.any { it.contains("carnitine") || it.contains("carnitina") })
        assertTrue("Fat loss stack should include Green Tea", productNames.any { it.contains("té verde") || it.contains("green tea") })
    }

    @Test
    fun testAiRecommendationsForGoalEndurance() {
        val recs = scraperEngine.getAiRecommendedProductsForGoal(SupplementGoal.ENDURANCE)
        
        assertTrue("Should return AI recommended products for endurance", recs.isNotEmpty())
        val productNames = recs.map { it.name.lowercase() }
        assertTrue("Endurance stack should include Electrolytes", productNames.any { it.contains("electrolito") })
        assertTrue("Endurance stack should include Beta-Alanine", productNames.any { it.contains("beta-alanina") || it.contains("beta-alanine") })
    }

    @Test
    fun testWebScrapingEngineVendorOffersAndPriceComparison() {
        val products = scraperEngine.getAllProducts()
        assertTrue("Catalog should contain supplements", products.isNotEmpty())

        val wheyProduct = products.first { it.id == "supp-whey-iso" }
        assertEquals(5, wheyProduct.vendorOffers.size)

        val vendorsInOffers = wheyProduct.vendorOffers.map { it.vendor }.toSet()
        assertTrue(vendorsInOffers.contains(SupplementVendor.PROZIS))
        assertTrue(vendorsInOffers.contains(SupplementVendor.MYPROTEIN))
        assertTrue(vendorsInOffers.contains(SupplementVendor.HSN))
        assertTrue(vendorsInOffers.contains(SupplementVendor.DECATHLON))
        assertTrue(vendorsInOffers.contains(SupplementVendor.BULK_POWDERS))

        val bestOffer = wheyProduct.bestOffer
        assertNotNull("Best offer should be non-null", bestOffer)
        val minPriceInOffers = wheyProduct.vendorOffers.minOf { it.scrapedPrice }
        assertEquals(minPriceInOffers, bestOffer.scrapedPrice, 0.01)

        wheyProduct.vendorOffers.forEach { offer ->
            assertTrue("Scraped price should be less than or equal to original price", offer.scrapedPrice <= offer.originalPrice)
            assertTrue("Discount % should be non-negative", offer.discountPct >= 0)
            assertTrue("Purchase URL must be present", offer.purchaseUrl.startsWith("https://"))
            assertTrue("Rating should be between 1 and 5", offer.rating in 1.0f..5.0f)
        }
    }

    @Test
    fun testSearchAndFiltering() {
        val creatinaResults = scraperEngine.searchProducts(query = "creatina")
        assertTrue("Search for 'creatina' should return products", creatinaResults.isNotEmpty())
        assertTrue("All returned products should match creatina", creatinaResults.all { it.name.lowercase().contains("creatina") })

        val hsnFiltered = scraperEngine.searchProducts(vendorFilter = SupplementVendor.HSN)
        assertTrue("Filtered products should have HSN offer", hsnFiltered.all { product ->
            product.vendorOffers.any { it.vendor == SupplementVendor.HSN }
        })
    }

    @Test
    fun testSimulateLiveWebScrape() {
        val scrapedProducts = scraperEngine.simulateLiveWebScrape(query = "whey")
        assertTrue("Live web scrape should return products", scrapedProducts.isNotEmpty())
        val product = scrapedProducts.first()
        assertTrue("Live scraped offers should exist", product.vendorOffers.isNotEmpty())
    }
}
