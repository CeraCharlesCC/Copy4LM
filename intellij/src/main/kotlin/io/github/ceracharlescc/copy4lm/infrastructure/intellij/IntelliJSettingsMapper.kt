package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import io.github.ceracharlescc.copy4lm.Copy4LMSettings
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureOptions
import io.github.ceracharlescc.copy4lm.domain.vo.FileCollectionOptions

internal object IntelliJSettingsMapper {

    fun toCopyOptions(state: Copy4LMSettings.State, projectName: String): CopyOptions {
        val common = state.common
        val fileContent = state.fileContent
        return CopyOptions(
            headerFormat = fileContent.headerFormat,
            footerFormat = fileContent.footerFormat,
            preText = fileContent.preText,
            postText = fileContent.postText,
            fileCountLimit = common.fileCountLimit,
            setMaxFileCount = common.setMaxFileCount,
            filenameFilters = common.filenameFilters,
            useFilenameFilters = common.useFilenameFilters,
            respectGitIgnore = common.respectGitIgnore,
            addExtraLineBetweenFiles = fileContent.addExtraLineBetweenFiles,
            strictMemoryRead = common.strictMemoryRead,
            maxFileSizeKB = common.maxFileSizeKB,
            projectName = projectName
        )
    }

    fun toFileCollectionOptions(state: Copy4LMSettings.State): FileCollectionOptions {
        val common = state.common
        return FileCollectionOptions(
            fileCountLimit = common.fileCountLimit,
            setMaxFileCount = common.setMaxFileCount,
            filenameFilters = common.filenameFilters,
            useFilenameFilters = common.useFilenameFilters,
            maxFileSizeKB = common.maxFileSizeKB,
            respectGitIgnore = common.respectGitIgnore
        )
    }

    fun toDirectoryStructureOptions(state: Copy4LMSettings.State, projectName: String): DirectoryStructureOptions {
        val common = state.common
        val dirState = state.directoryStructure
        return DirectoryStructureOptions(
            preText = dirState.preText,
            postText = dirState.postText,
            fileCountLimit = common.fileCountLimit,
            setMaxFileCount = common.setMaxFileCount,
            filenameFilters = common.filenameFilters,
            useFilenameFilters = common.useFilenameFilters,
            respectGitIgnore = common.respectGitIgnore,
            maxFileSizeKB = common.maxFileSizeKB,
            projectName = projectName
        )
    }
}
