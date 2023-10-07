/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.android.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import boofcv.android.camera2.CameraID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * Camera fragment that creates ImageReader for processing camera images but doesn't do any
 * processing or thread management. Provides most of the boilerplate for constructing an image
 */
abstract class CameraProcessFragment : Fragment() {
    /** Used to adjust how it captures images. Affects quality and speed */
    private var captureRequestTemplateType = CameraDevice.TEMPLATE_RECORD

    /** Data structures for each camera and capture surface */
    protected val cameraDevices = HashMap<CameraID, DeviceSurfaces>()

    /** This is called after the camera has initialized */
    protected var invokeAfterCameraInitialize = {}

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    protected val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [HandlerThread] where all camera operations run */
    protected val cameraThread = HandlerThread("CameraHandler").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    protected val cameraHandler = Handler(cameraThread.looper)

    override fun onDestroyView() {
        closeAllCameras()
        super.onDestroyView()
    }

    /**
     * Stops all camera capture sessions and cleans up
     */
    protected fun closeAllCameras() = lifecycleScope.launch(Dispatchers.Main) {
        closeAllCamerasNow()
    }

    /**
     * Immediately invokes the close all functions. Does not wait for the main dispatcher
     */
    protected open fun closeAllCamerasNow() {
        for (cam in cameraDevices.values) {
            cam.session?.stopRepeating()
            try {
                cam.device.close()
                for (reader in cam.readers) {
                    reader.close()
                }
            } catch( IllegalStateException: e) {
                Log.e(TAG, "Failed to close a camera", e)
            }
        }
        cameraDevices.clear()
    }

    /**
     * Adds a surface for the camera device which will invoke the following operator every
     * time a new image is captured. Blocks until finished.
     */
    protected fun addCameraProcessor(
        cameraID: CameraID,
        width: Int,
        height: Int,
        op: (image: Image) -> Unit
    ): ImageReader {
        val resolution = selectResolution(cameraID.openID, width, height)

        val imageReader = ImageReader.newInstance(
            resolution.width, resolution.height, imageFormat, IMAGE_BUFFER_SIZE
        )
        imageReader.setOnImageAvailableListener(
            createOnAvailableListener(op),
            cameraHandler
        )

        val camera = lookupDeviceSurfaces(cameraID)
        camera.readers.add(imageReader)
        camera.surfaces.add(imageReader.surface)
        return imageReader
    }

    /**
     * Adds a new surface for the device. Blocks until finished.
     */
    protected fun addCameraSurface(cameraID: CameraID, surface: Surface) {
        Log.i(TAG, "camera: add surface: id=$cameraID")
        val camera: DeviceSurfaces = lookupDeviceSurfaces(cameraID)
        camera.surfaces.add(surface)
    }

    /**
     * Looks up the device and opens the device if it has not already been opened while
     * creating a new [DeviceSurfaces]
     */
    private fun lookupDeviceSurfaces(cameraID: CameraID): DeviceSurfaces {
        val camera: DeviceSurfaces
        if (cameraID in cameraDevices) {
            camera = cameraDevices[cameraID]!!
        } else {
            camera = DeviceSurfaces(openCamera(cameraID.openID))
            cameraDevices[cameraID] = camera
            Log.i(TAG, "added new surface. id=${cameraID}")
        }
        return camera
    }

    /**
     * Call after configuring cameras.
     *
     * @param configure Optional Lambda to do additional configurations for a camera.
     */
    protected fun initializeCamera(configure: (cameraID: CameraID, builder: CaptureRequest.Builder) -> Unit = { _, _ -> }) =
        lifecycleScope.launch(Dispatchers.Main) {
            Log.i(TAG, "Inside initializeCamera")

            if (cameraDevices.isEmpty())
                throw RuntimeException("Need to add camera surfaces first")

            for (cameraID in cameraDevices.keys) {
                val camera = cameraDevices[cameraID]!!
                val requestBuilder = camera.device.createCaptureRequest(captureRequestTemplateType)
                for (surface in camera.surfaces) {
                    requestBuilder.addTarget(surface)
                }

                // Special configurations
                configure.invoke(cameraID, requestBuilder)

                val session = createCaptureSession(cameraID, camera)
                session.setRepeatingRequest(
                    requestBuilder.build(),
                    createCaptureRequestListener(camera),
                    cameraHandler
                )

                camera.session = session
            }

            invokeAfterCameraInitialize.invoke()
        }

    /**
     * Used to provide a capture request listener. By default null is returned.
     */
    open fun createCaptureRequestListener(camera: DeviceSurfaces): CameraCaptureSession.CaptureCallback? {
        return null
    }

