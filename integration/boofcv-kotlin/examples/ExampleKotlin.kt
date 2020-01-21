import boofcv.gui.image.ShowImages
import boofcv.io.UtilIO
import boofcv.io.image.UtilImageIO
import boofcv.kotlin.asBufferedImage
import boofcv.kotlin.plus
import boofcv.struct.image.GrayF32

fun main() {
    val input = UtilImageIO.loadImage(UtilIO.pathExample("standard/kodim17.jpg"), GrayF32::class.java)

    val output = input plus 1.0f

    ShowImages.showWindow(output.asBufferedImage(),"Image", true)
}