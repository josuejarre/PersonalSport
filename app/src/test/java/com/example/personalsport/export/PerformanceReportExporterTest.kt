package com.example.personalsport.export

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private class TestExportContext(private val tempCacheDir: File) : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getCacheDir(): File = tempCacheDir
}

class PerformanceReportExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testContext: Context
    private lateinit var exporter: PerformanceReportExporter

    @Before
    fun setUp() {
        testContext = TestExportContext(tempFolder.root)
        exporter = PerformanceReportExporter(testContext)
    }

    @Test
    fun testGenerateReportPdfCreatesValidFile() {
        val pdfFile = exporter.generateReportPdf(
            profileName = "Carlos Atleta",
            weightKg = 76.2,
            bodyFatPercent = 13.5,
            muscleMassKg = 40.1,
            runningKm = 52.0,
            swimmingMeters = 3500,
            swolfScore = 33,
            tonnageKg = 30000.0,
            sleepScore = 90,
            photoCount = 8
        )

        assertNotNull(pdfFile)
        assertTrue(pdfFile.name.endsWith(".pdf"))
        assertTrue(pdfFile.name.contains("PersonalSport"))
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }
}
