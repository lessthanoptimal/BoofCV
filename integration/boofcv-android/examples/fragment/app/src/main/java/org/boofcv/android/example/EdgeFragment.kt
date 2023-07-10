package org.boofcv.android.example

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import boofcv.android.BoofAndroidUtils
import boofcv.android.ConvertBitmap
import boofcv.android.VisualizeImageData
import boofcv.android.camera2.CameraID
import boofcv.factory.filter.derivative.FactoryDerivative
import boofcv.struct.image.GrayS16
import boofcv.struct.image.GrayU8
import boofcv.struct.image.ImageDimension
import org.boofcv.android.example.databinding.EdgeLayoutBinding
import org.boofcv.android.fragments.AndroidImageProcessing
import org.boofcv.android.fragments.ImageProcessingFragment
import org.boofcv.android.fragments.LockImages
import org.ddogleg.struct.DogArray_I8


/**
 * Process a video feed, computes edges, displays a visualization of the edges.
 */
class EdgeFragment : ImageProcessingFragment() {
    // Bindings for user interface
    private var _uiBinding: EdgeLayoutBinding? = null
    private val uiBinding get() = _uiBinding!!

    init {
        val derivX = GrayS16(0, 0)
        val derivY = GrayS16(0, 0)
        val gradientOp = FactoryDerivative.three(GrayU8::class.java, GrayS16::class.java)

        imageProcessing = object : AndroidImageProcessing {
            // These are images that Android understands and used to visualization
            // A double buffer strategy is used to allow the processing and UI threads
            // to run with minimal coupling
            var bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            var bitmapWork = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val bitmapTmp = DogArray_I8()

            override fun process(frame: LockImages) {
                // Compute the gradient
                gradientOp.process(frame.gray, derivX, derivY)

                // Make sure the bitmap is large enough
                bitmapWork = ConvertBitmap.checkDeclare(frame.gray, bitmapWork)

                // Visualize the gradient
                VisualizeImageData.colorizeGradient(derivX, derivY, -1, bitmapWork, bitmapTmp);

                // Visualization and image processing are two different threads.
                // To avoid reading / writing the same data structure at the same time we will
                // synchronize
                synchronized(this) {
                    // Perform a fast swap
                    val tmp = bitmapWork
                    bitmapWork = bitmap
                    bitmap = tmp
                }
            }

            override fun visualize(canvas: Canvas, imageToView: Matrix) {
                // Draw the visualized gradient to the canvas
                synchronized(this) {
                    canvas.drawBitmap(this.bitmap, imageToView, null)
                }
            }
        }
    }

    override fun onDestroyView() {
        _uiBinding = null
        super.onDestroyView()
    }

    /**
     * Need to tell it where it will render the results
     */
    override fun getCameraFrame(): FrameLayout {
        return uiBinding.cameraFrame
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _uiBinding = EdgeLayoutBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun lookupCameraConfig(selectedCam: CameraID, resolution: ImageDimension) {
        // Tell it to use the first camera that it finds that's back facing. If none are found
        // then it will use a front facing camera
        val cameras = BoofAndroidUtils.getAllCameras(cameraManager)
        for (camera in cameras) {
            selectedCam.setTo(camera)

            val characteristics = cameraManager.getCameraCharacteristics(camera.id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (BoofAndroidUtils.facingToString(facing) == "back") {
                break
            }
        }

        // Hard code 640x480 If there isn't an exact match it will select something close
        resolution.setTo(640, 480)

        Log.i(TAG, "Selected camera $selectedCam")
    }

    override fun customConfigure(cameraID: CameraID, builder: CaptureRequest.Builder) {
        // nothing special needs to be done so this function is empty
    }

}