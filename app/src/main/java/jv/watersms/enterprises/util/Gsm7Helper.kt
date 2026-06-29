package jv.watersms.enterprises.util

import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.commons.charset.GSMCharset

object Gsm7Helper {
    private const val GSM7_MAX_LENGTH = 160
    private const val UCS2_MAX_LENGTH = 70

    private val gsmCharset = CharsetUtil.CHARSET_GSM

    fun isGsm7(text: String): Boolean = GSMCharset.canRepresent(text)

    fun maxSmsLength(text: String): Int {
        return if (isGsm7(text)) GSM7_MAX_LENGTH else UCS2_MAX_LENGTH
    }

    fun truncateToSmsLimit(text: String): String {
        val maxLen = maxSmsLength(text)
        return if (text.length <= maxLen) text else text.substring(0, maxLen)
    }

    fun normalizeToGsm7(text: String): String {
        return gsmCharset.normalize(text)
    }

    fun prepareForSending(text: String): String {
        val normalized = normalizeToGsm7(text)
        return truncateToSmsLimit(normalized)
    }
}
