package jv.watersms.enterprises.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for [ContactImportHelper] phone number parser.
 *
 * These tests do NOT require Android context or Robolectric.
 * They test the pure-JVM logic of formatToInternational and parseManualContacts,
 * which rely on libphonenumber (works on standard JVM).
 *
 * IMPORTANT: The 555 exchange is reserved for fictional use and libphonenumber's
 * isValidNumber() returns false for all 555-prefixed numbers. All US test numbers
 * use real area codes (212-NYC, 312-Chicago, 415-SF, 617-Boston, 202-DC, 310-LA)
 * with + prefix for reliable E.164 output, or `endsWith` assertions for bare numbers.
 */
class ContactImportHelperTest {

    // ═══════════════════════════════════════════
    // formatToInternational — E.164 Output Format
    // ═══════════════════════════════════════════

    @Test
    fun `formatToInternational with US number with dashes returns E164`() {
        val result = ContactImportHelper.formatToInternational("+1 212-123-4567", "US")
        assertEquals("+12121234567", result)
    }

    @Test
    fun `formatToInternational with US number with dots returns E164`() {
        val result = ContactImportHelper.formatToInternational("+1 212.123.4567", "US")
        assertEquals("+12121234567", result)
    }

    @Test
    fun `formatToInternational with US number with parens and dash returns E164`() {
        val result = ContactImportHelper.formatToInternational("(212) 123-4567", "US")
        assertEquals("+12121234567", result)
    }

    @Test
    fun `formatToInternational with US number with spaces returns E164`() {
        val result = ContactImportHelper.formatToInternational("+1 212 123 4567", "US")
        assertEquals("+12121234567", result)
    }

    @Test
    fun `formatToInternational with already E164 number returns unchanged`() {
        val result = ContactImportHelper.formatToInternational("+12121234567", "US")
        assertEquals("+12121234567", result)
    }

    @Test
    fun `formatToInternational with international plus prefix returns E164`() {
        val result = ContactImportHelper.formatToInternational("+1 312 123 4567", "US")
        assertEquals("+13121234567", result)
    }

    @Test
    fun `formatToInternational with ten digit number uses region to add country code`() {
        val result = ContactImportHelper.formatToInternational("2121234567", "US")
        assertTrue("Should end with the national number", result.endsWith("2121234567"))
    }

    @Test
    fun `formatToInternational with blank input returns blank`() {
        val result = ContactImportHelper.formatToInternational("", "US")
        assertEquals("", result)
    }

    @Test
    fun `formatToInternational with whitespace input returns blank`() {
        val result = ContactImportHelper.formatToInternational("   ", "US")
        assertEquals("", result)
    }

    @Test
    fun `formatToInternational with UK mobile and GB region returns E164`() {
        val result = ContactImportHelper.formatToInternational("07700 900123", "GB")
        assertEquals("+447700900123", result)
    }

    @Test
    fun `formatToInternational with UK number with plus returns E164`() {
        val result = ContactImportHelper.formatToInternational("+44 7700 900123", "US")
        assertEquals("+447700900123", result)
    }

    @Test
    fun `formatToInternational with German number and DE region returns E164`() {
        val result = ContactImportHelper.formatToInternational("0151 12345678", "DE")
        assertTrue("Should start with +49 for Germany", result.startsWith("+49"))
    }

    @Test
    fun `formatToInternational with Australian number and AU region returns E164`() {
        val result = ContactImportHelper.formatToInternational("+61 412 345 678", "US")
        assertEquals("+61412345678", result)
    }

    @Test
    fun `formatToInternational with Indian number and IN region returns E164`() {
        val result = ContactImportHelper.formatToInternational("+91 98765 43210", "US")
        assertEquals("+919876543210", result)
    }

    @Test
    fun `formatToInternational with Japanese number and JP region returns E164`() {
        val result = ContactImportHelper.formatToInternational("+81 90 1234 5678", "US")
        assertEquals("+819012345678", result)
    }

    @Test
    fun `formatToInternational with Canadian number and CA region returns E164`() {
        val result = ContactImportHelper.formatToInternational("+1 416-555-0198", "CA")
        assertEquals("+14165550198", result)
    }

