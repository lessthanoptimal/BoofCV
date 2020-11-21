package boofcv.kotlin

import georegression.geometry.ConvertRotation3D_F64
import georegression.geometry.GeometryMath_F64
import georegression.struct.EulerType
import georegression.struct.affine.Affine2D_F64
import georegression.struct.homography.Homography2D_F64
import georegression.struct.point.Point2D_F32
import georegression.struct.point.Point2D_F64
import georegression.struct.point.Point3D_F32
import georegression.struct.point.Point3D_F64
import georegression.struct.se.Se2_F32
import georegression.struct.se.Se2_F64
import georegression.struct.se.Se3_F32
import georegression.struct.se.Se3_F64
import georegression.struct.so.Quaternion_F64
import georegression.struct.so.Rodrigues_F64
import georegression.struct.so.SpecialOrthogonalOps_F64
import georegression.transform.affine.AffinePointOps_F64
import georegression.transform.homography.HomographyPointOps_F64
import georegression.transform.se.SePointOps_F32
import georegression.transform.se.SePointOps_F64
import org.ejml.data.DMatrixRMaj
import kotlin.math.sqrt

fun DMatrixRMaj.times(a : Point2D_F64, out : Point2D_F64) { GeometryMath_F64.mult(this,a, out)}
fun DMatrixRMaj.times(a : Point2D_F64, out : Point3D_F64) { GeometryMath_F64.mult(this,a, out)}
operator fun DMatrixRMaj.times( p : Point2D_F64): Point2D_F64 {
    val out = Point2D_F64();GeometryMath_F64.mult(this,p, out);return out
}
operator fun DMatrixRMaj.times( p : Point3D_F64): Point3D_F64 {
    val out = Point3D_F64();GeometryMath_F64.mult(this,p, out);return out
}

operator fun Se3_F32.times( p : Point3D_F32 ): Point3D_F32 {return SePointOps_F32.transform(this,p,null)}
operator fun Se3_F64.times( p : Point3D_F64 ): Point3D_F64 {return SePointOps_F64.transform(this,p,null)}

operator fun Se3_F32.times( t : Se3_F32 ): Se3_F32 {return t.concat(this,null)}
operator fun Se3_F64.times( t : Se3_F64 ): Se3_F64 {return t.concat(this,null)}

operator fun Se2_F32.times( p : Point2D_F32 ): Point2D_F32 {return SePointOps_F32.transform(this,p,null)}
operator fun Se2_F64.times( p : Point2D_F64 ): Point2D_F64 {return SePointOps_F64.transform(this,p,null)}

operator fun Se2_F32.times( t : Se2_F32 ): Se2_F32 {return t.concat(this,null)}
operator fun Se2_F64.times( t : Se2_F64 ): Se2_F64 {return t.concat(this,null)}

fun Point2D_F64.asHomogenous(): Point3D_F64 = Point3D_F64(this.x,this.y,1.0)
fun Point2D_F64.asHomogenous(out : Point3D_F64): Point3D_F64 { out.setTo(this.x,this.y,1.0); return out}

fun Point3D_F32.transform( t : Se3_F32 ) { SePointOps_F32.transform(t,this,this)}
fun Point3D_F32.transform( t : Se3_F32 , dst : Point3D_F32) { SePointOps_F32.transform(t,this,dst)}
fun Point3D_F64.transform( t : Se3_F64 ) { SePointOps_F64.transform(t,this,this)}
fun Point3D_F64.transform( t : Se3_F64 , dst : Point3D_F64) { SePointOps_F64.transform(t,this,dst)}

fun Point2D_F64.transform( t : Se2_F64 ) { SePointOps_F64.transform(t,this,this)}
fun Point2D_F64.transform( t : Se2_F64, dst : Point2D_F64) { SePointOps_F64.transform(t,this,dst)}
fun Point2D_F64.transform( t : Homography2D_F64 ) { HomographyPointOps_F64.transform(t,this,this)}
fun Point2D_F64.transform( t : Homography2D_F64, dst : Point2D_F64) { HomographyPointOps_F64.transform(t,this,dst)}
fun Point2D_F64.transform( t : Affine2D_F64 ) { AffinePointOps_F64.transform(t,this,this)}
fun Point2D_F64.transform( t : Affine2D_F64, dst : Point2D_F64) { AffinePointOps_F64.transform(t,this,dst)}

fun DMatrixRMaj.bestFitSO3() { SpecialOrthogonalOps_F64.bestFit(this)}
fun DMatrixRMaj.asRodrigues( out : Rodrigues_F64? = null ) : Rodrigues_F64 = ConvertRotation3D_F64.matrixToRodrigues(this,out)
fun DMatrixRMaj.asQuaternion( out : Quaternion_F64? = null ) : Quaternion_F64 = ConvertRotation3D_F64.matrixToQuaternion(this,out)
fun DMatrixRMaj.asEuler( type : EulerType, out : DoubleArray? = null ) : DoubleArray = ConvertRotation3D_F64.matrixToEuler(this,type,out)

fun Rodrigues_F64.asMatrix( out : DMatrixRMaj? = null ) : DMatrixRMaj = ConvertRotation3D_F64.rodriguesToMatrix(this,out)
fun Rodrigues_F64.asQuaternion( out : Quaternion_F64? = null ) : Quaternion_F64 = ConvertRotation3D_F64.rodriguesToQuaternion(this,out)
fun Rodrigues_F64.asEuler(type : EulerType, out : DoubleArray? = null ) : DoubleArray = ConvertRotation3D_F64.rodriguesToEuler(this,type,out)

fun Quaternion_F64.asMatrix( out : DMatrixRMaj? = null ) : DMatrixRMaj = ConvertRotation3D_F64.quaternionToMatrix(this,out)
fun Quaternion_F64.asRodrigues( out : Rodrigues_F64? = null ) : Rodrigues_F64 = ConvertRotation3D_F64.quaternionToRodrigues(this,out)
fun Quaternion_F64.asEuler(type : EulerType, out : DoubleArray? = null ) : DoubleArray = ConvertRotation3D_F64.quaternionToEuler(this,type,out)

fun DoubleArray.asMatrix(type: EulerType, out : DMatrixRMaj? = null ) : DMatrixRMaj = ConvertRotation3D_F64.eulerToMatrix(type,this[0],this[1],this[2],out)
fun DoubleArray.asRodrigues(type: EulerType, out : Rodrigues_F64? = null) : Rodrigues_F64 = this.asMatrix(type).asRodrigues(out)
fun DoubleArray.asQuaternion(type: EulerType, out : Quaternion_F64? = null ) : Quaternion_F64 = ConvertRotation3D_F64.eulerToQuaternion(type,this[0],this[1],this[2],out)

fun DoubleArray.asRodrigues() : Rodrigues_F64 {
    return if( this.size == 3 ) {
        val x = this[0]
        val y = this[1]
        val z = this[2]
        val theta = sqrt(x*x + y*y + z*z)
        Rodrigues_F64(theta, x/theta, y/theta, z/theta)
    } else if( size == 4 ) {
        Rodrigues_F64(this[0], this[1], this[2], this[3])
    } else {
        throw IllegalArgumentException("Array must be of size 3 or 4")
    }
}

fun DoubleArray.asQuaternion() : Quaternion_F64 {
    if( size != 4 )
        throw IllegalArgumentException("Array must be of size 4")
    return Quaternion_F64(this[0],this[1],this[2],this[3])
}