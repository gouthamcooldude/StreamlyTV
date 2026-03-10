package com.streamlytv.utils

import com.streamlytv.data.model.Channel
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object M3UParser {

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(content))
        var line: String?
        var currentInfo: String? = null

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()

            when {
                trimmed.startsWith("#EXTINF:") -> {
                    currentInfo = trimmed
                }
                trimmed.isNotEmpty() && !trimmed.startsWith("#") && currentInfo != null -> {
                    parseChannel(currentInfo!!, trimmed)?.let { channels.add(it) }
                    currentInfo = null
                }
            }
        }

        return channels
    }

    private fun parseChannel(info: String, url: String): Channel? {
        return try {
            val name = extractName(info)
            val group = extractAttribute(info, "group-title") ?: ""
            val logo = extractAttribute(info, "tvg-logo") ?: ""
            val epgId = extractAttribute(info, "tvg-id") ?: ""
            val tvgName = extractAttribute(info, "tvg-name") ?: name

            val language = Channel.detectLanguage(name, group)
            val is4K = Channel.detect4K(name, group, url)
            val is51 = Channel.detect51(name, group)

            val id = epgId.ifEmpty { UUID.nameUUIDFromBytes("$name$url".toByteArray()).toString() }

            Channel(
                id = id,
                name = name,
                url = url,
                group = group,
                logo = logo,
                language = language,
                epgId = epgId.ifEmpty { tvgName },
                is4K = is4K,
                is51 = is51
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractName(info: String): String {
        val commaIndex = info.lastIndexOf(',')
        return if (commaIndex >= 0) info.substring(commaIndex + 1).trim() else ""
    }

    private fun extractAttribute(info: String, attr: String): String? {
        val pattern = Regex("""$attr="([^"]*?)"""")
        return pattern.find(info)?.groupValues?.get(1)
    }
}
