package com.qrhealthcare.app.data.payment

/**
 * Builds a VietQR / Napas-247 compliant EMV QR payload that any Vietnamese
 * banking app can scan to initiate an instant transfer.
 *
 * Spec reference: NAPAS QR transfer specification (EMVCo Merchant-Presented
 * QR with the Napas GUID `A000000727`). The resulting string is rendered as
 * a normal QR code in the UI — there is no network call here, so this works
 * fully offline.
 *
 * The QR encodes:
 *   - which bank (BIN) and account number the funds go to
 *   - the transfer amount in VND (omit for an "open amount" QR)
 *   - an optional reference / note that appears in the sender's bank app
 *
 * When the customer scans this with their bank's mobile app, the amount and
 * receiver are pre-filled and they only have to confirm. The bank handles
 * the actual transfer.
 *
 * NOTE: We do not (and cannot, without a bank integration) automatically
 * detect that a payment has cleared. That requires either a bank webhook
 * (e.g. SePay / Casso) or polling a statement API. For now the user manually
 * confirms with a "Tôi đã chuyển khoản" button after they complete the
 * transfer in their bank app.
 */
object VietQR {

    /**
     * Build the EMV string. Pass [amount] = null to make a "static" QR that
     * lets the sender type any amount in their bank app.
     *
     * [description] becomes the transfer note (e.g. an order reference).
     * Bank apps allow ~25 ASCII chars; we strip diacritics and trim.
     */
    fun build(
        bankBin: String,
        accountNumber: String,
        amount: Long? = null,
        description: String? = null
    ): String {
        val merchantAccountInfo =
            tlv("00", "A000000727") +                                  // Napas GUID
                tlv("01", tlv("00", bankBin) + tlv("01", accountNumber)) + // Acquirer / account
                tlv("02", "QRIBFTTA")                                  // Service: account-to-account

        val initMethod = if (amount != null) "12" else "11"            // 12=dynamic, 11=static

        var payload =
            tlv("00", "01") +                  // Payload format indicator
                tlv("01", initMethod) +
                tlv("38", merchantAccountInfo) + // 38 = Napas merchant info container
                tlv("53", "704") +               // Currency: VND
                (amount?.let { tlv("54", it.toString()) } ?: "") +
                tlv("58", "VN")                  // Country

        val sanitized = description?.let { sanitizeNote(it) }
        if (!sanitized.isNullOrBlank()) {
            payload += tlv("62", tlv("08", sanitized))                 // 62/08 = transfer note
        }

        payload += "6304"                                              // CRC tag (id=63, len=04)
        return payload + crc16Ccitt(payload)
    }

    /** EMV TLV: 2-char tag + 2-digit length + value (length is char count of value). */
    private fun tlv(tag: String, value: String): String {
        require(value.length <= 99) { "TLV value too long for tag $tag (${value.length} chars)" }
        return tag + "%02d".format(value.length) + value
    }

    /**
     * CRC-16/CCITT-FALSE: polynomial 0x1021, init 0xFFFF, no reflection,
     * no final XOR — this is what Napas requires.
     */
    internal fun crc16Ccitt(data: String): String {
        var crc = 0xFFFF
        for (b in data.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return "%04X".format(crc)
    }

    /**
     * Strip Vietnamese diacritics + non-ASCII so the transfer note encodes
     * cleanly. Bank apps tolerate accents on display but some legacy systems
     * mangle them — safer to keep ASCII for references.
     */
    private fun sanitizeNote(s: String): String {
        val noAccents = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
        return noAccents.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }
            .trim()
            .take(25)
    }
}
