package org.boofcv.android.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import boofcv.alg.color.ColorFormat
import boofcv.android.BoofAndroidUtils
import boofcv.android.ConvertCameraImage
import boofcv.android.camera2.CameraID
import boofcv.android.fragments.CameraProcessFragment
import boofcv.misc.BoofMiscOps
import boofcv.misc.VariableLockSet
import boofcv.struct.image.GrayU8
import boofcv.struct.image.ImageDimension
import boofcv.struct.image.Planar
import org.ddogleg.struct.DogArray_I8
import pabeles.concurrency.GrowArray


/**
 * Image data that used by the frame grabber and the image processing thread
 */
class LockImages : VariableLockSet() {
    // If true then data structures are being used in the processing thread
    var processing = false

    // Unique ID for this data frame
    var sequenceID = 0L

    // When the image was captured at
    var timestamp = 0L

    // Converted camera image
    val color = Planar(GrayU8::class.java, 1, 1, 3)
    val gray = GrayU8(1, 1)

    /** Request that it be marked as processing. Returns false if already processing */
    fun requestProcess(): Boolean {
        lock()
        try {
            if (processing)
                return false
            processing = true
            return true
        } finally {
            unlock()
        }
    }

    /** Safely marks the thread as not being processed. */
    fun releaseProcessing() {
        lock()
        try {
            if (!processing)
                throw RuntimeException("Not processing and release requested!")
            processing = false
        } finally {
            unlock()
        }
    }
}

/**
 * Automatically converts Image into a format that BoofCV understands and
 * creates a surface for visualizing results. A common API is provided for image
 * processing.
 */
abstract class ImageProcessingFragment : CameraProcessFragment() {
    // Images that are being written to by the frame grabber thread then read by the image
    // processing thread. The IP thread might sway these two variables, so the grabber thread
    // needs to take that into consideration
    private var imagesCam = LockImages()
    private var imagesPro = LockImages()
    private var frameCount = 0L

    // external image processing logic
    protected var imageProcessing: AndroidImageProcessing? = null

    // Thread that image processing is done inside of
    private var imageProcessingThread = ProcessThread()

    private lateinit var displayView: DisplayView

    protected val displayMetrics: DisplayMetrics by lazy { Resources.getSystem().displayMetrics }

    /** Resolution of images from camera after done configuring */
    var cameraResolution = Size(0, 0)

    // Performance statistics
    private val timeConvertMS = MovingAverageStats()
    private val timeProcessingMS = MovingAverageStats()

    /**
     * Stores the transform from the video image to the view it is displayed on. Handles
     * All those annoying rotations.
     */
    protected val imageToView = Matrix()
    protected val viewToImage = Matrix()

    // Image processing workspace
    private val workspace = GrowArray { DogArray_I8() }

    override fun onDestroyView() {
        imageProcessingThread.requestStop = true
        super.onDestroyView()
    }

    /**
     * Handles the most recent [Image] from the camera. Keeps track of the number of threads.
     * If it's still processing the previous image it will return. Otherwise it will convert
     * the image into a usable format by BoofCV then tell the processing thread that another
     * image is available.
     */
    fun processImage(dataFrame: Image) {
        frameCount++
        val image = imagesCam
        if (!image.requestProcess())
            return

        val time0 = System.nanoTime()
        // Perform a fast copy from the camera image into a format we can understand
        // YUV format requires a simple copy to get a gray image
        ConvertCameraImage.imageToBoof(dataFrame, ColorFormat.RGB, image.gray, workspace)
        ConvertCameraImage.imageToBoof(dataFrame, ColorFormat.RGB, image.color, workspace)
        image.timestamp = dataFrame.timestamp
        image.sequenceID = frameCount
        image.releaseProcessing()
        val time1 = System.nanoTime()

        // Print out profiling information for debugging
        timeConvertMS.update((time1 - time0) * 1e-6)
        if (frameCount % 500 == 0L)
            Log.i(
                TAG,
                "Profiling. frames: %6d convert: %6.2f (ms) process: %6.2f (ms)".format(
                    frameCount,
                    timeConvertMS.average,
                    timeProcessingMS.average
                )
            )
    }

