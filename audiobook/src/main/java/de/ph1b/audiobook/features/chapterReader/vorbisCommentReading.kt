package de.ph1b.audiobook.features.chapterReader

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import java.io.InputStream

/**
 * Reads vorbis comment according to [specification](https://xiph.org/vorbis/doc/v-comment.html)
 */
fun readVorbisComment(stream: InputStream): VorbisComment {
  val vendorLength = stream.readLeUInt32()
  val vendor = stream.readBytes(vendorLength.toInt()).toString(Charsets.UTF_8)
  val numberComments = stream.readLeUInt32()
  val comments = (1..numberComments).map {
    val length = stream.readLeUInt32()
    val comment = stream.readBytes(length.toInt()).toString(Charsets.UTF_8)
    val parts = comment.split("=", limit = 2)
    if (parts.size != 2) throw VorbisCommentParseException("Expected TAG=value comment format")
    Pair(parts[0].toUpperCase(), parts[1])
  }.toMap()
  return VorbisComment(vendor, comments)
}

class VorbisCommentParseException(message: String) : Exception(message)

data class VorbisComment(val vendor: String, val comments: Map<String, String>) {
  /**
   * Chapters extracted according to https://wiki.xiph.org/Chapter_Extension
   */
  val chapters: SparseArray<String>
    get() {
      val chapters = SparseArray<String>()
      var i = 1
      while (true) {
        val iStr = i.toString().padStart(3, '0')
        val timeStr = comments["CHAPTER$iStr"] ?: break
        val name = comments["CHAPTER${iStr}NAME"] ?: return emptySparseArray()
        val time = parseVorbisCommentChapterTime(timeStr) ?: return emptySparseArray()
        chapters.put(time, name)
        ++i
      }
      return chapters
    }
}

private val VORBIS_COMMENT_CHAPTER_TIME_REGEX = Regex("""(\d+):(\d+):(\d+).(\d+)""")

internal fun parseVorbisCommentChapterTime(timeStr: String): Int? {
  val matchResult = VORBIS_COMMENT_CHAPTER_TIME_REGEX.matchEntire(timeStr) ?: return null
  val hours = matchResult.groups[1]!!.value.toInt()
  val minutes = hours * 60 + matchResult.groups[2]!!.value.toInt()
  val seconds = minutes * 60 + matchResult.groups[3]!!.value.toInt()
  val milliseconds = seconds * 1000 + matchResult.groups[4]!!.value.take(3).padEnd(3, '0').toInt()
  return milliseconds
}
