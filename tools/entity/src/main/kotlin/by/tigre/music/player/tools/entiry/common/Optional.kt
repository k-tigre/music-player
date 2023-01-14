package by.tigre.music.player.tools.entiry.common

sealed class Optional<out T : Any> {
    abstract fun toNullable(): T?
    abstract fun doIfSome(block: (T) -> Unit)

    data class Some<out T : Any>(val value: T) : Optional<T>() {
        override fun toString() = "Some($value)"
        override fun toNullable(): T = value
        override fun doIfSome(block: (T) -> Unit) = block(value)
    }

    object None : Optional<Nothing>() {
        override fun toString() = "None"
        override fun toNullable(): Nothing? = null
        override fun doIfSome(block: (Nothing) -> Unit) = Unit
    }

    inline fun process(onSome: (T) -> Unit, onNone: () -> Unit) =
        when (this) {
            is Some -> onSome(value)
            is None -> onNone()
        }
}

fun <T : Any> T?.toOptional(): Optional<T> = if (this == null) Optional.None else Optional.Some(this)
