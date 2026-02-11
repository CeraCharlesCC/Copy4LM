package io.github.ceracharlescc.copy4lm

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

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
        var fileCountLimit: Int = 30,
        var setMaxFileCount: Boolean = true,
        var maxFileSizeKB: Int = 500,
        var useFilenameFilters: Boolean = false,
        var filenameFilters: List<String> = listOf(),
        var respectGitIgnore: Boolean = true,
        var strictMemoryRead: Boolean = true,
        var showCopyNotification: Boolean = true
    )

    data class FileContentState(
        var headerFormat: String = $$"```$FILE_PATH",
        var footerFormat: String = "```",
        var preText: String = "",
        var postText: String = "",
        var addExtraLineBetweenFiles: Boolean = true
    )

    data class DirectoryStructureState(
        var preText: String = "",
        var postText: String = ""
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
