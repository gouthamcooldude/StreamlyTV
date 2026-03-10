package com.streamlytv.utils

import android.util.Xml
import com.streamlytv.data.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

object EpgParser {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(xmlContent: String): Map<String, List<EpgProgram>> {
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentProgram: TempProgram? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                currentProgram = TempProgram(
                                    channelId = parser.getAttributeValue(null, "channel") ?: "",
                                    start = parseDate(parser.getAttributeValue(null, "start")),
                                    stop = parseDate(parser.getAttributeValue(null, "stop"))
                                )
                            }
                            "title" -> {
                                if (currentProgram != null) {
                                    currentProgram.title = parser.nextText()
                                }
                            }
                            "desc" -> {
                                if (currentProgram != null) {
                                    currentProgram.description = parser.nextText()
                                }
                            }
                            "category" -> {
                                if (currentProgram != null) {
                                    currentProgram.category = parser.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && currentProgram != null) {
                            val prog = currentProgram
                            if (prog.channelId.isNotEmpty() && prog.title.isNotEmpty()) {
                                val epg = EpgProgram(
                                    channelId = prog.channelId,
                                    title = prog.title,
                                    description = prog.description,
                                    startTime = prog.start,
                                    endTime = prog.stop,
                                    category = prog.category
                                )
                                programs.getOrPut(prog.channelId) { mutableListOf() }.add(epg)
                            }
                            currentProgram = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return programs
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        return try {
            dateFormat.parse(dateStr.trim())?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private data class TempProgram(
        val channelId: String,
        val start: Long,
        val stop: Long,
        var title: String = "",
        var description: String = "",
        var category: String = ""
    )
}
