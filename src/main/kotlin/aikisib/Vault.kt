package aikisib

import org.aeonbits.owner.Config
import org.aeonbits.owner.Config.DefaultValue
import org.aeonbits.owner.Config.Sources

@Sources("classpath:vault.properties")
interface Vault : Config {
    /**
     * Путь к страничке входа в админку вордпресса.
     */
    @DefaultValue("/wp-admin/")
    fun wordpressLoginPath(): String

    /**
     * Имя пользователя с правами администратора.
     */
    @DefaultValue("admin")
    fun username(): String

    /**
     * Пароль пользователя с правами администратора.
     */
    @DefaultValue("pAsSw0rD")
    fun password(): String
}
