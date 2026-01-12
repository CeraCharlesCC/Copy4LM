package com.github.mwguerra.copyfilecontent.infrastructure.intellij

import com.github.mwguerra.copyfilecontent.CopyFileContentSettings
import com.github.mwguerra.copyfilecontent.domain.CopyOptions

/**
 * Maps IntelliJ settings state to domain CopyOptions.
 */
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
