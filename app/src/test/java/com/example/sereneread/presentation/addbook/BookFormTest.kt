package com.example.sereneread.presentation.addbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BookFormTest {

    private val year = 2026

    @Test
    fun `valid form returns trimmed input`() {
        val result = validateBookForm("  Babel  ", "  R. F. Kuang ", " 2022 ", currentYear = year)
        assertEquals(BookFormInput("Babel", "R. F. Kuang", 2022), result.input)
        assertNull(result.titleError)
        assertNull(result.yearError)
    }

    @Test
    fun `empty author and year become null`() {
        val result = validateBookForm("Babel", "   ", "", currentYear = year)
        assertEquals(BookFormInput("Babel", null, null), result.input)
    }

    @Test
    fun `blank title is rejected`() {
        val result = validateBookForm("   ", "Kuang", "2022", currentYear = year)
        assertNull(result.input)
        assertEquals("Enter a title", result.titleError)
    }

    @Test
    fun `year range matches backend rule 1000 to current year`() {
        assertNotNull(validateBookForm("A", "", "1000", currentYear = year).input)
        assertNotNull(validateBookForm("A", "", "2026", currentYear = year).input)

        val tooOld = validateBookForm("A", "", "999", currentYear = year)
        assertNull(tooOld.input)
        assertEquals("Use a year between 1000 and 2026", tooOld.yearError)

        assertNull(validateBookForm("A", "", "2027", currentYear = year).input)
        assertNull(validateBookForm("A", "", "3000", currentYear = year).input)
    }

    @Test
    fun `non numeric year is rejected`() {
        val result = validateBookForm("A", "", "20x2", currentYear = year)
        assertNull(result.input)
        assertNotNull(result.yearError)
    }

    @Test
    fun `both errors are reported together`() {
        val result = validateBookForm("", "", "3000", currentYear = year)
        assertNotNull(result.titleError)
        assertNotNull(result.yearError)
    }

    @Test
    fun `suggests a readable title from the file name`() {
        assertEquals(
            "OceanofPDF.com Saha Mansion",
            suggestTitleFromFileName("_OceanofPDF.com_Saha_Mansion.pdf")
        )
        assertEquals("Thinking Fast Slow (2011)", suggestTitleFromFileName("Thinking_Fast_Slow_(2011).PDF"))
        assertEquals("Babel", suggestTitleFromFileName("Babel"))
    }
}
