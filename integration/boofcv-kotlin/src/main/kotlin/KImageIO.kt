@file:Suppress("UNCHECKED_CAST")

package boofcv.kotlin

import boofcv.alg.color.ColorRgb
import boofcv.core.image.GeneralizedImageOps
import boofcv.io.image.ConvertBufferedImage
import boofcv.io.image.UtilImageIO
import boofcv.struct.image.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException

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

fun <T : ImageGray<T>>BufferedImage.asGray( type : Class<T> , weighted : Boolean = false ): T {
    // cast to GrayU8 is a hack to get around Kotlin's strict implementation of Generics
    val dataType = ImageDataType.classToType(type)
            ?:throw RuntimeException("Invalid image type ${type.javaClass.simpleName}")
    return this.asGray<GrayU8>(dataType, weighted) as T
}

fun <T : ImageGray<T>> BufferedImage.asGray( type : ImageDataType , weighted : Boolean = false ): T {
    val gray = ImageType<T>(ImageType.Family.GRAY,type,0).createImage(this.width,this.height)
    if (weighted) {
        val color = this.asPlanar(gray.javaClass)
        ConvertBufferedImage.convertFrom(this, color, true)
        ColorRgb.rgbToGray_Weighted(color, gray)
    } else {
        ConvertBufferedImage.convertFromSingle(this,gray,gray.javaClass)
    }
    return gray
}

fun <T : ImageGray<T>>BufferedImage.asPlanar( type : Class<T>): Planar<T> {
    val dst = Planar<T>(type,this.width, this.height, 1)
    ConvertBufferedImage.convertFrom(this, dst, true)
    return dst
}

fun <T : ImageInterleaved<T>> BufferedImage.asInterleaved( type : Class<T> ): ImageInterleaved<T> {
    val dst = GeneralizedImageOps.createInterleaved(type,this.width, this.height, 1) as T
    ConvertBufferedImage.convertFromInterleaved(this, dst, true)
    return dst
}

fun BufferedImage.asGrayU8( weighted : Boolean = false ): GrayU8 {
    return this.asGray(GrayU8::class.java, weighted)
}
fun BufferedImage.asGrayF32( weighted : Boolean = false ): GrayF32 {
    return this.asGray(GrayF32::class.java, weighted)
}

fun BufferedImage.asPlanarU8(): Planar<GrayU8> = ConvertBufferedImage.convertFrom(this, true, ImageType.PL_U8)
fun BufferedImage.asPlanarF32(): Planar<GrayF32> = ConvertBufferedImage.convertFrom(this, true, ImageType.PL_F32)
fun BufferedImage.asInterleavedU8(): InterleavedU8 = ConvertBufferedImage.convertFrom(this, true, ImageType.IL_U8)
fun BufferedImage.asInterleavedF32(): InterleavedF32 = ConvertBufferedImage.convertFrom(this, true, ImageType.IL_F32)
fun <T:ImageBase<T>>BufferedImage.asImage( type : ImageType<T> , weighted : Boolean = false ): T {
    return when( type.family ) {
        // cast to GrayU8 is a hack to get around Kotlin's strict implementation of Generics
        ImageType.Family.GRAY -> this.asGray<GrayU8>(type.dataType, weighted) as T
        else -> {
            val color = type.createImage(this.width, this.height)
            ConvertBufferedImage.convertFrom(this, true, color)
            color
        }
    }
}

fun <T:ImageBase<T>>File.loadImage( type : ImageType<T>, weighted:Boolean = false ): T {
    val buffered = UtilImageIO.loadImage(this.absolutePath) ?: throw IOException("Couldn't load image")
    return buffered.asImage(type, weighted)
}
