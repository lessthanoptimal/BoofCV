@file:Suppress("UNCHECKED_CAST")

package boofcv.kotlin

import boofcv.alg.color.ColorRgb
import boofcv.core.image.GConvertImage
import boofcv.struct.image.*

fun <TI : ImageInterleaved<TI>, TG : ImageGray<TG>> TI.asGray(weighted : Boolean = false ) : TG {
    val gray = ImageType<GrayU8>(ImageType.Family.GRAY,this.imageType.dataType,0).createImage(this.width,this.height) as TG
    if (weighted) {
        ColorRgb.rgbToGray_Weighted(this, gray)
    } else {
        GConvertImage.average(this,gray)
    }
    return gray
}

fun <T : ImageGray<T>> Planar<T>.asGray(weighted: Boolean = false ) : T {
    val gray = ImageType<GrayU8>(ImageType.Family.GRAY,this.imageType.dataType,0).createImage(this.width,this.height) as T
    if (weighted) {
        ColorRgb.rgbToGray_Weighted(this, gray)
    } else {
        GConvertImage.average(this,gray)
    }
    return gray
}

fun <TI : ImageInterleaved<TI>, TG : ImageGray<TG>> TI.asPlanar() : Planar<TG> {
    val planar = ImageType<Planar<GrayU8>>(ImageType.Family.PLANAR,this.imageType.dataType,this.numBands).
            createImage(this.width,this.height) as Planar<TG>
    GConvertImage.convert(this,planar)
    return planar
}

fun <TI : ImageInterleaved<TI>, TG : ImageGray<TG>> Planar<TG>.asInterleaved() : TI {
    val interleaved = ImageType<InterleavedU8>(ImageType.Family.INTERLEAVED,this.imageType.dataType,this.numBands).
            createImage(this.width,this.height) as TI
    GConvertImage.convert(this,interleaved)
    return interleaved
}

fun <T : ImageGray<T>,Y : ImageGray<Y>> Y.asType( type : Class<T>) : T {
    val out = ImageType.single<T>(type).createImage(width, height)
    GConvertImage.convert(this,out)
    return out
}

fun <T : ImageGray<T>,Y : ImageGray<Y>> Planar<Y>.asType( type : Class<T>) : Planar<T> {
    val out = ImageType.pl(numBands,type).createImage(width, height)
    GConvertImage.convert(this,out)
    return out
}

fun <T : ImageInterleaved<T>,Y : ImageInterleaved<Y>> Y.asType( type : Class<T>) : T {
    val out = ImageType.il<T>(numBands,type).createImage(width, height)
    GConvertImage.convert(this,out)
    return out
}