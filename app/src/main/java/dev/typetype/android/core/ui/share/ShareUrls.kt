package dev.typetype.android.core.ui.share

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.compositionLocalOf
import dev.typetype.android.R
import dev.typetype.android.domain.navigation.resolveIncomingVideoUrl
import dev.typetype.android.domain.navigation.toPublicWatchParameter
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val LocalServerBaseUrl = compositionLocalOf<String?> { null }

fun buildShareUrl(serverBaseUrl: String?, videoUrl: String): String {
    if (serverBaseUrl.isNullOrBlank()) return videoUrl
    val origin = serverBaseUrl
        .trimEnd('/')
        .removeSuffix("/api")
        .trimEnd('/')
    if (origin.isBlank()) return videoUrl
    val encoded = URLEncoder.encode(toPublicWatchParameter(videoUrl), StandardCharsets.UTF_8.toString())
    return "$origin/watch?v=$encoded"
}

fun buildSourceShareUrl(videoUrl: String): String =
    resolveIncomingVideoUrl(videoUrl) ?: videoUrl.trim()

fun showShareChooser(
    context: Context,
    serverBaseUrl: String?,
    videoUrl: String,
    chooserTitle: String,
) {
    val sourceUrl = buildSourceShareUrl(videoUrl)
    val typeTypeUrl = buildShareUrl(serverBaseUrl, videoUrl)
    val choices = buildList {
        add(
            ShareChoice(
                context.getString(R.string.video_menu_share_typetype),
                typeTypeUrl,
                android.R.drawable.ic_menu_share,
            ),
        )
        providerChoice(context, sourceUrl)?.takeIf { it.url != typeTypeUrl }?.let(::add)
    }
    AlertDialog.Builder(context)
        .setTitle(R.string.video_menu_share)
        .setAdapter(ShareChoiceAdapter(context, choices)) { _, which ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, choices[which].url)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        }
        .show()
}

private data class ShareChoice(val label: String, val url: String, val icon: Int)

private fun providerChoice(context: Context, sourceUrl: String): ShareChoice? {
    val provider = sourceUrl.lowercase()
    return when {
        "youtube.com" in provider || "youtu.be" in provider -> ShareChoice(
            context.getString(R.string.video_menu_share_source, "YouTube"),
            sourceUrl,
            R.drawable.ic_service_youtube,
        )
        "nicovideo.jp" in provider || "nico.ms" in provider -> ShareChoice(
            context.getString(R.string.video_menu_share_source, "NicoNico"),
            sourceUrl,
            R.drawable.ic_service_niconico,
        )
        "bilibili.com" in provider || "b23.tv" in provider -> ShareChoice(
            context.getString(R.string.video_menu_share_source, "BiliBili"),
            sourceUrl,
            R.drawable.ic_service_bilibili,
        )
        else -> null
    }
}

private class ShareChoiceAdapter(
    private val context: Context,
    private val choices: List<ShareChoice>,
) : BaseAdapter() {
    override fun getCount(): Int = choices.size
    override fun getItem(position: Int): ShareChoice = choices[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val choice = getItem(position)
        val density = context.resources.displayMetrics.density
        val row = (convertView as? LinearLayout) ?: LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ColorDrawable(Color.TRANSPARENT)
            val icon = ImageView(context)
            addView(icon, LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt()))
            val label = TextView(context).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Body1)
            }
            addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        row.setPadding((20 * density).toInt(), (12 * density).toInt(), (20 * density).toInt(), (12 * density).toInt())
        (row.getChildAt(0) as ImageView).setImageResource(choice.icon)
        (row.getChildAt(1) as TextView).text = choice.label
        return row
    }
}

fun buildImageUrl(serverBaseUrl: String?, imageUrl: String): String {
    val source = imageUrl.trim().let { value ->
        if (value.startsWith("httpss://")) "https://${value.removePrefix("httpss://")}" else value
    }
    val base = serverBaseUrl?.trim()?.trimEnd('/')
    if (source.isBlank() || base.isNullOrBlank()) return source

    val origin = base.removeSuffix("/api")
    if (source.startsWith('/')) return "$origin$source"
    if (source.startsWith("$origin/")) return source
    if (!source.startsWith("http://") && !source.startsWith("https://")) return source
    if (!needsImageProxy(source)) return source

    val encoded = URLEncoder.encode(source, StandardCharsets.UTF_8.toString())
    return "$base/proxy?url=$encoded"
}

private fun needsImageProxy(source: String): Boolean {
    val host = runCatching { URI(source).host?.lowercase() }.getOrNull() ?: return false
    return host.endsWith("ggpht.com") ||
        host.endsWith("googleusercontent.com") ||
        host.endsWith("hdslb.com") ||
        host.endsWith("ytimg.com")
}
