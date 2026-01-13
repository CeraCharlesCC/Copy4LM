package io.github.ceracharlescc.copy4lm.infrastructure.intellij

import io.github.ceracharlescc.copy4lm.Copy4LMSettings
import io.github.ceracharlescc.copy4lm.domain.service.PlaceholderFormatter
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
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
            maxFileSizeKB = common.maxFileSizeKB
        )
    }

    fun formatDirectoryStructureText(
        state: Copy4LMSettings.State,
        projectName: String,
        directoryStructure: String
    ): String {
        val dirState = state.directoryStructure

        val formattedPre = PlaceholderFormatter.format(
            template = dirState.preText,
            projectName = projectName,
            directoryStructure = directoryStructure
        )

        val formattedPost = PlaceholderFormatter.format(
            template = dirState.postText,
            projectName = projectName,
            directoryStructure = directoryStructure
        )

        return buildString {
            if (formattedPre.isNotBlank()) {
                append(formattedPre)
                if (!formattedPre.endsWith("\n")) append("\n")
            }
            append(directoryStructure)
            if (formattedPost.isNotBlank()) {
                if (!directoryStructure.endsWith("\n")) append("\n")
                append(formattedPost)
            }
        }
    }
}
