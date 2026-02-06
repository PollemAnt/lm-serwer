package com.example

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

object Strings {

    private val locale = Locale("pl")

    private val bundle: ResourceBundle =
        ResourceBundle.getBundle("messages", locale)

    fun get(key: String): String =
        bundle.getString(key)

    fun format(key: String, vararg args: Any): String =
        MessageFormat.format(get(key), *args)
}