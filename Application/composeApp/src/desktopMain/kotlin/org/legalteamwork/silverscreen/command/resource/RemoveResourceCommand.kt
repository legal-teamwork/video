package org.legalteamwork.silverscreen.command.resource

import io.github.oshai.kotlinlogging.KotlinLogging
import org.legalteamwork.silverscreen.command.CommandUndoSupport
import org.legalteamwork.silverscreen.rm.ResourceManager
import org.legalteamwork.silverscreen.rm.resource.FolderResource
import org.legalteamwork.silverscreen.rm.resource.Resource

class RemoveResourceCommand(
    private val resourceManager: ResourceManager,
    private val resource: Resource,
    private val folder: FolderResource = resourceManager.currentFolder.value
) : CommandUndoSupport {
    override val title: String = "Remove resource"
    override val description: String =
        "Remove '${resource.title}' from ${resourceManager.getRelativePath(folder)}"
    private val logger = KotlinLogging.logger {}

    override fun execute() {
        logger.info { "Remove '${resource.title}' from ${resourceManager.getRelativePath(folder)}" }

        folder.resources.remove(resource)
    }

    override fun undo() {
        logger.info { "UNDO: Remove '${resource.title}' from ${resourceManager.getRelativePath(folder)}" }

        folder.resources.add(resource)
    }
}