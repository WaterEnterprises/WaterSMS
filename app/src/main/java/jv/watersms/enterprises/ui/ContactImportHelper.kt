package jv.watersms.enterprises.ui

import android.content.Context
import android.provider.ContactsContract
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.Phonenumber

data class ImportedContact(
    val name: String,
    val phoneNumber: String,
    val isSelected: Boolean = false
)

object ContactImportHelper {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    /**
     * A comprehensive regex that matches virtually any phone number pattern:
     * - Optional leading `+` for international
     * - Country calling code (1-3 digits)
     * - Optional spaces, dots, dashes, parentheses
     * - Separators like `-`, `.`, ` `, `(`, `)`
     * - 5-15 total digits
     */
    private val phoneRegex = Regex(
        """\+?\d[\d\s.\-()]{4,14}\d""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Detects and extracts ANY phone number from the given raw string,
     * normalizes it, and converts to E.164 international format.
     *
     * Unlike the previous implementation which strictly relied on libphonenumber's
     * parse() (which can fail on unusual formats), this does a multi-pass approach:
     *
     * Pass 1 — Try libphonenumber parse directly (best for well-formed numbers).
     * Pass 2 — If that fails, strip all non-digit characters except leading `+`,
     *          then attempt to parse the cleaned string.
     * Pass 3 — If that also fails, prepend the default region's calling code
     *          and try again (catches numbers like "0912345678" that are local
     *          but missing a +).
     * Pass 4 — Last resort: return the raw number with only spaces/dashes/parens
     *          stripped but the `+` and digits preserved.
     */
    fun formatToInternational(raw: String, region: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return trimmed

        // Pass 1: Direct libphonenumber parse (possible numbers only)
        try {
            val parsed = phoneUtil.parse(trimmed, region.uppercase())
            if (phoneUtil.isPossibleNumber(parsed)) {
                return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            }
        } catch (_: NumberParseException) {
            // Fall through to next pass
        }

        // Pass 2: Strip all non-digit chars, then try again
        val cleaned = cleanPhoneNumber(trimmed)
        if (cleaned != trimmed) {
            try {
                val parsed = phoneUtil.parse(cleaned, region.uppercase())
                if (phoneUtil.isPossibleNumber(parsed)) {
                    return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                }
            } catch (_: NumberParseException) {
                // Fall through
            }
        } else if (cleaned.isNotEmpty()) {
            // Even if input was already clean (e.g. "2121234567"), still try to parse
            try {
                val parsed = phoneUtil.parse(cleaned, region.uppercase())
                if (phoneUtil.isPossibleNumber(parsed)) {
                    return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                }
            } catch (_: NumberParseException) {
                // Fall through
            }
        }

        // Pass 3: Prepend the country calling code and try again
        if (!cleaned.startsWith("+")) {
            try {
                val countryCode = phoneUtil.getCountryCodeForRegion(region.uppercase())
                if (countryCode > 0) {
                    val nationalStr = cleaned.removePrefix("0")
                    val withCode = "+$countryCode$nationalStr"
                    val parsed = phoneUtil.parse(withCode, region.uppercase())
                    if (phoneUtil.isPossibleNumber(parsed)) {
                        return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                    }
                }
            } catch (_: Exception) {
                // Fall through
            }
        }

        // Pass 4: Last resort — strip non-digits but keep +
        return if (cleaned.startsWith("+") && cleaned.length >= 8) {
            "+${cleaned.substring(1).filter { it.isDigit() }}"
        } else {
            cleaned.filter { it.isDigit() || it == '+' }
        }
    }

    /**
     * Strips common formatting characters (spaces, dashes, dots, parentheses)
     * but preserves a leading or embedded `+` sign.
     */
    private fun cleanPhoneNumber(raw: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.contains("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    /**
     * Extracts a phone number from an arbitrary string using regex.
     * Returns the first match found, or the original string if no match.
     * This handles cases like "Name: (555) 123-4567" or "call me at 555.123.4567"
     */
    private fun extractPhoneFromString(raw: String): String {
        // First try to find a phone-like pattern
        val match = phoneRegex.find(raw)
        if (match != null) {
            return match.value.trim()
        }
        return raw
    }

    /**
     * Attempts to determine a contact name from a raw input line.
     * Removes the phone portion and returns whatever remains as the name.
     */
    private fun extractNameFromLine(line: String, phone: String): String {
        var name = line
            .replace(phone, "")
            .trim()
            .removePrefix(",")
            .removePrefix(";")
            .removePrefix("|")
            .removePrefix("\t")
            .trim()
            .removeSuffix(",")
            .removeSuffix(";")
            .removeSuffix("|")
            .removeSuffix("\t")
            .trim()
        // Remove parentheses around name if any
        name = name.removePrefix("(").removeSuffix(")").trim()
        return name
    }

    fun fetchDeviceContacts(context: Context, region: String): List<ImportedContact> {
        val contactsList = mutableListOf<ImportedContact>()
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    if (nameIndex >= 0 && numberIndex >= 0) {
                        val name = it.getString(nameIndex) ?: ""
                        val number = it.getString(numberIndex) ?: ""
                        if (number.isNotBlank()) {
                            val intl = formatToInternational(number, region)
                            contactsList.add(ImportedContact(name, intl))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactsList.distinctBy { it.phoneNumber }
    }

    /**
     * Parses manually pasted contact input with robust detection of ANY phone number format.
     *
     * Accepts:
     * - "Name, Phone" — comma-separated
     * - "Name; Phone" — semicolon-separated
     * - "Name | Phone" — pipe-separated
     * - "Name\tPhone" — tab-separated
     * - "Name Phone" — space-separated (when the phone is clearly a number)
     * - "Phone" — just a phone number (no name)
     * - "Phone — Name" — reverse order with dash
     * - "Name <Phone>" — email-style bracket format
     * - "(Name) Phone" — name in parentheses
     * - Mix of formats on different lines
     */
    fun parseManualContacts(input: String, region: String): List<ImportedContact> {
        val list = mutableListOf<ImportedContact>()
        val lines = input.split("\n")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            try {
                // First, try to extract any phone-like substring from the line
                val phoneStr = extractPhoneFromString(line)
                if (phoneStr.isBlank()) continue

                val formattedPhone = formatToInternational(phoneStr, region)
                if (formattedPhone.isBlank()) continue

                // Determine the name by removing the phone portion from the line
                var name = extractNameFromLine(line, phoneStr)

                // If name is still the full line, phone is probably the entire input
                if (name.isEmpty() || name.equals(line, ignoreCase = true)) {
                    name = formattedPhone
                }

                // De-duplicate within this parse batch
                if (list.none { it.phoneNumber == formattedPhone }) {
                    list.add(ImportedContact(name, formattedPhone))
                }
            } catch (_: Exception) {
                // Skip malformed lines silently
            }
        }

        return list
    }
}
