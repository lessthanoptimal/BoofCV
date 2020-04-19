@file:Suppress("UNCHECKED_CAST")

package boofcv.kotlin

import boofcv.gui.feature.VisualizeFeatures
import boofcv.gui.feature.VisualizeShapes
import georegression.struct.curve.EllipseRotated_F64
import georegression.struct.shapes.Polygon2D_F64
import georegression.struct.shapes.Quadrilateral_F64
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage

fun BufferedImage.copy() : BufferedImage {
    val out = BufferedImage(width,height,type)
    out.createGraphics().drawImage(this,0,0,width,height,null)
    return out
}

fun BufferedImage.isInside( x:Int , y:Int ) : Boolean {
    return x >= 0 && y >= 0 && x < width && y < height
}

fun Graphics2D.draw( polygon : Polygon2D_F64, loop : Boolean, scale : Double ) {
    VisualizeShapes.drawPolygon(polygon,loop,scale,this)
}

fun Graphics2D.draw( quad : Quadrilateral_F64) {
    VisualizeShapes.draw(quad,this)
}

fun Graphics2D.draw( ellipse : EllipseRotated_F64) {
    VisualizeShapes.drawEllipse(ellipse,this)
}

fun Graphics2D.draw( ellipse : EllipseRotated_F64, scale : Double ) {
    VisualizeShapes.drawEllipse(ellipse,scale, this)
}

fun Graphics2D.drawRect( x0 : Double , y0 : Double , x1 : Double , y1 : Double ) {
    VisualizeShapes.drawRectangle(x0,y0,x1,y1, Line2D.Double(), this)
}

fun Graphics2D.drawCross( x : Double , y : Double , radius : Double ) {
    VisualizeFeatures.drawCross(this, x , y , radius)
}

fun Graphics2D.drawDot(x : Double, y : Double, radius : Double, color : Color, border : Boolean ) {
    VisualizeFeatures.drawPoint(this, x , y , radius, color, border)
}