@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.ceracharlescc.copy4lm

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.application.usecase.CopyFilesUseCase
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.CopyResult
import io.github.ceracharlescc.copy4lm.domain.vo.CopyStats
import kotlin.js.JsExport

private val DEFAULT_COPY_OPTIONS = CopyOptions()

@JsExport
external interface JsFileRef {
    val name: String
    val path: String
    val isDirectory: Boolean
}

@JsExport
external interface JsFileGateway {
    fun childrenOf(dir: JsFileRef): Array<JsFileRef>
    fun readText(file: JsFileRef, strictMemoryRead: Boolean): String
    fun isBinary(file: JsFileRef): Boolean
    fun sizeBytes(file: JsFileRef): Double
    fun relativePath(file: JsFileRef): String
}

@JsExport
external interface JsLogger {
    fun info(message: String)
    fun error(message: String, throwable: String? = null)
}

@JsExport
data class JsCopyOptions(
    val headerFormat: String = DEFAULT_COPY_OPTIONS.headerFormat,
    val footerFormat: String = DEFAULT_COPY_OPTIONS.footerFormat,
    val preText: String = DEFAULT_COPY_OPTIONS.preText,
    val postText: String = DEFAULT_COPY_OPTIONS.postText,
    val fileCountLimit: Int = DEFAULT_COPY_OPTIONS.fileCountLimit,
    val setMaxFileCount: Boolean = DEFAULT_COPY_OPTIONS.setMaxFileCount,
    val filenameFilters: Array<String> = DEFAULT_COPY_OPTIONS.filenameFilters.toTypedArray(),
    val useFilenameFilters: Boolean = DEFAULT_COPY_OPTIONS.useFilenameFilters,
    val addExtraLineBetweenFiles: Boolean = DEFAULT_COPY_OPTIONS.addExtraLineBetweenFiles,
    val strictMemoryRead: Boolean = DEFAULT_COPY_OPTIONS.strictMemoryRead,
    val maxFileSizeKB: Int = DEFAULT_COPY_OPTIONS.maxFileSizeKB,
    val projectName: String = DEFAULT_COPY_OPTIONS.projectName
)

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
    val stats: JsCopyStats,
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
        addExtraLineBetweenFiles = addExtraLineBetweenFiles,
        strictMemoryRead = strictMemoryRead,
        maxFileSizeKB = maxFileSizeKB,
        projectName = projectName
    )

private fun CopyResult.toJsCopyResult(): JsCopyResult =
    JsCopyResult(
        clipboardText = clipboardText,
        copiedFileCount = copiedFileCount,
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

private object NoopLogger : LoggerPort {
    override fun info(message: String) = Unit
    override fun error(message: String, throwable: Throwable?) = Unit
}

private class JsFileRefAdapter(private val delegate: JsFileRef) : FileRef {
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

    override fun readText(file: FileRef, strictMemoryRead: Boolean): String =
        delegate.readText(file.unwrap(), strictMemoryRead)

    override fun isBinary(file: FileRef): Boolean =
        delegate.isBinary(file.unwrap())

    override fun sizeBytes(file: FileRef): Long =
        delegate.sizeBytes(file.unwrap()).toLong()

    override fun relativePath(file: FileRef): String =
        delegate.relativePath(file.unwrap())
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
