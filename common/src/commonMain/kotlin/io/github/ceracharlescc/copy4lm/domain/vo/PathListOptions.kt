package io.github.ceracharlescc.copy4lm.domain.vo

data class PathListOptions(
    val start: String = CopyDefaults.PATH_LIST_START,
    val end: String = CopyDefaults.PATH_LIST_END,
    val delimiter: String = CopyDefaults.PATH_LIST_DELIMITER
)
