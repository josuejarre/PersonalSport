package com.example.personalsport.supplement

import java.util.Locale

enum class SupplementGoal(
    val title: String,
    val subtitle: String,
    val description: String
) {
    MUSCLE_GAIN(
        title = "Ganancia Muscular",
        subtitle = "Hipertrofia & Fuerza",
        description = "Maximiza la síntesis proteica, la congestión muscular y la fuerza explosiva en tus entrenamientos."
    ),
    FAT_LOSS(
        title = "Pérdida de Grasa",
        subtitle = "Definición & Termogénesis",
        description = "Estimula la movilización de ácidos grasos, eleva el gasto calórico basal y preserva la masa magra."
    ),
    ENDURANCE(
        title = "Resistencia (Running/Swim)",
        subtitle = "Rendimiento Aeróbico & Lactato",
        description = "Optimiza la hidratación isomolar, retrasa el umbral de lactato y previene el agotamiento por glucógeno."
    ),
    RECOVERY(
        title = "Recuperación Rápida",
        subtitle = "Reparación & Anti-Inflamatorio",
        description = "Acelera la reconstrucción del tejido muscular post-esfuerzo y reduce el dolor muscular diferido (agujetas)."
    )
}

enum class SupplementVendor(
    val displayName: String,
    val baseUrl: String,
    val badgeColorHex: Long
) {
    PROZIS("Prozis", "https://www.prozis.com", 0xFFE53935),
    MYPROTEIN("MyProtein", "https://www.myprotein.es", 0xFF00ACC1),
    HSN("HSN Store", "https://www.hsnstore.com", 0xFFFB8C00),
    DECATHLON("Decathlon", "https://www.decathlon.es", 0xFF1E88E5),
    BULK_POWDERS("Bulk Powders", "https://www.bulk.com/es", 0xFF43A047)
}

data class SupplementVendorOffer(
    val vendor: SupplementVendor,
    val productTitle: String,
    val originalPrice: Double,
    val scrapedPrice: Double,
    val discountPct: Int = ((originalPrice - scrapedPrice) / originalPrice * 100).toInt().coerceAtLeast(0),
    val rating: Float,
    val reviewsCount: Int,
    val purchaseUrl: String,
    val inStock: Boolean = true,
    val shippingInfo: String = "Envío 24/48h"
)

data class SupplementProduct(
    val id: String,
    val name: String,
    val categoryName: String,
    val primaryGoal: SupplementGoal,
    val dosageAndTiming: String,
    val aiRationale: String,
    val vendorOffers: List<SupplementVendorOffer>
) {
    val bestOffer: SupplementVendorOffer
        get() = vendorOffers.minByOrNull { it.scrapedPrice } ?: vendorOffers.first()

    val maxDiscountPct: Int
        get() = vendorOffers.maxOfOrNull { it.discountPct } ?: 0

    val averageRating: Float
        get() = if (vendorOffers.isNotEmpty()) vendorOffers.map { it.rating }.average().toFloat() else 4.5f
}

object SupplementScraperEngine {

