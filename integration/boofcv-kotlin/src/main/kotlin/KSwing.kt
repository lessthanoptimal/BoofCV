@file:Suppress("UNCHECKED_CAST")

package boofcv.kotlin

import boofcv.alg.color.ColorRgb
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.*
import java.awt.image.BufferedImage

/**
 * Convenience extension to convert from [GrayF32] to [BufferedImage]
 *
 * @return New [BufferedImage] with equivalent image representation
 */
fun GrayF32.asBufferedImage(): BufferedImage = ConvertBufferedImage.convertTo(this, null, true)
fun GrayU8.asBufferedImage(): BufferedImage = ConvertBufferedImage.convertTo(this, null, true)
fun Planar<*>.asBufferedImage(): BufferedImage {
    return when {
        this.getBandType() == GrayU8::class.java ->
            ConvertBufferedImage.convertTo_U8(this as Planar<GrayU8>, null, true)
        this.getBandType() == GrayF32::class.java ->
            ConvertBufferedImage.convertTo_F32(this as Planar<GrayF32>, null, true)
        else -> throw IllegalArgumentException("Unknown plane type")
    }
}

fun BufferedImage.asGrayU8(): GrayU8 = ConvertBufferedImage.convertFrom(this, GrayU8(1,1))
fun BufferedImage.asGrayU8( weighted : Boolean ): GrayU8 {
    if (weighted) {
        val color = this.asPlanarU8()
        val gray = GrayU8(color.width, color.height)
        ColorRgb.rgbToGray_Weighted(color, gray)
        return gray
    } else {
        return this.asGrayU8()
    }
}
fun BufferedImage.asGrayF32(): GrayF32 = ConvertBufferedImage.convertFrom(this, GrayF32(1,1))
fun BufferedImage.asGrayF32( weighted : Boolean ): GrayF32 {
    if (weighted) {
        val color = this.asPlanarF32()
        val gray = GrayF32(color.width, color.height)
        ColorRgb.rgbToGray_Weighted(color, gray)
        return gray
    } else {
        return this.asGrayF32()
    }
}
fun BufferedImage.asPlanarU8(): Planar<GrayU8> = ConvertBufferedImage.convertFrom(this, true, ImageType.PL_U8)
fun BufferedImage.asPlanarF32(): Planar<GrayF32> = ConvertBufferedImage.convertFrom(this, true, ImageType.PL_F32)
fun BufferedImage.asInterleavedU8(): InterleavedU8 = ConvertBufferedImage.convertFrom(this, true, ImageType.IL_U8)
fun BufferedImage.asInterleavedF32(): InterleavedF32 = ConvertBufferedImage.convertFrom(this, true, ImageType.IL_F32)

