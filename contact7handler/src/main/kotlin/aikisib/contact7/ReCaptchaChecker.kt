package aikisib.contact7

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import mu.KLogging

interface ReCaptchaChecker {

    /**
     * Проверяем, а не спам ли то, что прислал пользователь?
     *
     * @param reCaptchaResponseToken - ответ Гугловой рекапчи, как его прислал пользователь.
     * @return true, если не-спам. false, если не-спам. null, если отослан повторный запрос с валидным токеном.
     */
    suspend fun validate(reCaptchaResponseToken: String): Boolean?
}

internal class ReCaptchaCheckerImpl(
    private val reCaptchaSecret: String,
    private val client: HttpClient,
) : ReCaptchaChecker {
    private val lock = Mutex()
    private val localResultCache = LruCache()

    override suspend fun validate(reCaptchaResponseToken: String) =
        lock.withLock {
            when (localResultCache[reCaptchaResponseToken]) {
                null -> validateWithGoogle(reCaptchaResponseToken).also {
                    localResultCache[reCaptchaResponseToken] = it
                }
                true -> null
                false -> false
            }
        }

    private suspend fun validateWithGoogle(reCaptchaResponseToken: String): Boolean {
        val response = client.post(RECAPTCHA_VERIFICATION_ENDPOINT) {
            parameter("secret", reCaptchaSecret)
            parameter("response", reCaptchaResponseToken)
        }
        if (response.status != HttpStatusCode.OK) {
            logger.warn { "Статус ${response.status} для проверки Капчи." }
            return false
        }

        val parsed: ReCaptchaResponse = response.body()
        return parsed.success
    }

    companion object : KLogging() {
        private const val RECAPTCHA_VERIFICATION_ENDPOINT = "https://www.google.com/recaptcha/api/siteverify"
    }
}

@Serializable
data class ReCaptchaResponse(
    val success: Boolean,
)

private class LruCache : LinkedHashMap<String, Boolean>(MAX_SIZE, LOAD_FACTOR, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
        return size > MAX_SIZE
    }

    companion object {
        private const val MAX_SIZE = 1000
        private const val LOAD_FACTOR = 0.75f
    }
}
