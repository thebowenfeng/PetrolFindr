package com.bowenfeng.petrolfindr.common

import java.security.SecureRandom

class RandomUtils {
    companion object {
        fun generateAndroidIdLike(): String {
            val bytes = ByteArray(8) // 64 bits
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }
}
