package org.legalteamwork.silverscreen.rm.resource

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.pathString
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class VideoResource(
    private val resourcePath: String,
    override var parent: FolderResource?,
    override val title: MutableState<String> = mutableStateOf(File(resourcePath).name),
    /**
     * Pre-calculated number of frames in the provided video resource
     */
    private val framesCount: Int? = null
) : Resource {
    override val previewPath: String by lazy { buildPreviewFile() }
    val numberOfFrames: Int by lazy { framesCount ?: grabLengthInFrames(File(resourcePath)) }
    override val properties: ResourceProperties
        get() = ResourceProperties(
            listOf(
                ResourceProperty("Description", "Resource type", "Video"),
                ResourceProperty("Description", "Title", title.value),
                ResourceProperty("Description", "Preview path", previewPath),
                ResourceProperty("Video", "Resource path", resourcePath),
                ResourceProperty("Video", "Number of frames", numberOfFrames.toString()),
            )
        )

    constructor(resourceFile: File, parent: FolderResource?, framesCount: Int? = null) : this(
        resourceFile.absolutePath,
        resourceFile.name,
        parent,
        framesCount
    )

    constructor(resourcePath: String, stringTitle: String, parent: FolderResource?, framesCount: Int? = null) : this(
        resourcePath,
        parent,
        mutableStateOf(stringTitle),
        framesCount
    )

    /**
     * Gets a frame from the current video resource with the provided index
     */
    fun getFrame(index: Int = 5): ResourceFrame {
        try {
            val frameGrabber = FFmpegFrameGrabber(resourcePath)
            frameGrabber.format = "mp4"
            frameGrabber.frameNumber = index

            frameGrabber.start()

            val frame = frameGrabber.grabImage() ?: throw Exception("Frame is NULL!")
            if (frame.image == null) throw Exception("Frame Image is NULL!")

            val converter = Java2DFrameConverter()
            val bufferedImage = converter.convert(frame)

            frameGrabber.stop()
            frameGrabber.close()

            return ResourceFrame(bufferedImage)
        } catch (e: FFmpegFrameGrabber.Exception) {
            throw BuildException()
        }
    }

    /**
     * Returns amount of frames in the provided resource file
     *
     * @see FFmpegFrameGrabber.getLengthInVideoFrames
     */
    fun grabLengthInFrames(resourceFile: File): Int {
        val frameGrabber = FFmpegFrameGrabber(resourceFile)

        try {
            frameGrabber.start()
            val lengthInFrames = frameGrabber.lengthInFrames
            frameGrabber.stop()
            frameGrabber.close()

            return lengthInFrames
        } catch (e: FFmpegFrameGrabber.Exception) {
            throw BuildException()
        }
    }

    /**
     * Creates preview for the current video resource
     *
     * @return preview file path
     */
    fun buildPreviewFile(): String {
        val result = kotlin.io.path.createTempFile().pathString
        val bufferedImage = getFrame().bufferedImage
        val previewFile = File(result)

        // Scale to size with width = 256
        val scaledInstance = bufferedImage.getScaledInstance(256, -1, java.awt.Image.SCALE_FAST)
        val scaledBufferedImage = BufferedImage(
            scaledInstance.getWidth(null),
            scaledInstance.getHeight(null),
            BufferedImage.TYPE_INT_ARGB
        )
        val graphics = scaledBufferedImage.createGraphics()
        graphics.drawImage(scaledInstance, 0, 0, null)
        graphics.dispose()
        ImageIO.write(scaledBufferedImage, "png", previewFile)

        return result
    }

    override fun clone() = VideoResource(resourcePath, "$title (clone)", parent)
    override fun action() {}

    class BuildException : Exception()
}
