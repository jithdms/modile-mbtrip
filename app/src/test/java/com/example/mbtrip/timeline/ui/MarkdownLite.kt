package com.example.mbtrip.timeline.ui

/*
 * '할 일' 마크다운 — 외부 라이브러리 없이 필요한 문법만 지원.
 *   # 제목 / ## 작은 제목
 *   - 목록
 *   - [ ] 체크리스트 / - [x] 완료
 *   **굵게** (줄 안 어디서나)
 * Compose 와 무관한 순수 로직이라 JUnit 으로 테스트한다. 그리기는 MarkdownView.kt.
 */

internal sealed interface MdLine {
    val lineIndex: Int

    data class Heading(override val lineIndex: Int, val level: Int, val text: String) : MdLine
    data class Check(override val lineIndex: Int, val checked: Boolean, val text: String) : MdLine
    data class Bullet(override val lineIndex: Int, val text: String) : MdLine
    data class Paragraph(override val lineIndex: Int, val text: String) : MdLine
    data class Blank(override val lineIndex: Int) : MdLine
}

private val CHECK = Regex("""^\s*[-*]\s+\[([ xX])]\s?(.*)$""")
private val BULLET = Regex("""^\s*[-*]\s+(.*)$""")
private val HEADING = Regex("""^(#{1,3})\s+(.*)$""")

internal fun parseMarkdown(md: String): List<MdLine> =
    md.lines().mapIndexed { i, raw ->
        val line = raw.trimEnd()
        CHECK.matchEntire(line)?.let { m ->
            return@mapIndexed MdLine.Check(i, m.groupValues[1].lowercase() == "x", m.groupValues[2])
        }
        HEADING.matchEntire(line)?.let { m ->
            return@mapIndexed MdLine.Heading(i, m.groupValues[1].length, m.groupValues[2])
        }
        BULLET.matchEntire(line)?.let { m -> return@mapIndexed MdLine.Bullet(i, m.groupValues[1]) }
        if (line.isBlank()) MdLine.Blank(i) else MdLine.Paragraph(i, line)
    }

/** 미리보기에서 체크박스를 누르면 원문의 해당 줄 [ ] ↔ [x] 를 뒤집는다 */
internal fun toggleCheck(md: String, lineIndex: Int): String {
    val lines = md.lines().toMutableList()
    if (lineIndex !in lines.indices) return md
    val line = lines[lineIndex]
    lines[lineIndex] = when {
        line.contains("[ ]") -> line.replaceFirst("[ ]", "[x]")
        line.contains("[x]") -> line.replaceFirst("[x]", "[ ]")
        line.contains("[X]") -> line.replaceFirst("[X]", "[ ]")
        else -> line
    }
    return lines.joinToString("\n")
}

/** "**굵게** 보통" → [("굵게", true), (" 보통", false)] */
internal fun splitBold(text: String): List<Pair<String, Boolean>> =
    text.split("**").mapIndexedNotNull { i, part ->
        if (part.isEmpty()) null else part to (i % 2 == 1)
    }

/** 할 일 진행률 (완료, 전체) — 카드에 '할 일 1/3' 처럼 보여줄 때 사용 */
internal fun todoProgress(md: String): Pair<Int, Int> {
    val checks = parseMarkdown(md).filterIsInstance<MdLine.Check>()
    return checks.count { it.checked } to checks.size
}
