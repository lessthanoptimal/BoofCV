package boofcv.kotlin

import boofcv.alg.distort.brown.LensDistortionBrown
import boofcv.alg.distort.pinhole.LensDistortionPinhole
import boofcv.alg.distort.universal.LensDistortionUniversalOmni
import boofcv.alg.geo.PerspectiveOps
import boofcv.struct.calib.CameraPinhole
import boofcv.struct.calib.CameraPinholeBrown
import boofcv.struct.calib.CameraUniversalOmni
import georegression.struct.point.Point2D_F64
import org.ejml.data.DMatrix3x3
import org.ejml.data.DMatrixRMaj
import org.ejml.data.FMatrixRMaj

fun CameraPinhole.asNarrowDistortion(): LensDistortionPinhole = LensDistortionPinhole(this)
fun CameraPinholeBrown.asNarrowDistortion(): LensDistortionBrown = LensDistortionBrown(this)
fun CameraUniversalOmni.asWideDistortion(): LensDistortionUniversalOmni = LensDistortionUniversalOmni(this)

fun CameraPinhole.toBrown(): CameraPinholeBrown { val model = CameraPinholeBrown(); model.setTo(this); return model }
fun CameraPinholeBrown.toPinhole(): CameraPinhole { return CameraPinhole(this) }

fun CameraPinhole.times(norm : Point2D_F64, out : Point2D_F64 ) { PerspectiveOps.convertNormToPixel(this,norm.x,norm.y, out)}
fun CameraPinhole.times(norm : Point2D_F64 ) : Point2D_F64 {
    val out = Point2D_F64();PerspectiveOps.convertNormToPixel(this,norm.x,norm.y, out); return out}

fun CameraPinhole.scale( scale : Double ) { PerspectiveOps.scaleIntrinsic(this, scale) }
fun CameraPinhole.asDMatrix() : DMatrixRMaj {return PerspectiveOps.pinholeToMatrix(this, DMatrixRMaj(3,3))}
fun CameraPinhole.asFMatrix() : FMatrixRMaj {return PerspectiveOps.pinholeToMatrix(this, FMatrixRMaj(3,3))}
fun CameraPinhole.asDMatrix33() : DMatrix3x3 {return PerspectiveOps.pinholeToMatrix(this, DMatrix3x3())}
fun DMatrixRMaj.asCameraPinhole( width: Int , height:Int ) : CameraPinhole {return PerspectiveOps.matrixToPinhole(this,width,height,CameraPinhole())}