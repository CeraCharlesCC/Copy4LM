package io.github.ceracharlescc.copy4lm.domain.service

internal object DirectoryStructureBuilder {

    fun build(rootName: String, relativePaths: List<String>): String {
        if (relativePaths.isEmpty()) {
            return "Directory structure:\n└── $rootName/"
        }

        val root = Node(rootName, isDir = true)

        for (path in relativePaths) {
            insertPath(root, path)
        }

        return buildString {
            appendLine("Directory structure:")
            renderNode(this, root, prefix = "", isLast = true, isRoot = true)
        }.trimEnd()
    }

    private fun insertPath(root: Node, path: String) {
        val segments = path.split("/").filter { it.isNotEmpty() }
        var current = root

        for ((index, segment) in segments.withIndex()) {
            val isLastSegment = index == segments.lastIndex
            val isDir = !isLastSegment

            current = current.children.getOrPut(segment) {
                Node(segment, isDir = isDir)
            }

            if (!isLastSegment && !current.isDir) {
                current.isDir = true
            }
        }
    }

    private fun renderNode(
        sb: StringBuilder,
        node: Node,
        prefix: String,
        isLast: Boolean,
        isRoot: Boolean
    ) {
        val connector = if (isLast) "└── " else "├── "
        val suffix = if (node.isDir) "/" else ""

        if (isRoot) {
            sb.appendLine("$connector${node.name}$suffix")
        } else {
            sb.appendLine("$prefix$connector${node.name}$suffix")
        }

        val sortedChildren = node.children.values.sortedWith(
            compareByDescending<Node> { it.isDir }.thenBy { it.name.lowercase() }
        )

        val childPrefix = if (isRoot) {
            "    "
        } else {
            prefix + if (isLast) "    " else "│   "
        }

        for ((index, child) in sortedChildren.withIndex()) {
            val childIsLast = index == sortedChildren.lastIndex
            renderNode(sb, child, childPrefix, childIsLast, isRoot = false)
        }
    }

    private class Node(
        val name: String,
        var isDir: Boolean,
        val children: MutableMap<String, Node> = mutableMapOf()
    )
}
