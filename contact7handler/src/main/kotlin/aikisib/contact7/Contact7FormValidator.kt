package aikisib.contact7

interface Contact7FormValidator {
    /**
     * Валидирует переданные параметры формы.
     * Возвращает либо пустой список, если все параметры валидные.
     * Либо непустой список какие параметры и почему некорректные.
     */
    fun validate(formParameters: Map<String, String?>): List<FormValidationError>
}

object Contact7FormValidatorImpl : Contact7FormValidator {

    override fun validate(formParameters: Map<String, String?>): List<FormValidationError> {
        val output = mutableListOf<FormValidationError>()
        if (formParameters["wpgdprc"] != "1") {
            output += FormValidationError(
                fieldName = "wpgdprc",
                message = "Примите правила приватности.",
            )
        }
        formParameters.validate(
            fieldName = "your-name",
            minLength = nameOrContactsMinLength,
            maxLength = nameOrContactsMaxLength,
            required = true,
        )
            ?.let { output += it }

        formParameters.validate(
            fieldName = "your-contacts",
            minLength = nameOrContactsMinLength,
            maxLength = nameOrContactsMaxLength,
            required = true,
        )
            ?.let { output += it }

        formParameters.validate(
            fieldName = "your-subject",
            minLength = subjectOrMessageOrCaptchaMinLength,
            maxLength = subjectOrMessageOrCaptchaMaxLength,
            required = true,
        )
            ?.let { output += it }

        formParameters.validate(
            fieldName = "your-message",
            minLength = subjectOrMessageOrCaptchaMinLength,
            maxLength = subjectOrMessageOrCaptchaMaxLength,
        )
            ?.let { output += it }

        formParameters.validate(
            fieldName = "_wpcf7_recaptcha_response",
            minLength = subjectOrMessageOrCaptchaMinLength,
            maxLength = subjectOrMessageOrCaptchaMaxLength,
            required = true,
        )
            ?.let { output += it }

        return output
    }

    @Suppress("ReturnCount")
    private fun Map<String, String?>.validate(
        fieldName: String,
        minLength: Int,
        maxLength: Int,
        required: Boolean = false,
    ): FormValidationError? {
        fun error(message: String) =
            FormValidationError(
                fieldName = fieldName,
                message = message,
            )

        val content = this[fieldName]

        if (content.isNullOrBlank()) {
            if (required) {
                return error("Поле обязательно для заполнения.")
            }
            return null
        }

        val length = content.length
        if (length < minLength) {
            return error("Поле слишком короткое.")
        }
        if (length > maxLength) {
            return error("Поле слишком длинное.")
        }
        return null
    }

    private const val nameOrContactsMinLength = 3
    private const val nameOrContactsMaxLength = 255

    private const val subjectOrMessageOrCaptchaMinLength = 10
    private const val subjectOrMessageOrCaptchaMaxLength = 1024
}
