package com.pwitko.common

sealed interface IResult<out T>
object Loading: IResult<Nothing>
data class Success<out T>(val data: T): IResult<T>
data class Failure(val reason: Throwable): IResult<Nothing>
