package io.github.ceracharlescc.copy4lm.application.port

/**
 * Abstract representation of a file in the file system.
 */
interface FileRef {
    val name: String
    val path: String
    val isDirectory: Boolean
}

/**
 * Port for file system operations.
 */
interface FileGateway {
    /**
     * Returns the children of a directory.
     */
    fun childrenOf(dir: FileRef): List<FileRef>

    /**
     * Reads the text content of a file.
     * @param file The file to read
     * @param strictMemoryRead If true, only reads from memory if file is open in editor
     */
    fun readText(file: FileRef, strictMemoryRead: Boolean): String

    /**
     * Checks if the file is a binary file.
     */
    fun isBinary(file: FileRef): Boolean

    /**
     * Returns the size of the file in bytes.
     */
    fun sizeBytes(file: FileRef): Long

    /**
     * Returns the relative path of the file from the repository root.
     */
    fun relativePath(file: FileRef): String
}
