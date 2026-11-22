package com.kevin233.chat.data

object Skills {

    val builtin: List<Skill> = listOf(
        Skill(
            id = "web-design",
            name = "网页设计",
            description = "从结构到响应式样式，直接给能用的页面",
            content = "你是一名前端设计师。先确认页面目标和内容，再给完整的单文件 HTML，包含响应式布局、清晰的信息层级和克制的配色。默认使用系统字体，避免不必要的依赖。代码后简短说明布局和可访问性要点。",
            builtin = true
        ),
        Skill(
            id = "code-review",
            name = "代码审查",
            description = "按严重程度列出问题，给可直接替换的代码",
            content = "你是一名严格的代码审查员。按严重程度从高到低列出问题，每条写清楚位置、风险、修复建议。能直接改的地方给出替换代码。最后列出可以延后处理的次要问题，不要为了凑数挑刺。",
            builtin = true
        ),
        Skill(
            id = "data-analysis",
            name = "数据分析",
            description = "先对齐口径，再给步骤、代码和结论",
            content = "你是一名数据分析师。开始前先确认字段含义、时间范围和统计口径。分析时先检查缺失值、异常值和样本量，再给可复现的步骤和代码。结论要区分相关和因果，不确定的地方明确说明。",
            builtin = true
        ),
        Skill(
            id = "polish",
            name = "文字润色",
            description = "保留原意，去掉啰嗦和套话",
            content = "你是一名中文编辑。保留作者的原意和语气，去掉重复、空话和不必要的形容词。先给润色后的完整版本，再用三条以内的说明指出主要改动。不要改变事实，不要添加原文没有的信息。",
            builtin = true
        ),
        Skill(
            id = "deck",
            name = "演示大纲",
            description = "按听众和时长搭出能讲的幻灯片结构",
            content = "你是一名演示教练。先确认听众、时长和想让人记住的一件事。按开场、核心论点、证据、行动建议组织大纲，每页只放一个核心结论，并给一句讲稿提示。总页数控制在时长允许的范围内。",
            builtin = true
        )
    )

    fun parse(text: String, fallbackName: String): Skill {
        val normalized = text.replace("\r\n", "\n").trim()
        if (normalized.startsWith("---")) {
            val end = normalized.indexOf("\n---", 3)
            if (end > 0) {
                val front = normalized.substring(3, end)
                val body = normalized.substring(end + 4).trim()
                val name = Regex("(?m)^name:\\s*(.+)$").find(front)?.groupValues?.get(1)
                    ?.trim()?.trim('"', '\'')?.ifBlank { null }
                val description = Regex("(?m)^description:\\s*(.+)$").find(front)?.groupValues?.get(1)
                    ?.trim()?.trim('"', '\'') ?: ""
                if (body.isNotBlank()) {
                    return Skill(
                        name = name ?: fallbackName.ifBlank { "导入的技能" },
                        description = description,
                        content = body
                    )
                }
            }
        }
        val firstLine = normalized.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return Skill(
            name = fallbackName.ifBlank { firstLine.take(40).ifBlank { "导入的技能" } },
            description = firstLine.take(70),
            content = normalized
        )
    }
}
