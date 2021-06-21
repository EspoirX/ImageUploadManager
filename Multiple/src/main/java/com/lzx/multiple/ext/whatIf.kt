package com.lzx.multiple

inline fun <T> T.whatIf(
    given: Boolean?,
    whatIf: T.() -> Unit
): T {
    if (given == true) {
        this.apply { whatIf() }
    }
    return this
}

inline fun Boolean?.whatIf(
    whatIf: () -> Unit,
    whatIfNot: () -> Unit
): Boolean? {
    if (this == true) {
        whatIf()
    } else {
        whatIfNot()
    }
    return this
}

inline fun Boolean?.whatIf(
    whatIf: () -> Unit
): Boolean? {
    return this.whatIf(
        whatIf = whatIf,
        whatIfNot = { }
    )
}

inline fun <T> T.whatIf(
    given: (T) -> Boolean,
    whatIf: () -> Unit
): T {
    if (given(this)) {
        whatIf()
    }
    return this
}

inline fun <T> T.whatIf(
    given: (T) -> Boolean,
    whatIf: () -> Unit,
    whatIfNot: () -> Unit
): T {
    if (given(this)) {
        whatIf()
    } else {
        whatIfNot()
    }
    return this
}

inline fun <T, R> T.whatIfMap(
    given: Boolean?,
    default: R,
    whatIf: (T) -> R
): R {
    return this.whatIfMap(
        given = given,
        whatIf = whatIf,
        whatIfNot = { default }
    )
}

inline fun <T, R> T.whatIfMap(
    given: Boolean?,
    whatIf: (T) -> R,
    whatIfNot: (T) -> R
): R {
    if (given == true) {
        return whatIf(this)
    }
    return whatIfNot(this)
}

inline fun tryCatch(
    tryFun: () -> Unit,
    catchFun: (ex: Exception) -> Unit,
    finallyFun: () -> Unit
) {
    try {
        tryFun()
    } catch (ex: Exception) {
        catchFun(ex)
    } finally {
        finallyFun()
    }
}

inline fun tryCatch(
    tryFun: () -> Unit,
    catchFun: (ex: Exception) -> Unit
) {
    tryCatch(tryFun, catchFun, finallyFun = {})
}

inline fun tryCatch(
    tryFun: () -> Unit
) {
    tryCatch(tryFun, catchFun = {}, finallyFun = {})
}