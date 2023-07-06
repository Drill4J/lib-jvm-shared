package mu.internal

public actual object ErrorMessageProducer {
    public actual fun getErrorLog(e: Throwable): String = "Log message invocation failed: $e"
}
