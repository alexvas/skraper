package aikisib.contact7

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import mu.KLogging

interface CaptchaChecker {

    /**
     * Проверяем, а не спам ли то, что прислал пользователь?
     *
     * @param captchaResponseToken - ответ Яндексовой капчи, как его прислал пользователь.
     * @return true, если не-спам. false, если не-спам. null, если отослан повторный запрос с валидным токеном.
     */
    suspend fun validate(captchaResponseToken: String, ipAddress: String, dataHash: String): Boolean?
}

internal class CaptchaCheckerImpl(
    private val yaCaptchaSecret: String,
    private val client: HttpClient,
) : CaptchaChecker {
    private val lock = Mutex()
    private val localResultCache = LruResultCache<ResultForIp>()
    private val seenCache = LruResultCache<Int>()

    override suspend fun validate(captchaResponseToken: String, ipAddress: String, dataHash: String) =
        lock.withLock {
            logger.trace { "Проверяем токен $captchaResponseToken для ip $ipAddress" }
            val seen = seenCache.putIfAbsent(ipAddress, 1)
            if (seen != null) {
                seenCache[ipAddress] = seen + 1
                if (seen > MAX_SEEN) {
                    // ограничиваем сообщения с одного и того же IP-адреса
                    logger.trace { "Превышен лимит сообщений с адреса $ipAddress" }
                    return@withLock false
                }
            }

            val savedResult = localResultCache[captchaResponseToken]
            when {
                // Новый токен.
                savedResult == null -> validateWithYandex(captchaResponseToken, ipAddress).also { yaDecision ->
                    localResultCache[captchaResponseToken] = ResultForIp(ipAddress, dataHash, yaDecision)
                }
                !savedResult.result -> {
                    logger.debug { "Перед этим результат для $ipAddress и токена был отрицательным" }
                    false
                }
                dataHash == savedResult.dataHash -> {
                    logger.debug { "Дублирующая отправка той же формы для $ipAddress" }
                    null
                }
                ipAddress != savedResult.ipAddress -> {
                    logger.debug { "Подделаный токен. Уже использованный токен c IP-адреса ${savedResult.ipAddress} попробовали использовать через $ipAddress." }
                    false
                }
                else -> {
                    logger.debug { "Скорректированная форма с тем же токеном" }
                    true
                }
            }
        }

    @Suppress("ReturnCount")
    private suspend fun validateWithYandex(captchaResponseToken: String, ipAddress: String): Boolean {
        val response = client.get(makeGetUrl(ipAddress, captchaResponseToken))
        if (response.status != HttpStatusCode.OK) {
            logger.warn { "Статус ${response.status} для проверки Капчи." }
            return false
        }

        val parsed: CaptchaResponse = response.body()
        if (parsed.status != "ok") {
            logger.info { "Валидация провалена. ${parsed.message}" }
            return false
        }
        logger.info { "Яндекс валидировал ОК. ${parsed.message}" }
        return true
    }

    private fun makeGetUrl(ipAddress: String, token: String) =
        "https://captcha-api.yandex.ru/validate?secret=$yaCaptchaSecret&ip=$ipAddress&token=$token"

    companion object : KLogging() {
        // не больше 5 сообщений с одного IP-адреса
        private const val MAX_SEEN = 5
    }
}

@Serializable
data class CaptchaResponse(
    val status: String,
    val message: String?,
)

private class LruResultCache<T> : LinkedHashMap<String, T>(MAX_SIZE, LOAD_FACTOR, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>?): Boolean {
        return size > MAX_SIZE
    }

    companion object {
        private const val MAX_SIZE = 1000
        private const val LOAD_FACTOR = 0.75f
    }
}

private data class ResultForIp(
    val ipAddress: String,
    val dataHash: String,
    val result: Boolean,
)
