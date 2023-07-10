package org.boofcv.android.fragments

import android.graphics.Canvas
import android.graphics.Matrix

/**
 * Interface for processing camera images in a work thread direct from the camera
 * then visualizing results
 */
interface AndroidImageProcessing {
    /**
     * Perform time consuming image processing here
     */
    fun process(frame: LockImages)

    /**
     * Visualizes most recently processed images on top of the captured image
     */
    fun visualize(canvas: Canvas, imageToView: Matrix)
}