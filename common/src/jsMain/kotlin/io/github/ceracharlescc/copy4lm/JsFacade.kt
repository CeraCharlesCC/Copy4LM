@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.ceracharlescc.copy4lm

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.application.usecase.CopyDirectoryStructureUseCase
import io.github.ceracharlescc.copy4lm.application.usecase.CopyFilesUseCase
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.CopyResult
import io.github.ceracharlescc.copy4lm.domain.vo.CopyStats
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureOptions
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureResult
import kotlin.js.JsExport

@JsExport
external interface JsFileRef {
    val name: String
    val path: String
    val isDirectory: Boolean
}

@JsExport
external interface JsFileGateway {
    fun childrenOf(dir: JsFileRef): Array<JsFileRef>
    fun readText(file: JsFileRef, strictMemoryRead: Boolean): String?
    fun isBinary(file: JsFileRef): Boolean
    fun sizeBytes(file: JsFileRef): Double
    fun relativePath(file: JsFileRef): String
    fun isGitIgnored(file: JsFileRef): Boolean
}

@JsExport
external interface JsLogger {
    fun info(message: String)
    fun error(message: String, throwable: String? = definedExternally)
}

@JsExport
external interface JsCopyOptions {
    val headerFormat: String
    val footerFormat: String
    val preText: String
    val postText: String
    val fileCountLimit: Int
    val setMaxFileCount: Boolean
    val filenameFilters: Array<String>
    val useFilenameFilters: Boolean
    val respectGitIgnore: Boolean
    val addExtraLineBetweenFiles: Boolean
    val strictMemoryRead: Boolean
    val maxFileSizeKB: Int
    val projectName: String
}

@JsExport
data class JsCopyStats(
    val totalChars: Int,
    val totalLines: Int,
    val totalWords: Int,
    val totalTokens: Int
)

@JsExport
data class JsCopyResult(
    val clipboardText: String,
    val copiedFileCount: Int,
    val failedFileCount: Int,
    val stats: JsCopyStats,
    val fileLimitReached: Boolean
)

@JsExport
external interface JsDirectoryStructureOptions {
    val preText: String
    val postText: String
    val fileCountLimit: Int
    val setMaxFileCount: Boolean
    val filenameFilters: Array<String>
    val useFilenameFilters: Boolean
    val respectGitIgnore: Boolean
    val maxFileSizeKB: Int
    val projectName: String
}

@JsExport
data class JsDirectoryStructureResult(
    val clipboardText: String,
    val collectedFileCount: Int,
    val fileLimitReached: Boolean
)

@JsExport
fun copyFiles(
    files: Array<JsFileRef>,
    options: JsCopyOptions,
    gateway: JsFileGateway,
    logger: JsLogger? = null
): JsCopyResult {
    val fileGateway = JsFileGatewayAdapter(gateway)
    val loggerPort = logger?.let { JsLoggerAdapter(it) } ?: NoopLogger
    val useCase = CopyFilesUseCase(fileGateway, loggerPort)
    val result = useCase.execute(files.map { JsFileRefAdapter(it) }, options.toCopyOptions())
    return result.toJsCopyResult()
}

@JsExport
fun copyDirectoryStructure(
    files: Array<JsFileRef>,
    options: JsDirectoryStructureOptions,
    gateway: JsFileGateway,
    logger: JsLogger? = null
): JsDirectoryStructureResult {
    val fileGateway = JsFileGatewayAdapter(gateway)
    val loggerPort = logger?.let { JsLoggerAdapter(it) } ?: NoopLogger
    val useCase = CopyDirectoryStructureUseCase(fileGateway, loggerPort)
    return useCase.execute(files.map { JsFileRefAdapter(it) }, options.toDirectoryStructureOptions()).toJsDirectoryStructureResult()
}

@JsExport
fun buildDirectoryStructure(rootName: String, relativePaths: Array<String>): String =
    DirectoryStructureBuilder.build(rootName = rootName, relativePaths = relativePaths.toList())

private fun JsCopyOptions.toCopyOptions(): CopyOptions =
    CopyOptions(
        headerFormat = headerFormat,
        footerFormat = footerFormat,
        preText = preText,
        postText = postText,
        fileCountLimit = fileCountLimit,
        setMaxFileCount = setMaxFileCount,
        filenameFilters = filenameFilters.toList(),
        useFilenameFilters = useFilenameFilters,
        respectGitIgnore = respectGitIgnore,
        addExtraLineBetweenFiles = addExtraLineBetweenFiles,
        strictMemoryRead = strictMemoryRead,
        maxFileSizeKB = maxFileSizeKB,
        projectName = projectName
    )

private fun CopyResult.toJsCopyResult(): JsCopyResult =
    JsCopyResult(
        clipboardText = clipboardText,
        copiedFileCount = copiedFileCount,
        failedFileCount = failedFileCount,
        stats = stats.toJsCopyStats(),
        fileLimitReached = fileLimitReached
    )

private fun CopyStats.toJsCopyStats(): JsCopyStats =
    JsCopyStats(
        totalChars = totalChars,
        totalLines = totalLines,
        totalWords = totalWords,
        totalTokens = totalTokens
    )

private fun JsDirectoryStructureOptions.toDirectoryStructureOptions(): DirectoryStructureOptions =
    DirectoryStructureOptions(
        preText = preText,
        postText = postText,
        fileCountLimit = fileCountLimit,
        setMaxFileCount = setMaxFileCount,
        filenameFilters = filenameFilters.toList(),
        useFilenameFilters = useFilenameFilters,
        maxFileSizeKB = maxFileSizeKB,
        respectGitIgnore = respectGitIgnore,
        projectName = projectName
    )

private fun DirectoryStructureResult.toJsDirectoryStructureResult(): JsDirectoryStructureResult =
    JsDirectoryStructureResult(
        clipboardText = clipboardText,
        collectedFileCount = collectedFileCount,
        fileLimitReached = fileLimitReached
    )

private object NoopLogger : LoggerPort {
    override fun info(message: String) = Unit
    override fun error(message: String, throwable: Throwable?) = Unit
}

private class JsFileRefAdapter(val delegate: JsFileRef) : FileRef {
    override val name: String
        get() = delegate.name
    override val path: String
        get() = delegate.path
    override val isDirectory: Boolean
        get() = delegate.isDirectory
}

private class JsFileGatewayAdapter(private val delegate: JsFileGateway) : FileGateway {
    override fun childrenOf(dir: FileRef): List<FileRef> =
        delegate.childrenOf(dir.unwrap()).map { JsFileRefAdapter(it) }

    override fun readText(file: FileRef, strictMemoryRead: Boolean): String? =
        delegate.readText(file.unwrap(), strictMemoryRead)

    override fun isBinary(file: FileRef): Boolean =
        delegate.isBinary(file.unwrap())

    override fun sizeBytes(file: FileRef): Long =
        delegate.sizeBytes(file.unwrap()).toLong()

    override fun relativePath(file: FileRef): String =
        delegate.relativePath(file.unwrap())

    override fun isGitIgnored(file: FileRef): Boolean =
        delegate.isGitIgnored(file.unwrap())
}

private class JsLoggerAdapter(private val delegate: JsLogger) : LoggerPort {
    override fun info(message: String) {
        delegate.info(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        delegate.error(message, throwable?.message)
    }
}

private fun FileRef.unwrap(): JsFileRef = (this as JsFileRefAdapter).delegate