    /**
     * Changes the camera's zoom level. With a Pixel 6 Pro this will be done by down sampling
     * the image less up to 2x zoom, after that it will be a digital zoom.
     */
    public fun setZoom(cameraID: String, zoomValue: Double, builder: CaptureRequest.Builder) {
        val characteristics = cameraManager.getCameraCharacteristics(cameraID)
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
        val width = sensorSize.width()
        val height = sensorSize.height()
        val centerX = width / 2
        val centerY = height / 2
        val deltaX = (width * 0.5 / zoomValue).roundToInt()
        val deltaY = (height * 0.5 / zoomValue).roundToInt()

        val cropRegion =
            Rect(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY)
        builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraID: String): CameraDevice {
        val results = OpenResult()
        cameraManager.openCamera(cameraID, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.i(TAG, "Camera $cameraID has opened")
                results.camera = camera
                results.finished = true
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.w(TAG, "Camera $cameraID has been disconnected. null-activity=${activity == null}")
                activity?.finish()
                results.finished = true
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraID error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                results.finished = true
            }
        }, cameraHandler)

        // If there's a coroutine way to do this we should do this. The problem with
        // suspendCancelableCoroutine is that it didn't lend itself towards configuring
        // multiple cameras / surfaces by calling functions to add them. It would load
        // the first camera, suspend then go to the next function before it initialized
        // the first camera. Causing bad stuff
        val startTime = System.currentTimeMillis()
        while (!results.finished && System.currentTimeMillis() < startTime + 10_000L) {
            Thread.yield()
        }

        if (results.camera == null)
            Log.e(TAG, "Failed to get camera object when opening camera.")

        return results.camera!!
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(cameraID: CameraID, camera: DeviceSurfaces):
            CameraCaptureSession = suspendCoroutine { cont ->

        // Configure it so it can point a camera inside a multi-camera system
        val configurations = ArrayList<OutputConfiguration>()
        for (surface in camera.surfaces) {
            val config = OutputConfiguration(surface)
            if (!cameraID.isLogical) {
                config.setPhysicalCameraId(cameraID.id)
            }
            configurations.add(config)
        }

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        camera.device.createCaptureSessionByOutputConfigurations(
            configurations,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val exc = RuntimeException("Camera $cameraID session configuration failed")
                    Log.e(TAG, exc.message, exc)
                    cont.resumeWithException(exc)
                }
            },
            cameraHandler
        )
    }

    /**
     * Handle new images from the camera when they are available
     */
    private fun createOnAvailableListener(op: (image: Image) -> Unit): ImageReader.OnImageAvailableListener {
        return ImageReader.OnImageAvailableListener { reader ->
            reader.acquireNextImage().use { dataFrame ->
                // This can happen if the process is closed out of order
                if (dataFrame == null) {
                    Log.e(TAG, "null dataFrame")
                    return@use
                }
                // Catch the issue here to prevent it from becomming a big issue
                try {
                    op.invoke(dataFrame)
                } catch (e: Throwable) {
                    Log.e(TAG, "Exception in acquireNextImage", e)
                }
            }
        }
    }

    /**
     * Selects the resolution which has the closest number of pixels to the target
     */
    fun selectResolution(id: String, targetWidth: Int, targetHeight: Int): Size {
        // Search to find best match
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
        val sizes = map!!.getOutputSizes(imageFormat)

        var bestSize = sizes[0]
        var bestError = sizeError(bestSize, targetWidth, targetHeight)
        for (i in 1 until sizes.size) {
            val error = sizeError(sizes[i], targetWidth, targetHeight)
            if (error >= bestError)
                continue
            bestError = error
            bestSize = sizes[i]
        }

        Log.i(
            TAG,
            "resolution: id='$id' target={${targetWidth}x${targetHeight}} selected={${bestSize.width}x${bestSize.height}}"
        )

        return bestSize
    }

    /** Computes how different the passes in size is from the requested image size */
    private fun sizeError(size: Size, targetWidth: Int, targetHeight: Int): Int {
        return abs(size.width - targetWidth) + abs(size.height - targetHeight)
    }

    /**
     * Camera device, it's surfaces, and image readers
     */
    inner class DeviceSurfaces(val device: CameraDevice) {
        val surfaces = ArrayList<Surface>()
        val readers = ArrayList<ImageReader>()
        var session: CameraCaptureSession? = null
    }

    /**
     * Keeps track of asynchronous state when opening a camera
     */
    inner class OpenResult {
        var exception: Exception? = null
        var camera: CameraDevice? = null
        var finished = false
    }

    companion object {
        private const val TAG = "CameraProcFrag"

        /** Maximum number of images that will be held in the reader's buffer */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        private const val imageFormat = ImageFormat.YUV_420_888
    }
}