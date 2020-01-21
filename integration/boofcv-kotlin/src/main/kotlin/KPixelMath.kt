package boofcv.kotlin

import boofcv.alg.misc.PixelMath
import boofcv.struct.image.GrayF32

operator fun GrayF32.plus(other: GrayF32): GrayF32 {
    val result = createSameShape()
    PixelMath.add(this, other, result)
    return result
}

infix operator fun GrayF32.plus(value: Float): GrayF32 {
    val result = createSameShape()
    PixelMath.plus(this, value, result)
    return result
}

operator fun GrayF32.plusAssign(other: GrayF32) {
    PixelMath.add(this, other, this)
}