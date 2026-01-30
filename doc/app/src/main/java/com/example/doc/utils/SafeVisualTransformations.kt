package com.example.doc.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SafeDecimalVisualTransformation(private val format: String = "00.0") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val filtered = text.text.filter { it.isDigit() || it == '.' }
        val display = if (filtered.isNotEmpty()) filtered else format
        val length = filtered.length
        return TransformedText(
            AnnotatedString(display),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (length == 0) return 0
                    return offset.coerceAtMost(display.length)
                }
                override fun transformedToOriginal(offset: Int): Int {
                    if (length == 0) return 0
                    return offset.coerceAtMost(length)
                }
            }
        )
    }
}

class SafePressureVisualTransformation(private val format: String = "000/00") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val display = when {
            digits.isEmpty() -> format
            digits.length <= 3 -> digits + "/"
            digits.length in 4..5 -> digits.substring(0, 3) + "/" + digits.substring(3)
            digits.length > 5 -> digits.substring(0, 3) + "/" + digits.substring(3, 5)
            else -> format
        }
        val length = digits.length
        return TransformedText(
            AnnotatedString(display),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (length == 0) return 0
                    return offset.coerceAtMost(display.length)
                }
                override fun transformedToOriginal(offset: Int): Int {
                    if (length == 0) return 0
                    return offset.coerceAtMost(length)
                }
            }
        )
    }
}

class SafeIntegerVisualTransformation(private val format: String = "000") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val filtered = text.text.filter { it.isDigit() }
        val display = if (filtered.isNotEmpty()) filtered else format
        val length = filtered.length
        return TransformedText(
            AnnotatedString(display),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (length == 0) return 0
                    return offset.coerceAtMost(display.length)
                }
                override fun transformedToOriginal(offset: Int): Int {
                    if (length == 0) return 0
                    return offset.coerceAtMost(length)
                }
            }
        )
    }
}