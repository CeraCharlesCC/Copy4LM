package io.github.ceracharlescc.copy4lm.domain.vo

object CopyDefaults {
    const val HEADER_FORMAT = "```\$FILE_PATH"
    const val FOOTER_FORMAT = "```"
    const val FILE_CONTENT_PRE_TEXT = "=====\n\$PROJECT_NAME\n=====\n"
    const val EMPTY_TEXT = ""
    const val FILE_COUNT_LIMIT = 30
    const val SET_MAX_FILE_COUNT = true
    const val USE_FILENAME_FILTERS = false
    const val RESPECT_GIT_IGNORE = true
    const val ADD_EXTRA_LINE_BETWEEN_FILES = true
    const val STRICT_MEMORY_READ = true
    const val MAX_FILE_SIZE_KB = 500
    const val PROJECT_NAME = ""
}
