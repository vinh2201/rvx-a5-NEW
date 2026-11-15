package app.revanced.shared.util.resources

import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.util.Document
import app.revanced.shared.util.FileCopyCompat
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object ResourceUtils {
    /**
     * Copy resources from the current class loader to the resource directory.
     * @param sourceResourceDirectory The source resource directory name.
     * @param resources The resources to copy.
     */
    internal fun ResourcePatchContext.copyResources(sourceResourceDirectory: String, vararg resources: ResourceGroup) {
        val classLoader = ResourceUtils.javaClass.classLoader
        val targetResourceDirectory = this["res"]

        for (resourceGroup in resources) {
            resourceGroup.resources.forEach { resource ->
                val resourceFile = "${resourceGroup.resourceDirectoryName}/$resource"

                // Make sure the target directory exists (e.g. a freshly created "xml" or "drawable-*" directory).
                targetResourceDirectory.resolve(resourceFile).parentFile?.mkdirs()

                try {
                    Files.copy(
                        classLoader.getResourceAsStream("$sourceResourceDirectory/$resourceFile")!!,
                        targetResourceDirectory.resolve(resourceFile).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (e: NoSuchMethodError) {
                    FileCopyCompat.copy(
                        classLoader.getResourceAsStream("$sourceResourceDirectory/$resourceFile")!!,
                        targetResourceDirectory.resolve(resourceFile)
                    )
                }
            }
        }
    }

    /**
     * Resource names mapped to their corresponding resource data.
     * @param resourceDirectoryName The name of the directory of the resource.
     * @param resources A list of resource names.
     */
    internal class ResourceGroup(val resourceDirectoryName: String, vararg val resources: String)

    /**
     * Copy resources from the current class loader to the resource directory.
     * @param resourceDirectory The directory of the resource.
     * @param targetResource The target resource.
     * @param elementTag The element to copy.
     */
    internal fun ResourcePatchContext.copyXmlNode(resourceDirectory: String, targetResource: String, elementTag: String) {
        val stringsResourceInputStream = ResourceUtils.javaClass.classLoader.getResourceAsStream("$resourceDirectory/$targetResource")!!

        // Copy nodes from the resources node to the real resource node
        elementTag.copyXmlNode(
            this.document(stringsResourceInputStream),
            this.document("res/$targetResource")
        ).close()
    }

    /**
     * Copies the specified node of the source [Document] to the target [Document].
     * @param source the source [Document].
     * @param target the target [Document]-
     * @return AutoCloseable that closes the target [Document]s.
     */
    internal fun String.copyXmlNode(source: Document, target: Document): AutoCloseable {
        val hostNodes = source.getElementsByTagName(this).item(0).childNodes

        val destinationNode = target.getElementsByTagName(this).item(0)

        for (index in 0 until hostNodes.length) {
            val node = hostNodes.item(index).cloneNode(true)
            target.adoptNode(node)
            destinationNode.appendChild(node)
        }

        return AutoCloseable {
            source.close()
            target.close()
        }
    }
}
