package com.rcsolutions.rcosmobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

private const val TAG = "DocumentTextExtractor"
private const val MAX_TEXT_CHARS = 12_000
private const val MAX_PDF_PAGES = 4
private const val MAX_PDF_DIMENSION = 1280

/**
 * What an uploaded company document turns into for the AI extraction.
 * - [Text]: readable text (plain text, CSV, .docx) — sent as prompt text.
 * - [Images]: page/photo bitmaps (PDF pages, images) — sent as image parts.
 * - [Unsupported]: the file could not be read on the phone; the reason
 *   is shown to the owner so they can save it as PDF or paste the text.
 *
 * Nothing is uploaded anywhere: extraction runs on-device into the
 * same Firebase AI Logic call the photo path already uses.
 */
sealed interface DocumentInput {
    data class Text(val text: String, val label: String) : DocumentInput
    data class Images(val bitmaps: List<Bitmap>, val label: String) : DocumentInput
    data class Unsupported(val reason: String) : DocumentInput
}

suspend fun extractDocumentInput(
    context: Context,
    uri: Uri,
    mimeType: String?,
    displayName: String?
): DocumentInput = withContext(Dispatchers.IO) {
    val type = (mimeType ?: "").lowercase()
    val name = (displayName ?: "").lowercase()
    val label = displayName?.takeIf { it.isNotBlank() } ?: "Document"
    try {
        when {
            type.startsWith("text/") || name.endsWith(".txt") || name.endsWith(".csv") -> {
                val text = readTextCapped(context, uri)
                if (text.isBlank()) DocumentInput.Unsupported("That file has no readable text.")
                else DocumentInput.Text(text, label)
            }
            type == "application/pdf" || name.endsWith(".pdf") -> {
                val pages = renderPdfPages(context, uri)
                if (pages.bitmaps.isEmpty()) DocumentInput.Unsupported("That PDF has no readable pages.")
                else DocumentInput.Images(pages.bitmaps, "$label · ${pages.totalPages} page(s), first ${pages.bitmaps.size} read")
            }
            type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                name.endsWith(".docx") -> {
                val text = extractDocxText(context, uri)
                if (text.isBlank()) DocumentInput.Unsupported("That Word file has no readable text.")
                else DocumentInput.Text(text, label)
            }
            type.startsWith("image/") -> {
                val bitmap = decodeImage(context, uri)
                if (bitmap == null) DocumentInput.Unsupported("That image could not be opened.")
                else DocumentInput.Images(listOf(bitmap), label)
            }
            type == "application/msword" || name.endsWith(".doc") -> {
                DocumentInput.Unsupported(
                    "This older Word format can't be read on the phone. " +
                        "Save it as PDF, or paste the text into the box above."
                )
            }
            else -> DocumentInput.Unsupported(
                "This file type can't be read here. Upload a PDF, Word, text, or image file — or paste the text into the box above."
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Document extraction failed for $label", e)
        DocumentInput.Unsupported("That file could not be read. Try saving it as PDF, or paste the text into the box above.")
    }
}

private fun readTextCapped(context: Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        val sb = StringBuilder()
        val buffer = CharArray(4096)
        while (sb.length < MAX_TEXT_CHARS) {
            val read = reader.read(buffer)
            if (read <= 0) break
            sb.append(buffer, 0, read)
        }
        return sb.toString().take(MAX_TEXT_CHARS).trim()
    }
    return ""
}

private data class PdfPages(val bitmaps: List<Bitmap>, val totalPages: Int)

private fun renderPdfPages(context: Context, uri: Uri): PdfPages {
    val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return PdfPages(emptyList(), 0)
    pfd.use {
        val renderer = PdfRenderer(it)
        renderer.use { pdf ->
            val total = pdf.pageCount
            val bitmaps = mutableListOf<Bitmap>()
            val pages = minOf(total, MAX_PDF_PAGES)
            for (i in 0 until pages) {
                pdf.openPage(i).use { page ->
                    val scale = MAX_PDF_DIMENSION.toFloat() / maxOf(page.width, page.height).toFloat()
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                }
            }
            return PdfPages(bitmaps, total)
        }
    }
}

/** .docx is a zip: the text lives in word/document.xml. Tags are
 * stripped, paragraph ends become line breaks, entities unescaped. */
private fun extractDocxText(context: Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zip.readBytes().toString(Charsets.UTF_8)
                    val withBreaks = xml
                        .replace("</w:p>", "\n")
                        .replace(Regex("<[^>]+>"), " ")
                    return unescapeXml(withBreaks)
                        .replace(Regex("[ \\t]+"), " ")
                        .replace(Regex("\\n\\s*\\n+"), "\n")
                        .trim()
                        .take(MAX_TEXT_CHARS)
                }
                entry = zip.nextEntry
            }
        }
    }
    return ""
}

private fun unescapeXml(text: String): String =
    text.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }

private fun decodeImage(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } else {
        @Suppress("DEPRECATION")
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
}.getOrNull()
