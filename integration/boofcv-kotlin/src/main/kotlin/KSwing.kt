@file:Suppress("UNCHECKED_CAST")

package boofcv.kotlin

import java.awt.image.BufferedImage

fun BufferedImage.copy() : BufferedImage {
    val out = BufferedImage(width,height,type)
    out.createGraphics().drawImage(this,0,0,width,height,null)
    return out
}

fun BufferedImage.isInside( x:Int , y:Int ) : Boolean {
    return x >= 0 && y >= 0 && x < width && y < height
}