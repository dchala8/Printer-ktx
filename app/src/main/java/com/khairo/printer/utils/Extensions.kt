package com.khairo.printer.utils

import android.annotation.SuppressLint
import android.util.Log
import com.khairo.coroutines.CoroutinesEscPosPrinter
import java.text.SimpleDateFormat
import java.util.*

fun String.replaceNonstandardDigits(): String {
    if (this.isEmpty()) {
        return this
    }
    val builder = StringBuilder()
    for (element in this) {
        if (element.isNonstandardDigit()) {
            val numericValue = Character.getNumericValue(element)
            if (numericValue >= 0) {
                builder.append(numericValue)
            }
        } else {
            builder.append(element)
        }
    }
    return builder.toString()
}

fun Char.isNonstandardDigit(): Boolean {
    return Character.isDigit(this) && this !in '0'..'9'
}

@SuppressLint("SimpleDateFormat")
fun String.getDateTime(): String = SimpleDateFormat(this).format(Date()).replaceNonstandardDigits()


@SuppressLint("UseCompatLoadingForDrawables")
fun printViaWifi(
    printer: CoroutinesEscPosPrinter,
    body: String
): CoroutinesEscPosPrinter {
    Log.d("PRINTER",printer.toString())
    return printer.setTextToPrint(body)
}
