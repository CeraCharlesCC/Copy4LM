@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.ceracharlescc.copy4lm

import io.github.ceracharlescc.copy4lm.application.port.FileGateway
import io.github.ceracharlescc.copy4lm.application.port.FileRef
import io.github.ceracharlescc.copy4lm.application.port.LoggerPort
import io.github.ceracharlescc.copy4lm.application.usecase.CopyDirectoryStructureUseCase
import io.github.ceracharlescc.copy4lm.application.usecase.CopyFilesUseCase
import io.github.ceracharlescc.copy4lm.application.usecase.CopyPathsUseCase
import io.github.ceracharlescc.copy4lm.domain.service.DirectoryStructureBuilder
import io.github.ceracharlescc.copy4lm.domain.vo.CopyOptions
import io.github.ceracharlescc.copy4lm.domain.vo.CopyResult
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureOptions
import io.github.ceracharlescc.copy4lm.domain.vo.DirectoryStructureResult
import io.github.ceracharlescc.copy4lm.domain.vo.PathListOptions
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
fun copyFiles(
    files: Array<JsFileRef>,
    options: JsCopyOptions,
    gateway: JsFileGateway,
    logger: JsLogger? = null
): CopyResult {
    val fileGateway = JsFileGatewayAdapter(gateway)
    val loggerPort = logger?.let { JsLoggerAdapter(it) } ?: NoopLogger
    val useCase = CopyFilesUseCase(fileGateway, loggerPort)
    return useCase.execute(files.map { JsFileRefAdapter(it) }, options.toCopyOptions())
}

@JsExport
fun copyDirectoryStructure(
    files: Array<JsFileRef>,
    options: JsDirectoryStructureOptions,
    gateway: JsFileGateway,
    logger: JsLogger? = null
): DirectoryStructureResult {
    val fileGateway = JsFileGatewayAdapter(gateway)
    val loggerPort = logger?.let { JsLoggerAdapter(it) } ?: NoopLogger
    val useCase = CopyDirectoryStructureUseCase(fileGateway, loggerPort)
    return useCase.execute(files.map { JsFileRefAdapter(it) }, options.toDirectoryStructureOptions())
}

@JsExport
fun buildDirectoryStructure(rootName: String, relativePaths: Array<String>): String =
    DirectoryStructureBuilder.build(rootName = rootName, relativePaths = relativePaths.toList())

@JsExport
external interface JsPathListOptions {
    val start: String
    val end: String
    val delimiter: String
}

@JsExport
fun copyPaths(
    files: Array<JsFileRef>,
    options: JsPathListOptions,
    relativePath: (JsFileRef) -> String,
    absolutePaths: Boolean
): String = CopyPathsUseCase { relativePath(it.unwrap()) }.execute(
    files = files.map { JsFileRefAdapter(it) },
    options = PathListOptions(options.start, options.end, options.delimiter),
    absolutePaths = absolutePaths
)

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
