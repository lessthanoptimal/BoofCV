package boofcv.kotlin

import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayF32
import java.awt.image.BufferedImage

/**
 * Convenience extension to convert from [GrayF32] to [BufferedImage]
 *
 * @return New [BufferedImage] with equivalent image representation
 */
fun GrayF32.asBufferedImage(): BufferedImage = ConvertBufferedImage.convertTo(this, null, true)