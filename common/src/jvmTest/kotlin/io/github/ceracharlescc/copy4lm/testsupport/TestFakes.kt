package io.github.ceracharlescc.copy4lm.testsupport

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort

internal data class FakeFileRef(
    override val name: String,
    override val path: String,
    override val isDirectory: Boolean = false
) : FileRef

internal class FakeFileGateway(
    private val children: Map<String, List<FakeFileRef>> = emptyMap(),
    private val contents: Map<String, String> = emptyMap(),
    private val binaryPaths: Set<String> = emptySet(),
    private val sizesBytes: Map<String, Long> = emptyMap(),
    private val repoRoot: String = "/repo"
) : FileGateway {

    /** Records (path, strictMemoryRead) to assert strictMemoryRead is passed correctly. */
    val readCalls: MutableList<Pair<String, Boolean>> = mutableListOf()

    override fun childrenOf(dir: FileRef): List<FileRef> =
        children[dir.path].orEmpty()

    override fun readText(file: FileRef, strictMemoryRead: Boolean): String {
        readCalls += (file.path to strictMemoryRead)
        return contents[file.path].orEmpty()
    }

    override fun isBinary(file: FileRef): Boolean =
        file.path in binaryPaths

    override fun sizeBytes(file: FileRef): Long =
        sizesBytes[file.path]
            ?: (contents[file.path]?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L)

    override fun relativePath(file: FileRef): String {
        val prefix = repoRoot.trimEnd('/') + "/"
        return if (file.path.startsWith(prefix)) file.path.removePrefix(prefix) else file.path
    }
}

class CapturingLogger : LoggerPort {
    val infos = mutableListOf<String>()
    val errors = mutableListOf<Pair<String, Throwable?>>()

    override fun info(message: String) {
        infos += message
    }

    override fun error(message: String, throwable: Throwable?) {
        errors += (message to throwable)
    }
}
