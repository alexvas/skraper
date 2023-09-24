package aikisib.contact7

object Timeouts {
    // client
    const val SOCKET_TIMEOUT_MS = 10_000
    const val CONNECT_TIMEOUT_MS = 10_000
    const val CONNECTION_REQUEST_TIMEOUT_MS = 20_000

    // server
    const val REQUEST_READ_TIMEOUT_S = 10
    const val RESPONSE_WRITE_TIMEOUT_S = 10
}
