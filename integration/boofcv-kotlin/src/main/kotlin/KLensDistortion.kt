package boofcv.kotlin

import boofcv.alg.distort.brown.LensDistortionBrown
import boofcv.alg.distort.universal.LensDistortionUniversalOmni
import boofcv.struct.calib.CameraPinholeBrown
import boofcv.struct.calib.CameraUniversalOmni

fun CameraPinholeBrown.asNarrowDistortion(): LensDistortionBrown = LensDistortionBrown(this)
fun CameraUniversalOmni.asWideDistortion(): LensDistortionUniversalOmni = LensDistortionUniversalOmni(this)