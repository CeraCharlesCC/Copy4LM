package io.github.ceracharlescc.copy4lm

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import io.github.ceracharlescc.copy4lm.domain.vo.CopyDefaults

@Service(Service.Level.PROJECT)
@State(
    name = "Copy4LMSettings",
    storages = [Storage("Copy4LMSettings.xml")]
)
internal class Copy4LMSettings : PersistentStateComponent<Copy4LMSettings.State> {

    data class State(
        var common: CommonState = CommonState(),
        var fileContent: FileContentState = FileContentState(),
        var directoryStructure: DirectoryStructureState = DirectoryStructureState()
    )

    data class CommonState(
        var fileCountLimit: Int = CopyDefaults.FILE_COUNT_LIMIT,
        var setMaxFileCount: Boolean = CopyDefaults.SET_MAX_FILE_COUNT,
        var maxFileSizeKB: Int = CopyDefaults.MAX_FILE_SIZE_KB,
        var useFilenameFilters: Boolean = CopyDefaults.USE_FILENAME_FILTERS,
        var filenameFilters: List<String> = listOf(),
        var respectGitIgnore: Boolean = CopyDefaults.RESPECT_GIT_IGNORE,
        var strictMemoryRead: Boolean = CopyDefaults.STRICT_MEMORY_READ,
        var showCopyNotification: Boolean = true
    )

    data class FileContentState(
        var headerFormat: String = CopyDefaults.HEADER_FORMAT,
        var footerFormat: String = CopyDefaults.FOOTER_FORMAT,
        var preText: String = CopyDefaults.FILE_CONTENT_PRE_TEXT,
        var postText: String = CopyDefaults.EMPTY_TEXT,
        var addExtraLineBetweenFiles: Boolean = CopyDefaults.ADD_EXTRA_LINE_BETWEEN_FILES
    )

    data class DirectoryStructureState(
        var preText: String = CopyDefaults.EMPTY_TEXT,
        var postText: String = CopyDefaults.EMPTY_TEXT
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): Copy4LMSettings =
            project.getService(Copy4LMSettings::class.java)
    }
}
