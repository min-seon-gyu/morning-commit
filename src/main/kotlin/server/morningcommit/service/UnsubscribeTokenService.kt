package server.morningcommit.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class UnsubscribeTokenService(
    @Value("\${app.unsubscribe-secret}")
    private val secret: String
) {

    fun generateToken(email: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val signature = mac.doFinal(email.toByteArray())

        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }

    fun validateToken(email: String, token: String): Boolean {
        val expected = generateToken(email)

        return expected == token
    }
}
