package com.qrhealthcare.app.data.payment

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  EDIT THIS FILE WITH YOUR REAL BANK INFO BEFORE RECEIVING REAL PAYMENTS  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  The values below are placeholders. With these, the VietQR will still   ║
 * ║  generate and scan, but the money will go nowhere (account doesn't       ║
 * ║  exist). For a working prototype with your own bank, replace            ║
 * ║  [BANK_BIN], [ACCOUNT_NUMBER], and [ACCOUNT_NAME] below.                ║
 * ║                                                                          ║
 * ║  HOW TO FIND YOUR BANK'S BIN:                                            ║
 * ║  Look at any VietQR transfer slip you've received, or check the           ║
 * ║  official Napas list. Common BINs are in [BankBins] below.              ║
 * ║                                                                          ║
 * ║  In production you'd move these to a backend "merchant settings"         ║
 * ║  endpoint so multiple sellers can have their own accounts; for now      ║
 * ║  a single-merchant constant is the simplest workable path.              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
object MerchantConfig {

    /** Napas BIN of the receiving bank. See [BankBins] for common values. */
    const val BANK_BIN: String = "970422"  // ← MB Bank (placeholder)

    /** Receiving account number — no spaces, no separators. */
    const val ACCOUNT_NUMBER: String = "0000000000"  // ← REPLACE with your real account

    /** Human-readable account holder name — shown to the customer as a sanity check. */
    const val ACCOUNT_NAME: String = "QR HEALTHCARE"  // ← Account holder name on file at the bank

    /** Friendly name of the bank, shown in the UI alongside the QR. */
    const val BANK_NAME: String = "MB Bank"
}

/** Common Vietnamese bank Napas BINs — pick the one that matches [MerchantConfig.BANK_BIN]. */
object BankBins {
    const val VIETCOMBANK = "970436"
    const val VIETINBANK  = "970415"
    const val BIDV        = "970418"
    const val AGRIBANK    = "970405"
    const val MBBANK      = "970422"
    const val TECHCOMBANK = "970407"
    const val VPBANK      = "970432"
    const val ACB         = "970416"
    const val TPBANK      = "970423"
    const val SACOMBANK   = "970403"
    const val HDBANK      = "970437"
    const val OCB         = "970448"
    const val SHB         = "970443"
    const val EXIMBANK    = "970431"
    const val MSB         = "970426"
    const val LPBANK      = "970449"
    const val SEABANK     = "970440"
}
