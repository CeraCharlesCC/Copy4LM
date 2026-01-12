package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import io.github.ceracharlescc.copy4lm.CopyFileContentSettings
import io.github.ceracharlescc.copy4lm.domain.CopyOptions

object IntelliJSettingsMapper {
    fun toCopyOptions(state: CopyFileContentSettings.State): CopyOptions {
        return CopyOptions(
            headerFormat = state.headerFormat,
            footerFormat = state.footerFormat,
            preText = state.preText,
            postText = state.postText,
            fileCountLimit = state.fileCountLimit,
            setMaxFileCount = state.setMaxFileCount,
            filenameFilters = state.filenameFilters,
            useFilenameFilters = state.useFilenameFilters,
            addExtraLineBetweenFiles = state.addExtraLineBetweenFiles,
            strictMemoryRead = state.strictMemoryRead,
            maxFileSizeKB = state.maxFileSizeKB
        )
    }
}
