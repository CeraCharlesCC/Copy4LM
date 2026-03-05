package io.github.ceracharlescc.copy4lm.domain.service

object DirectoryStructureTextFormatter {

    fun format(
        preText: String,
        postText: String,
        projectName: String,
        directoryStructure: String
    ): String {
        val formattedPre = PlaceholderFormatter.format(
            template = preText,
            projectName = projectName,
            directoryStructure = directoryStructure
        )

        val formattedPost = PlaceholderFormatter.format(
            template = postText,
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
