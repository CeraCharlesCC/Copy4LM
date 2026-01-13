package io.github.ceracharlescc.copy4lm.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DirectoryStructureBuilderTest {

    @Test
    fun `builds structure with single file`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "MyProject",
            relativePaths = listOf("src/Main.kt")
        )

        val expected = """
            Directory structure:
            └── MyProject/
                └── src/
                    └── Main.kt
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `builds structure with multiple files in same directory`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "MyProject",
            relativePaths = listOf("src/A.kt", "src/B.kt")
        )

        val expected = """
            Directory structure:
            └── MyProject/
                └── src/
                    ├── A.kt
                    └── B.kt
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `builds structure with nested directories`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "MyProject",
            relativePaths = listOf(
                "src/main/kotlin/App.kt",
                "src/test/kotlin/AppTest.kt"
            )
        )

        val expected = """
            Directory structure:
            └── MyProject/
                └── src/
                    ├── main/
                    │   └── kotlin/
                    │       └── App.kt
                    └── test/
                        └── kotlin/
                            └── AppTest.kt
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `sorts directories first then files`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "MyProject",
            relativePaths = listOf(
                "README.md",
                "src/Main.kt"
            )
        )

        val expected = """
            Directory structure:
            └── MyProject/
                ├── src/
                │   └── Main.kt
                └── README.md
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `handles empty paths list`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "MyProject",
            relativePaths = emptyList()
        )

        val expected = "Directory structure:\n└── MyProject/"

        assertEquals(expected, result)
    }

    @Test
    fun `sorts children lexicographically`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "Repo",
            relativePaths = listOf(
                "src/zebra.kt",
                "src/alpha.kt",
                "src/Beta.kt"
            )
        )

        val expected = """
            Directory structure:
            └── Repo/
                └── src/
                    ├── alpha.kt
                    ├── Beta.kt
                    └── zebra.kt
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `handles deeply nested structure`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "Project",
            relativePaths = listOf(
                "a/b/c/d/e/file.txt"
            )
        )

        val expected = """
            Directory structure:
            └── Project/
                └── a/
                    └── b/
                        └── c/
                            └── d/
                                └── e/
                                    └── file.txt
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `builds complex realistic structure`() {
        val result = DirectoryStructureBuilder.build(
            rootName = "MyApp",
            relativePaths = listOf(
                "build.gradle.kts",
                "settings.gradle.kts",
                "src/main/kotlin/App.kt",
                "src/main/kotlin/service/UserService.kt",
                "src/main/resources/config.xml",
                "src/test/kotlin/AppTest.kt"
            )
        )

        val expected = """
            Directory structure:
            └── MyApp/
                ├── src/
                │   ├── main/
                │   │   ├── kotlin/
                │   │   │   ├── service/
                │   │   │   │   └── UserService.kt
                │   │   │   └── App.kt
                │   │   └── resources/
                │   │       └── config.xml
                │   └── test/
                │       └── kotlin/
                │           └── AppTest.kt
                ├── build.gradle.kts
                └── settings.gradle.kts
        """.trimIndent()

        assertEquals(expected, result)
    }
}