    private val catalog = listOf(
        // 1. Whey Isolate
        SupplementProduct(
            id = "supp-whey-iso",
            name = "Whey Protein Isolate 100%",
            categoryName = "Proteína de Aislamiento",
            primaryGoal = SupplementGoal.MUSCLE_GAIN,
            dosageAndTiming = "30g (1 cazo) disuelto en 250ml de agua o leche dentro de los 30 min post-entrenamiento.",
            aiRationale = "Proporciona un 90% de proteína pura de absorción ultra rápida con mínimo nivel de grasa y lactosa. Estimula de forma directa la vía mTOR para acelerar la hipertrofia muscular.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis 100% Real Whey Isolate 1000g",
                    originalPrice = 38.99,
                    scrapedPrice = 27.99,
                    rating = 4.8f,
                    reviewsCount = 2450,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/100-real-whey-isolate-1000-g",
                    shippingInfo = "Gratis > 29€ · Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein Impact Whey Isolate 1kg",
                    originalPrice = 42.99,
                    scrapedPrice = 29.99,
                    rating = 4.7f,
                    reviewsCount = 3890,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/impact-whey-isolate/10530911.html",
                    shippingInfo = "Gratis > 45€ · Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN Evowhey Protein Isolate 2.0 1kg",
                    originalPrice = 34.90,
                    scrapedPrice = 22.90,
                    rating = 4.9f,
                    reviewsCount = 5120,
                    purchaseUrl = "https://www.hsnstore.com/marcas/sport-series/evowhey-protein-isolate-2-0",
                    shippingInfo = "Gratis > 24.90€ · Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon Domyos Whey Isolate 100 1kg",
                    originalPrice = 32.99,
                    scrapedPrice = 26.99,
                    rating = 4.5f,
                    reviewsCount = 820,
                    purchaseUrl = "https://www.decathlon.es/es/p/proteina-aislada-whey-isolate-1kg/_/R-p-309121",
                    shippingInfo = "Recogida gratis en tienda"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk Pure Whey Isolate 1kg",
                    originalPrice = 39.99,
                    scrapedPrice = 25.99,
                    rating = 4.8f,
                    reviewsCount = 1750,
                    purchaseUrl = "https://www.bulk.com/es/pure-whey-isolate-es.html",
                    shippingInfo = "Gratis > 35€ · Entrega 48h"
                )
            )
        ),

        // 2. Creatine Monohydrate
        SupplementProduct(
            id = "supp-creatine",
            name = "Creatina Monohidrato Creapure®",
            categoryName = "Ergogénico de Fuerza",
            primaryGoal = SupplementGoal.MUSCLE_GAIN,
            dosageAndTiming = "5g diarios disueltos en agua, preferentemente post-entreno o con comida rica en carbohidratos.",
            aiRationale = "Incrementa las reservas de fosfocreatina intramuscular. Aumenta la fuerza máxima de 1RM, el número de repeticiones hasta el fallo y el volumen sarcoplásmico.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis Creatine Monohydrate Creapure 500g",
                    originalPrice = 29.99,
                    scrapedPrice = 19.99,
                    rating = 4.9f,
                    reviewsCount = 1980,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/creatine-monohydrate-creapure-500g",
                    shippingInfo = "Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein Creapure Creatine Powder 500g",
                    originalPrice = 34.99,
                    scrapedPrice = 23.99,
                    rating = 4.8f,
                    reviewsCount = 2910,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/creapure-creatina-en-polvo/10529741.html",
                    shippingInfo = "Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN Creatina Monohidrato Creapure® 500g",
                    originalPrice = 26.90,
                    scrapedPrice = 16.90,
                    rating = 5.0f,
                    reviewsCount = 6420,
                    purchaseUrl = "https://www.hsnstore.com/marcas/raw-series/creatina-monohidrato-en-polvo-creapure",
                    shippingInfo = "Gratis > 24.90€ · Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon Creatine Creapure Powder 300g",
                    originalPrice = 18.99,
                    scrapedPrice = 14.99,
                    rating = 4.4f,
                    reviewsCount = 530,
                    purchaseUrl = "https://www.decathlon.es/es/p/creatina-creapure-300g/_/R-p-320199",
                    shippingInfo = "Recogida gratis"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk Pure Creatine Monohydrate 500g",
                    originalPrice = 27.99,
                    scrapedPrice = 17.99,
                    rating = 4.8f,
                    reviewsCount = 1420,
                    purchaseUrl = "https://www.bulk.com/es/creatina-monohidrato-en-polvo.html",
                    shippingInfo = "Entrega 48h"
                )
            )
        ),

        // 3. L-Carnitine Tartrate
        SupplementProduct(
            id = "supp-lcarnitine",
            name = "L-Carnitine L-Tartrate 3000mg",
            categoryName = "Movilizador Graso",
            primaryGoal = SupplementGoal.FAT_LOSS,
            dosageAndTiming = "2000-3000mg 30 minutos antes de tu sesión cardio/aeróbica con un vaso de agua.",
            aiRationale = "Actúa como lanzadera lipídica transportando ácidos grasos de cadena larga al interior de la mitocondria para su B-oxidación y conversión en ATP celular.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis L-Carnitine 3000 Liquid 20 Viales",
                    originalPrice = 22.99,
                    scrapedPrice = 14.99,
                    rating = 4.6f,
                    reviewsCount = 890,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/l-carnitine-3000-20-viales",
                    shippingInfo = "Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein L-Carnitine Amino Acid 180 Caps",
                    originalPrice = 24.99,
                    scrapedPrice = 16.99,
                    rating = 4.5f,
                    reviewsCount = 1120,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/l-carnitine/10529815.html",
                    shippingInfo = "Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN L-Carnitina L-Tartrato 150g Polvo",
                    originalPrice = 18.90,
                    scrapedPrice = 11.90,
                    rating = 4.8f,
                    reviewsCount = 2150,
                    purchaseUrl = "https://www.hsnstore.com/marcas/raw-series/l-carnitina-l-tartrato-en-polvo",
                    shippingInfo = "Gratis > 24.90€"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon L-Carnitine Liquid Shot 500ml",
                    originalPrice = 16.99,
                    scrapedPrice = 12.99,
                    rating = 4.3f,
                    reviewsCount = 340,
                    purchaseUrl = "https://www.decathlon.es/es/p/l-carnitina-liquida-500ml/_/R-p-310111",
                    shippingInfo = "Recogida en tienda"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk L-Carnitine Powder 100g",
                    originalPrice = 19.99,
                    scrapedPrice = 13.99,
                    rating = 4.6f,
                    reviewsCount = 670,
                    purchaseUrl = "https://www.bulk.com/es/l-carnitina-en-polvo.html",
                    shippingInfo = "Entrega 48h"
                )
            )
        ),

        // 4. Green Tea Extract
        SupplementProduct(
            id = "supp-greentea",
            name = "Extracto de Té Verde (50% EGCG)",
            categoryName = "Termogénico & Antioxidante",
            primaryGoal = SupplementGoal.FAT_LOSS,
            dosageAndTiming = "500mg (equivalente a 250mg EGCG) en el desayuno o antes del entrenamiento cardio.",
            aiRationale = "Inhibe la enzima catecol-O-metiltransferasa (COMT), prolongando la acción de la noradrenalina para potenciar la termogénesis y acelerar el metabolismo basal.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis Green Tea Extract 500mg 90 Caps",
                    originalPrice = 16.99,
                    scrapedPrice = 9.99,
                    rating = 4.7f,
                    reviewsCount = 640,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/green-tea-extract-500mg",
                    shippingInfo = "Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein Green Tea Extract 120 Caps",
                    originalPrice = 18.99,
                    scrapedPrice = 11.99,
                    rating = 4.6f,
                    reviewsCount = 950,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/extracto-de-te-verde/10530111.html",
                    shippingInfo = "Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN Extracto de Té Verde (50% EGCG) 120 Veg Caps",
                    originalPrice = 14.90,
                    scrapedPrice = 8.90,
                    rating = 4.9f,
                    reviewsCount = 1830,
                    purchaseUrl = "https://www.hsnstore.com/marcas/essential-series/extracto-de-te-verde-500mg",
                    shippingInfo = "Gratis > 24.90€"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon Thermogenic Green Tea Caps 60 Caps",
                    originalPrice = 12.99,
                    scrapedPrice = 9.99,
                    rating = 4.2f,
                    reviewsCount = 210,
                    purchaseUrl = "https://www.decathlon.es/es/p/te-verde-capsulas/_/R-p-321102",
                    shippingInfo = "Recogida gratis"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk Super Green Tea Extract 100 Caps",
                    originalPrice = 15.99,
                    scrapedPrice = 10.49,
                    rating = 4.7f,
                    reviewsCount = 510,
                    purchaseUrl = "https://www.bulk.com/es/extracto-super-te-verde.html",
                    shippingInfo = "Entrega 48h"
                )
            )
        ),

        // 5. Electrolytes & Isotonic
        SupplementProduct(
            id = "supp-electrolytes",
            name = "Electrolitos Isomolares & Sales Minerales",
            categoryName = "Hidratación Endurance",
            primaryGoal = SupplementGoal.ENDURANCE,
            dosageAndTiming = "1 dosis en 500ml de agua sorbo a sorbo durante carreras > 60 mins o entrenamientos de natación intensos.",
            aiRationale = "Reemplaza el sodio, potasio, magnesio y calcio perdidos por el sudor. Previene calambres musculares, hiponatremia y deshidratación celular en deportes de resistencia.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis Isotonic Hydration Drink Powder 1kg",
                    originalPrice = 21.99,
                    scrapedPrice = 13.99,
                    rating = 4.8f,
                    reviewsCount = 1320,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/isotonic-hydration-1000g",
                    shippingInfo = "Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein Electrolyte Powder 250g",
                    originalPrice = 19.99,
                    scrapedPrice = 12.99,
                    rating = 4.6f,
                    reviewsCount = 1580,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/electrolitos-en-polvo/10529891.html",
                    shippingInfo = "Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN Evosalts / Sales de Electrolitos 120 Caps",
                    originalPrice = 15.90,
                    scrapedPrice = 9.90,
                    rating = 4.9f,
                    reviewsCount = 3210,
                    purchaseUrl = "https://www.hsnstore.com/marcas/sport-series/evosalts-sales-minerales",
                    shippingInfo = "Gratis > 24.90€"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon Aptonia Isotonic Drink Powder 650g",
                    originalPrice = 14.99,
                    scrapedPrice = 11.99,
                    rating = 4.7f,
                    reviewsCount = 2450,
                    purchaseUrl = "https://www.decathlon.es/es/p/bebida-isotonica-aptonia-650g/_/R-p-170119",
                    shippingInfo = "Recogida gratis"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk Complete Electrolyte Powder 250g",
                    originalPrice = 17.99,
                    scrapedPrice = 11.49,
                    rating = 4.7f,
                    reviewsCount = 890,
                    purchaseUrl = "https://www.bulk.com/es/electrolitos-completos.html",
                    shippingInfo = "Entrega 48h"
                )
            )
        ),

        // 6. Beta-Alanine
        SupplementProduct(
            id = "supp-beta-alanine",
            name = "Beta-Alanina Pura (Carnosyn®)",
            categoryName = "Tampón de Ácido Láctico",
            primaryGoal = SupplementGoal.ENDURANCE,
            dosageAndTiming = "3.2g a 5g diarios repartidos en 2 tomas (por ejemplo antes de entrenar y con la comida).",
            aiRationale = "Eleva la carnosina intramuscular, actuando como un potente tampón de hidrogeniones (H+). Retraza la acidosis y la fatiga en pasajes de alta intensidad en series de atletismo y natación.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis Beta-Alanine 1000mg 120 Tabs",
                    originalPrice = 23.99,
                    scrapedPrice = 15.99,
                    rating = 4.7f,
                    reviewsCount = 910,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/beta-alanine-1000mg",
                    shippingInfo = "Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein Beta-Alanine Powder 250g",
                    originalPrice = 27.99,
                    scrapedPrice = 18.99,
                    rating = 4.8f,
                    reviewsCount = 1640,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/beta-alanina-en-polvo/10529811.html",
                    shippingInfo = "Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN Beta-Alanina 100% Pura 500g",
                    originalPrice = 22.90,
                    scrapedPrice = 13.90,
                    rating = 4.9f,
                    reviewsCount = 2780,
                    purchaseUrl = "https://www.hsnstore.com/marcas/raw-series/beta-alanina-en-polvo",
                    shippingInfo = "Gratis > 24.90€"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon Endurance Beta-Alanine 100 Caps",
                    originalPrice = 19.99,
                    scrapedPrice = 15.99,
                    rating = 4.4f,
                    reviewsCount = 310,
                    purchaseUrl = "https://www.decathlon.es/es/p/beta-alanina-capsulas/_/R-p-331001",
                    shippingInfo = "Recogida gratis"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk Pure Beta-Alanine 250g",
                    originalPrice = 24.99,
                    scrapedPrice = 16.49,
                    rating = 4.7f,
                    reviewsCount = 730,
                    purchaseUrl = "https://www.bulk.com/es/beta-alanina-en-polvo.html",
                    shippingInfo = "Entrega 48h"
                )
            )
        ),

        // 7. BCAA & Glutamine
        SupplementProduct(
            id = "supp-bcaa-glutamine",
            name = "BCAA 8:1:1 + L-Glutamina",
            categoryName = "Recuperación Muscular & Anti-Catabólico",
            primaryGoal = SupplementGoal.RECOVERY,
            dosageAndTiming = "10g disueltos en agua inmediatamente después de finalizar la sesión de entrenamiento.",
            aiRationale = "Combina leucina de alto ratio (8:1:1) para encender la síntesis proteica con L-glutamina para reponer las reservas comprometidas en el sistema inmune e intestinal.",
            vendorOffers = listOf(
                SupplementVendorOffer(
                    vendor = SupplementVendor.PROZIS,
                    productTitle = "Prozis BCAA 8:1:1 Powder 300g",
                    originalPrice = 24.99,
                    scrapedPrice = 16.99,
                    rating = 4.8f,
                    reviewsCount = 1100,
                    purchaseUrl = "https://www.prozis.com/es/es/prozis/bcaa-811-300g",
                    shippingInfo = "Entrega 24h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.MYPROTEIN,
                    productTitle = "MyProtein Essential BCAA Powder 250g",
                    originalPrice = 26.99,
                    scrapedPrice = 17.99,
                    rating = 4.7f,
                    reviewsCount = 2200,
                    purchaseUrl = "https://www.myprotein.es/nutricion-deportiva/bcaa-en-polvo/10529851.html",
                    shippingInfo = "Entrega 48h"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.HSN,
                    productTitle = "HSN Evobcaa's Complex 2.0 (BCAA + Glutamina) 500g",
                    originalPrice = 28.90,
                    scrapedPrice = 17.90,
                    rating = 4.9f,
                    reviewsCount = 4120,
                    purchaseUrl = "https://www.hsnstore.com/marcas/sport-series/evobcaas-2-0",
                    shippingInfo = "Gratis > 24.90€"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.DECATHLON,
                    productTitle = "Decathlon BCAA Recover Drink 500g",
                    originalPrice = 22.99,
                    scrapedPrice = 18.99,
                    rating = 4.5f,
                    reviewsCount = 670,
                    purchaseUrl = "https://www.decathlon.es/es/p/bcaa-recuperacion-500g/_/R-p-312999",
                    shippingInfo = "Recogida gratis"
                ),
                SupplementVendorOffer(
                    vendor = SupplementVendor.BULK_POWDERS,
                    productTitle = "Bulk BCAA Glutamine Powder 500g",
                    originalPrice = 29.99,
                    scrapedPrice = 18.99,
                    rating = 4.8f,
                    reviewsCount = 980,
                    purchaseUrl = "https://www.bulk.com/es/bcaa-glutamina-polvo.html",
                    shippingInfo = "Entrega 48h"
                )
            )
        )
    )

    fun getAllProducts(): List<SupplementProduct> = catalog

    /**
     * AI recommendation logic returning products matching the user goal.
     */
    fun getAiRecommendedProductsForGoal(goal: SupplementGoal): List<SupplementProduct> {
        return catalog.filter { it.primaryGoal == goal }
    }

    /**
     * Search & Filter function over the catalog across major vendors.
     */
    fun searchProducts(
        query: String = "",
        goalFilter: SupplementGoal? = null,
        vendorFilter: SupplementVendor? = null
    ): List<SupplementProduct> {
        val q = query.trim().lowercase(Locale.ROOT)
        return catalog.filter { product ->
            val matchesQuery = q.isEmpty() ||
                    product.name.lowercase(Locale.ROOT).contains(q) ||
                    product.categoryName.lowercase(Locale.ROOT).contains(q) ||
                    product.dosageAndTiming.lowercase(Locale.ROOT).contains(q) ||
                    product.vendorOffers.any { it.productTitle.lowercase(Locale.ROOT).contains(q) }

            val matchesGoal = goalFilter == null || product.primaryGoal == goalFilter

            val matchesVendor = vendorFilter == null || product.vendorOffers.any { it.vendor == vendorFilter }

            matchesQuery && matchesGoal && matchesVendor
        }
    }

    /**
     * Simulates a live web scraping engine searching major vendors (Prozis, MyProtein, HSN, Decathlon, Bulk Powders).
     * Returns refreshed price offers with current discounts, quality ratings, and vendor direct purchase links.
     */
    fun simulateLiveWebScrape(query: String = ""): List<SupplementProduct> {
        return searchProducts(query = query).map { product ->
            // Slight dynamic adjustments to simulate live scraped variations
            val refreshedOffers = product.vendorOffers.map { offer ->
                val randomFactor = (0.95 + (offer.vendor.ordinal * 0.02) % 0.08)
                val refreshedScraped = (offer.scrapedPrice * randomFactor * 100).toInt() / 100.0
                offer.copy(
                    scrapedPrice = refreshedScraped,
                    discountPct = ((offer.originalPrice - refreshedScraped) / offer.originalPrice * 100).toInt().coerceAtLeast(0)
                )
            }
            product.copy(vendorOffers = refreshedOffers)
        }
    }
}