    /** Returns the frame used to display camera image */
    abstract fun getCameraFrame(): FrameLayout

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayView = DisplayView(requireContext())
        val layout = getCameraFrame()
        layout.addView(
            displayView, layout.childCount, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        displayView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            // load camera settings
            val cameraID = CameraID("")
            val requestedResolution = ImageDimension()
            lookupCameraConfig(cameraID, requestedResolution)

            val rotation = requireActivity().windowManager.defaultDisplay.rotation
            invokeAfterCameraInitialize = {
                initializeCameraCoordinates(cameraID, right - left, bottom - top, rotation)
            }

            // Add image processor for computer vision stream
            val reader = addCameraProcessor(
                cameraID,
                requestedResolution.width,
                requestedResolution.height
            ) { image -> processImage(image) }

            // Save actual resolution of frame grabber.
            cameraResolution = Size(reader.width, reader.height)

            initializeCamera { id, builder -> customConfigure(id, builder) }
        }
    }

    /**
     * Used to lookup which camera it should open and at what resolution
     *
     * @param cameraID (Output) Which camera it should open
     * @param resolution (Output) At what resolution it should open
     */
    abstract fun lookupCameraConfig(cameraID: CameraID, resolution: ImageDimension)

    /**
     * This is called when initializing the camera and allows additional settings to be
     * applied.
     *
     * @param cameraID The camera being configured
     * @param builder (Input/Output) Used to configure the camera
     */
    abstract fun customConfigure(cameraID: CameraID, builder: CaptureRequest.Builder)

    /**
     * Initializes the camera for capture and relevant transforms for visualization
     */
    protected fun initializeCameraCoordinates(
        cameraID: CameraID, viewWidth: Int, viewHeight: Int, displayRotation: Int
    ) {
        // Make sure the camera has been initialized
        BoofMiscOps.checkTrue(cameraResolution.width > 0 && cameraResolution.height > 0)

        // Compute transforms to properly display the camera image inside the displayView
        val characteristics = cameraManager.getCameraCharacteristics(cameraID.openID)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        BoofAndroidUtils.videoToDisplayMatrix(
            cameraResolution.width, cameraResolution.height, sensorOrientation,
            viewWidth, viewHeight, displayRotation * 90, false, imageToView
        )
        BoofMiscOps.checkTrue(imageToView.invert(viewToImage))

        // Start processing thread for images
        imageProcessingThread = ProcessThread()
        imageProcessingThread.start()
    }

    /**
     * Thread used to do image processing. If there are no new images to process then it will
     * sleep. A thread pool isn't used here to minimize latency and potentially respawning
     * threads. Yes I know some pools do use a single thread...
     */
    private inner class ProcessThread : Thread("ImageProcessing") {
        // when true it will shut down the thread in the next cycle
        var requestStop = false
        override fun run() {
            // used to keep track of threads and see if they need
            var previousFrame = 0L
            while (!requestStop) {
                // Sleep when it runs out of new data to process
                var shouldSleep = true

                // Swap in the latest image in
                if (imagesCam.requestProcess()) {
                    val tmp = imagesCam
                    imagesCam = imagesPro
                    imagesPro = tmp
                    tmp.releaseProcessing()
                }

                // Process only if it's newer
                if (imagesPro.sequenceID > previousFrame) {
                    // don't want it to sleep just in case another image is ready right after
                    // this is done processing
                    shouldSleep = false
                    previousFrame = imagesPro.sequenceID
                    val time0 = System.nanoTime()
                    try {
                        imageProcessing!!.process(imagesPro)
                    } catch (e: Exception) {
                        Log.e(TAG, "Caught exception in image processing!", e)
                    }
                    val time1 = System.nanoTime()
                    timeProcessingMS.update((time1 - time0) * 1e-6)

                    // Tell the UI to update
                    activity?.runOnUiThread { displayView.invalidate() }
                }

                // Can't interrupt this thread as it will also take down concurrency in BoofCV
                if (shouldSleep && !requestStop) {
                    try {
                        yield() // avoid sucking up all CPU
                    } catch (ignore: InterruptedException) {
                    }
                }
            }
        }
    }

    /**
     * Called whenever the UI decides it needs to update
     */
    open fun onDrawFrame(view: SurfaceView, canvas: Canvas) {
        imageProcessing!!.visualize(canvas, imageToView)
    }

    /**
     * Display for the camera preview. Need to have something
     */
    inner class DisplayView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
        init {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas?) {
            onDrawFrame(this, canvas!!)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {}
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    companion object {
        private const val TAG = "ImgProcFrag"
    }
}