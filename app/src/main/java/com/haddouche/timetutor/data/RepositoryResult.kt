package com.haddouche.timetutor.data

// Wrapper para resultados que pueden ser éxito o error
// Uso esto para un manejo de errores más limpio y consistente
sealed class RepositoryResult<out T> {

    data class Success<T>(val data: T) : RepositoryResult<T>()

    data class Error(val error: RepositoryError) : RepositoryResult<Nothing>()

    // Funciones helper para chequear el estado
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    // Obtener el valor si es éxito, null si es error
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    // Obtener el error si es error, null si es éxito
    fun errorOrNull(): RepositoryError? = when (this) {
        is Success -> null
        is Error -> error
    }

    // Ejecutar bloque si es éxito
    inline fun onSuccess(action: (T) -> Unit): RepositoryResult<T> {
        if (this is Success) action(data)
        return this
    }

    // Ejecutar bloque si es error
    inline fun onError(action: (RepositoryError) -> Unit): RepositoryResult<T> {
        if (this is Error) action(error)
        return this
    }
}
