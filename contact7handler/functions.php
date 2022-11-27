<?php

/*
 * Добавьте это к функциям плагина contact-form-7
 * wp-content/plugins/contact-form-7/includes/functions.php
 * и в ваши формы [ya_captcha]
 */

function init_yandex_smart_captcha() {
  wpcf7_add_form_tag( 'ya_captcha', 'yandex_captcha_handler' ); // "ya_captcha" is the type of the form-tag
}

function yandex_captcha_handler( $tag ) {
  return '<div
        class="smart-captcha"
        data-sitekey="<Ключ_клиентской_части>"
        style = "margin-bottom: 20px;"
></div>';
}

add_action( 'wpcf7_init', 'init_yandex_smart_captcha' );
