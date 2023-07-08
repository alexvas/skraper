const inputErrorClass = 't-input-error';

const sendStyle = 't-btn_sending';

$(function () {
    const form = $('#f-form');
    const button = $('button').first();
    const validationErrorMessage = $('#validationErrorMessage');
    const thanksMessage = $('#thanks');
    const failedMessage = $('#failed');

    function validateAndSerializeApplicationForm() {
        const emailInput = $('#email');
        const phoneInput = $('input[name="Phone"]');
        const captchaContainer = $("#captcha-container");
        const emailDiv = emailInput.closest('.t-input-block');
        const phoneDiv = phoneInput.closest('.t-input-block');
        emailDiv.removeClass(inputErrorClass);
        phoneDiv.removeClass(inputErrorClass);
        captchaContainer.removeClass(inputErrorClass);
        validationErrorMessage.hide();
        thanksMessage.hide();
        failedMessage.hide();
        const email = emailInput.val();
        const phone = phoneInput.val();
        const emailRegex = /^(?!\.)(?!.*\.\.)[a-zA-Zёа-яЁА-Я0-9\u2E80-\u2FD5\u3190-\u319f\u3400-\u4DBF\u4E00-\u9FCC\uF900-\uFAAD_.\-+]{0,63}[a-zA-Zёа-яЁА-Я0-9\u2E80-\u2FD5\u3190-\u319f\u3400-\u4DBF\u4E00-\u9FCC\uF900-\uFAAD_\-+]@[a-zA-Zа-яА-ЯЁёäöüÄÖÜßèéû0-9][a-zA-Zа-яА-ЯЁёäöüÄÖÜßèéû0-9.\-]{0,253}\.[a-zA-Zёа-яЁА-Я]{2,10}$/gi;
        const validEmail = emailRegex.test(email);
        const phoneRegex = /^[\d() +-]{7,19}\d$/
        const validPhone = phoneRegex.test(phone);
        const smartToken = $('input[name="smart-token"]').val()
        const validSmartToken = /\S/.test(smartToken);
        if ((validEmail || validPhone) && validSmartToken) {
            const draft = {'name': $('#name').val(), 'phone': phone, 'email': email}
            let output = Object.keys(draft).reduce((newObj, key) => {
                const value = draft[key];
                if (/\S/.test(value)) {
                    newObj[key] = value;
                }
                return newObj;
            }, {});
            let params = (new URL(document.location)).searchParams;
            output['landing'] = params.get('landing');
            output['smart-token'] = smartToken
            return output;
        }
        const someEmail = /\S/.test(email);
        const somePhone = /\S/.test(phone);
        if (someEmail || somePhone) {
            validationErrorMessage.show('slow');
            if (someEmail && !validEmail) {
                emailDiv.addClass(inputErrorClass);
            }
            if (somePhone && !validPhone) {
                phoneDiv.addClass(inputErrorClass);
            }
        }
        if (!validSmartToken) {
            captchaContainer.addClass(inputErrorClass)
        }
        return null;
    }

    function handle_submit(e) {
        button.addClass(sendStyle);
        e.preventDefault();
        const valid = validateAndSerializeApplicationForm();
        if (valid === null) {
            button.removeClass(sendStyle);
            return;
        }
        $.ajax({
            url: '/do_apply', method: 'POST', data: valid
        }).done(function () {
            button.removeClass(sendStyle);
            form.hide();
            const name = valid['name'];
            let message;
            if (/\S/.test(name)) {
                message = 'Спасибо, мы обязательно свяжемся с Вами, ' + valid['name'] + '!';
            } else {
                message = 'Спасибо, мы обязательно свяжемся с Вами!';
            }
            thanksMessage.text(message);
            thanksMessage.show('slow');
        }).fail(function () {
            button.removeClass(sendStyle);
            failedMessage.show('slow');
        })
    }

    form.on('submit', function (e) {
        handle_submit(e);
    });
});

