package org.legalteamwork.silverscreen.re

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.legalteamwork.silverscreen.AppScope
import org.legalteamwork.silverscreen.command.edit.AddFilterToResource
import org.legalteamwork.silverscreen.command.edit.AddResourceToTrackFabric
import org.legalteamwork.silverscreen.command.edit.MoveResourceOnTrackCommand
import org.legalteamwork.silverscreen.re.VideoTrack.resourcesOnTrack
import org.legalteamwork.silverscreen.re.VideoTrack.videoResources
import org.legalteamwork.silverscreen.resources.Dimens
import org.legalteamwork.silverscreen.resources.EditingPanelTheme
import org.legalteamwork.silverscreen.rm.resource.Resource
import org.legalteamwork.silverscreen.rm.window.effects.VideoEffect
import org.legalteamwork.silverscreen.rm.window.effects.VideoFilter
import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AppScope.VideoTrackCompose(timelineLength: Dp) {
    //val resources = remember { resourcesOnTrack }
    logger.info { "Composing video resource" }

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dataFlavor = DataFlavor(
                    DataFlavor.javaSerializedObjectMimeType + ";class=org.legalteamwork.silverscreen.rm.resource.Resource",
                    "Resource"
                )
                val transferData = event.awtTransferable.getTransferData(dataFlavor)

                if (transferData is Resource) {
                    val position = VideoTrack.getFreePosition()
                    val addResourceCommand = AddResourceToTrackFabric
                        .makeCommand(transferData, VideoTrack, position)

                    if (addResourceCommand != null) {
                        commandManager.execute(addResourceCommand)
                    }

                    return true
                } else {
                    return false
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .width(timelineLength)
                .background(color = EditingPanelTheme.VIDEO_TRACK_BACKGROUND_COLOR)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dragAndDropTarget
                ),
    ) {
        for (resource in resourcesOnTrack) {
            val resourceOnTrackScope = ResourceOnTrackScope(commandManager, resourceManager, resource)
            resourceOnTrackScope.ResourceOnTrackCompose()
        }
    }
}


@Composable
private fun <T> ResourceOnTrackScope.DragTarget(
    modifier: Modifier,
    dataToDrop: T,
    content: @Composable (() -> Unit)
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    val currentState = resourceOnTrack.localDragTargetInfo.component1()

    Box(
        modifier =
            modifier
                .offset { IntOffset(currentState.dragOffset.x.roundToInt(), 0) }
                .onGloballyPositioned {
                    currentPosition = it.localToWindow(Offset.Zero)
                }
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = {
                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + it
                        currentState.draggableComposable = content
                    }, onDrag = { change, dragAmount ->
                        change.consume()
                        logger.debug { "Dragging video resource..." }
                        currentState.dragOffset = Offset(max(0f, currentState.dragOffset.x + dragAmount.x), 0f)
                    }, onDragEnd = {
                        logger.debug { "Dragged video resource successfully" }
                        currentState.isDragging = false

                        val newPosition = (currentState.dragOffset.x / DpInFrame).roundToInt()
                        val moveResourceOnTrackCommand =
                            MoveResourceOnTrackCommand(resourceOnTrack, VideoTrack, newPosition)
                        commandManager.execute(moveResourceOnTrackCommand) // FIXME use application context
                    }, onDragCancel = {
                        logger.warn { "Canceled dragging video resource" }
                        currentState.dragOffset = Offset.Zero
                        currentState.isDragging = false
                    })
                },
    ) {
        content()
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ResourceOnTrackScope.ResourceOnTrackCompose() {
    val size by mutableStateOf(resourceOnTrack.framesCount * DpInFrame * 1.dp)
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dataFlavor = DataFlavor(
                    DataFlavor.javaSerializedObjectMimeType + ";class=org.legalteamwork.silverscreen.rm.window.effects.VideoEffect",
                    "VideoEffect"
                )
                val transferData = event.awtTransferable.getTransferData(dataFlavor)

                if (transferData is VideoEffect) {
                    val videoFilter = transferData.createFilter(resourceOnTrack)
                    val command = AddFilterToResource(videoFilter, resourceOnTrack)
                    commandManager.execute(command)

                    return true
                } else {
                    return false
                }
            }
        }
    }

    DragTarget(
        modifier = Modifier
            .fillMaxHeight()
            .width(size)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            ),
        dataToDrop = "",
    ) {
        Column {
            ResourceOnTrackMainLine(resourceOnTrack)

            for (videoFilter in resourceOnTrack.filters) {
                ResourceOnTrackFilterLine(videoFilter)
            }
        }
    }
}