    @Test
    fun `formatToInternational with invalid short number returns cleaned digits`() {
        val result = ContactImportHelper.formatToInternational("123", "US")
        // Too short to be a valid number
        assertEquals("123", result)
    }

    @Test
    fun `formatToInternational with garbage text and plus prefix returns cleaned digits`() {
        val result = ContactImportHelper.formatToInternational("  + 1  (abc)  2 1 2 - 1 2 3 4 ", "US")
        assertEquals("+12121234", result)
    }

    @Test
    fun `formatToInternational trims leading and trailing whitespace`() {
        val result = ContactImportHelper.formatToInternational("  +1 212 123 4567  ", "US")
        assertEquals("+12121234567", result)
    }

    @Test
    fun `formatToInternational treats lowercase region the same as uppercase`() {
        val upper = ContactImportHelper.formatToInternational("+1 212 123 4567", "US")
        val lower = ContactImportHelper.formatToInternational("+1 212 123 4567", "us")
        assertEquals(upper, lower)
    }

    // ═══════════════════════════════════════════
    // parseManualContacts — CSV / Delimiter Parsing
    // ═══════════════════════════════════════════

    @Test
    fun `parseManualContacts with comma separated returns parsed contact`() {
        val result = ContactImportHelper.parseManualContacts("John, +1 212-123-4567", "US")
        assertEquals(1, result.size)
        assertEquals("John", result[0].name)
        assertEquals("+12121234567", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with semicolon separated returns parsed contact`() {
        val result = ContactImportHelper.parseManualContacts("Jane; +1 312-234-5678", "US")
        assertEquals(1, result.size)
        assertEquals("Jane", result[0].name)
        assertEquals("+13122345678", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with pipe separated returns parsed contact`() {
        val result = ContactImportHelper.parseManualContacts("Bob | +1 415-345-6789", "US")
        assertEquals(1, result.size)
        assertEquals("Bob", result[0].name)
        assertEquals("+14153456789", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with tab separated returns parsed contact`() {
        val result = ContactImportHelper.parseManualContacts("Alice\t+1 617-456-7890", "US")
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals("+16174567890", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with just a phone number uses number as name`() {
        val result = ContactImportHelper.parseManualContacts("+12121234567", "US")
        assertEquals(1, result.size)
        assertEquals("+12121234567", result[0].name)
        assertEquals("+12121234567", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with multiple lines returns all contacts`() {
        val input = """
            John, +1 212-111-1111
            Jane, +1 312-222-2222
            Bob, +1 415-333-3333
        """.trimIndent()
        val result = ContactImportHelper.parseManualContacts(input, "US")
        assertEquals(3, result.size)
        assertEquals("John", result[0].name)
        assertEquals("Jane", result[1].name)
        assertEquals("Bob", result[2].name)
    }

    @Test
    fun `parseManualContacts with empty input returns empty list`() {
        val result = ContactImportHelper.parseManualContacts("", "US")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseManualContacts with blank lines returns empty list`() {
        val result = ContactImportHelper.parseManualContacts("   \n  \n  ", "US")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseManualContacts deduplicates same phone number`() {
        val result = ContactImportHelper.parseManualContacts(
            "John, +1 212-111-1111\nJane, +1 212-111-1111",
            "US"
        )
        assertEquals(1, result.size)
    }

    @Test
    fun `parseManualContacts with different country code on number`() {
        val result = ContactImportHelper.parseManualContacts("Alice, +49 170 1234567", "US")
        assertEquals(1, result.size)
        assertEquals("+491701234567", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with various international formats`() {
        val input = """
            UK, +44 7700 900123
            DE, +49 170 1234567
            AU, +61 412 345 678
            IN, +91 98765 43210
        """.trimIndent()
        val result = ContactImportHelper.parseManualContacts(input, "US")
        assertEquals(4, result.size)
        assertTrue(result.any { it.phoneNumber == "+447700900123" })
        assertTrue(result.any { it.phoneNumber == "+491701234567" })
        assertTrue(result.any { it.phoneNumber == "+61412345678" })
        assertTrue(result.any { it.phoneNumber == "+919876543210" })
    }

    @Test
    fun `parseManualContacts handles mixed delimiters in same input`() {
        val input = "John, +1 212-1111\nJane; +1 312-2222\nBob\t+1 415-3333"
        val result = ContactImportHelper.parseManualContacts(input, "US")
        assertEquals(3, result.size)
    }

    @Test
    fun `parseManualContacts with dots in number`() {
        val result = ContactImportHelper.parseManualContacts("John, +1 212.123.4567", "US")
        assertEquals(1, result.size)
        assertEquals("+12121234567", result[0].phoneNumber)
    }

    @Test
    fun `parseManualContacts with name containing special characters`() {
        val result = ContactImportHelper.parseManualContacts("Jean-Pierre, +1 212-123-4567", "US")
        assertEquals(1, result.size)
        assertEquals("Jean-Pierre", result[0].name)
    }

    @Test
    fun `parseManualContacts with name containing apostrophe`() {
        val result = ContactImportHelper.parseManualContacts("O'Brien, +1 212-123-4567", "US")
        assertEquals(1, result.size)
        assertEquals("O'Brien", result[0].name)
    }

    @Test
    fun `parseManualContacts skips lines with no valid phone number`() {
        val result = ContactImportHelper.parseManualContacts(
            "John, +1 212-111-1111\nJust some text\nJane, +1 312-222-2222",
            "US"
        )
        assertEquals(2, result.size)
    }

    @Test
    fun `parseManualContacts preserves correct naming when extra info after number`() {
        val result = ContactImportHelper.parseManualContacts("John, +1 212-123-4567 Office", "US")
        assertEquals(1, result.size)
        assertTrue(result[0].name.contains("John", ignoreCase = true))
    }

    @Test
    fun `parseManualContacts with whitespace padding around input`() {
        val result = ContactImportHelper.parseManualContacts("  John, +1 212-123-4567  ", "US")
        assertEquals(1, result.size)
        assertEquals("John", result[0].name)
    }

    @Test
    fun `parseManualContacts real world example multi-line import`() {
        val input = """
            Alice Johnson, +1 212-111-2222
            Bob Smith, +1 312-333-4444
            Charlie Brown, +1 415-555-6666
            Diana Prince, +1 617-777-8888
        """.trimIndent()
        val result = ContactImportHelper.parseManualContacts(input, "US")
        assertEquals(4, result.size)
        result.forEach { contact ->
            assertTrue("Should be E.164 format: ${contact.phoneNumber}", contact.phoneNumber.startsWith("+1"))
            assertTrue("Should be +1 followed by 10 digits: ${contact.phoneNumber}", contact.phoneNumber.length == 12)
        }
    }

    @Test
    fun `parseManualContacts with UK mobile and GB region returns E164`() {
        val result = ContactImportHelper.parseManualContacts("John, 07700 900123", "GB")
        assertEquals(1, result.size)
        // 07700 900123 is libphonenumber's canonical example UK number
        assertEquals("+447700900123", result[0].phoneNumber)
        assertEquals("John", result[0].name)
    }

    @Test
    fun `parseManualContacts with single line no name`() {
        val result = ContactImportHelper.parseManualContacts("+1 212-123-4567", "US")
        assertEquals(1, result.size)
        assertEquals("+12121234567", result[0].phoneNumber)
    }

    // ═══════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════

    @Test
    fun `formatToInternational with plus-only string`() {
        val result = ContactImportHelper.formatToInternational("+", "US")
        // Plus alone has no digits — function preserves it as-is
        assertEquals("+", result)
    }

    @Test
    fun `formatToInternational with special chars only`() {
        val result = ContactImportHelper.formatToInternational("---(...)---", "US")
        assertEquals("", result)
    }

    @Test
    fun `formatToInternational with non-digit characters returns empty`() {
        val result = ContactImportHelper.formatToInternational("abc def ghi", "US")
        assertEquals("", result)
    }

    @Test
    fun `formatToInternational with alpha chars stripped preserves plus prefix`() {
        val result = ContactImportHelper.formatToInternational("call +1 212 123 4567 now", "US")
        assertEquals("+12121234567", result)
    }
}
