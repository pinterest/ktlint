package com.pinterest.ktlint.internal

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Generate hex string for given [ByteArray] content.
 */
internal val ByteArray.hex get() = BigInteger(MessageDigest.getInstance("SHA-256").digest(this)).toString(16)