@Composable
fun ResourceOnTrackFilterLine(videoFilter: VideoFilter) {
    val videoEffect = videoFilter.videoEffect
    val offset = videoFilter.firstFrame * DpInFrame * 1.dp
    val size = videoFilter.framesLength * DpInFrame * 1.dp

    Box(Modifier.fillMaxWidth().height(Dimens.RESOURCE_ON_TRACK_EFFECT_PART_HEIGHT)) {
        Row {
            Box(Modifier.width(offset))
            Box(
                Modifier
                    .width(size)
                    .fillMaxHeight()
                    .background(EditingPanelTheme.EFFECT_BACKGROUND_COLOR, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(videoEffect.title)
            }
        }
    }
}

/*
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ResourceOnTrackScope.ResourceOnTrackMainLine(resourceOnTrack: ResourceOnTrack) {
    var droppableFileBackgroundColor by mutableStateOf(EditingPanelTheme.DROPPABLE_FILE_BACKGROUND_COLOR_1)
    val size by mutableStateOf(resourceOnTrack.framesCount * DpInFrame * 1.dp)
    val imageBitmap = remember {
        File(videoResources[resourceOnTrack.id].previewPath).inputStream().readAllBytes()
            .decodeToImageBitmap()
    }

    val imageWidthDp = imageBitmap.width.dp
    //val imageHeightDp = imageBitmap.height.dp
    val totalWidth = size.value
    val numberOfFullImages = (totalWidth / imageWidthDp.value).toInt()
    val remainingWidth = totalWidth % imageWidthDp.value

    DragTarget(
        modifier = Modifier.fillMaxHeight().width(size),
        dataToDrop = "",
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .width(size)
                .background(
                    color = EditingPanelTheme.DROPPABLE_FILE_BACKGROUND_COLOR_1,
                    shape = RoundedCornerShape(5.dp)
                ),
        ) {
            Row(modifier = Modifier.fillMaxHeight()) {
                for (i in 0 until numberOfFullImages) {
                    Image(
                        painter = BitmapPainter(imageBitmap),
                        contentDescription = videoResources[resourceOnTrack.id].title.value,
                        modifier = Modifier
                            .width(imageWidthDp)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }

                if (remainingWidth > 0) {
                    Image(
                        painter = BitmapPainter(imageBitmap),
                        contentDescription = videoResources[resourceOnTrack.id].title.value,
                        modifier = Modifier
                            .width(remainingWidth.dp)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopStart
                    )
                }
            }
        }
    }
}


 */


/*
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ResourceOnTrackMainLine(resourceOnTrack: ResourceOnTrack) {
    var droppableFileBackgroundColor by mutableStateOf(EditingPanelTheme.DROPPABLE_FILE_BACKGROUND_COLOR_1)

    BoxWithConstraints(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(Dimens.RESOURCE_ON_TRACK_MAIN_PART_HEIGHT)
            .background(
                color = Color.Blue,
                RoundedCornerShape(5.dp)
            )
            .clickable(
                onClick = {
                    if (VideoEditor.highlightResource(resourceOnTrack.id))
                        droppableFileBackgroundColor =
                            EditingPanelTheme.HIGHLIGHTED_DROPPABLE_FILE_BACKGROUND_COLOR
                    else
                        droppableFileBackgroundColor = EditingPanelTheme.DROPPABLE_FILE_BACKGROUND_COLOR_1
                }
            )
    ){
                val textHeight = 20.dp
                val previewHeight = 75.dp
                val previewWidth = 150.dp

                Column(
                    modifier =
                    Modifier
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        text = videoResources[resourceOnTrack.id].title.value,
                        modifier =
                        Modifier
                            .offset(x = 10.dp)
                            .height(textHeight),
                        color = EditingPanelTheme.DROPPABLE_FILE_TEXT_COLOR,
                    )
                    Image(
                        painter =
                        BitmapPainter(
                            File(videoResources[resourceOnTrack.id].previewPath).inputStream().readAllBytes()
                                .decodeToImageBitmap()
                        ),
                        contentDescription = videoResources[resourceOnTrack.id].title.value,
                        modifier =
                        Modifier
                            .width(previewWidth)
                            .height(previewHeight),
                    )

                }
            }
}




 */



@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ResourceOnTrackMainLine(resourceOnTrack: ResourceOnTrack) {
    val previewHeight = 75.dp
    val imageBitmap = remember {
        File(videoResources[resourceOnTrack.id].previewPath).inputStream().readAllBytes()
            .decodeToImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(previewHeight)
            .border(1.dp, Color.White, RoundedCornerShape(5.dp)) // Белая рамка
            .clip(RoundedCornerShape(5.dp)) // Скруглённые углы
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            val imageWidth = 150.dp // Ширина одной картинки
            val count = (LocalDensity.current.run { (1000.dp) } / imageWidth).toInt() + 1

            // Повторяем изображение для заполнения ширины
            for (i in 0 until count) {
                Image(
                    painter = BitmapPainter(imageBitmap),
                    contentDescription = null,
                    modifier = Modifier
                        .width(imageWidth)
                        .height(previewHeight),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}











