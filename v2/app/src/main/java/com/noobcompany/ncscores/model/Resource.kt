package com.noobcompany.ncscores.model

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val exception: Throwable) : Resource<Nothing>
    object Loading : Resource<Nothing>
}
