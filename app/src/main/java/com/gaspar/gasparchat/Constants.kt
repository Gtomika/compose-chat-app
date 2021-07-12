package com.gaspar.gasparchat

/**
 * Tag for logging.
 */
const val TAG = "GasparChat"

/**
 * Navigation destination constants.
 */
object NavDest {

    const val LOGIN = "login"

    const val REGISTER = "register"

    const val HOME = "home"

    const val PROFILE = "profile"

    const val SEARCH = "search"
}

/**
 * Limits of password length.
 */
object PasswordLimit {

    const val MIN = 6 //at least 6 is required by FIREBASE AUTH

    const val MAX = 30

}

object NameLimits {

    const val MIN = 3

    const val MAX = 30

}