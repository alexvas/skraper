package aikisib.contact7

import kotlinx.serialization.Serializable

@Suppress("ConstructorParameterNaming")
@Serializable
data class Feedback(
    val contact_form_id: Int,
    val status: String,
    val message: String,
    val posted_data_hash: String,
    val into: String,
    val invalid_fields: List<InvalidField>,
) {
    companion object {
        fun mailSent(
            contactFormId: Int,
            pageId: Int,
        ) = Feedback(
            contact_form_id = contactFormId,
            status = "mail_sent",
            message = "Спасибо за Ваше сообщение. Оно успешно отправлено.",
            posted_data_hash = "",
            into = "#" + tag(contactFormId, pageId),
            invalid_fields = listOf(),
        )

        fun validationFailed(
            contactFormId: Int,
            pageId: Int,
            failed: List<FormValidationError>,
        ): Feedback {
            val tag = tag(contactFormId, pageId)
            return Feedback(
                contact_form_id = contactFormId,
                status = "validation_failed",
                message = "Одно или несколько полей содержат ошибочные данные. " +
                    "Пожалуйста, проверьте их и попробуйте ещё раз.",
                posted_data_hash = "",
                into = "#$tag",
                invalid_fields = failed.map { InvalidField(tag, it) },
            )
        }

        fun spam(
            contactFormId: Int,
            pageId: Int,
        ) = Feedback(
            contact_form_id = contactFormId,
            status = "spam",
            message = "При отправке сообщения произошла ошибка. " +
                "Пожалуйста, попробуйте ещё раз позже.",
            posted_data_hash = "",
            into = "#" + tag(contactFormId, pageId),
            invalid_fields = listOf(),
        )

        private fun tag(contactFormId: Int, pageId: Int) =
            "wpcf7-f$contactFormId-p$pageId-o1"

        fun aborted(contactFormId: Int, pageId: Int) = Feedback(
            contact_form_id = contactFormId,
            status = "aborted",
            message = "При отправке сообщения произошла ошибка. " +
                "Пожалуйста, попробуйте ещё раз позже.",
            posted_data_hash = "",
            into = "#" + tag(contactFormId, pageId),
            invalid_fields = listOf(),
        )
    }
}

data class FormValidationError(
    val fieldName: String,
    val message: String,
)

@Suppress("ConstructorParameterNaming")
@Serializable
data class InvalidField(
    val into: String,
    val message: String,
    val idref: String?,
    val error_id: String,
) {
    constructor(tag: String, error: FormValidationError) : this(
        into = "span.wpcf7-form-control-wrap.${error.fieldName}",
        message = error.message,
        idref = null,
        error_id = "$tag-ve-${error.fieldName}",
    )
}
