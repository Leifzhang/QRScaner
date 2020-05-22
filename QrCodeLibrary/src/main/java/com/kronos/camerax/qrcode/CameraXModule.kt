package com.kronos.camerax.qrcode

import android.annotation.SuppressLint
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.Result
import kotlinx.android.synthetic.main.view_qr_scan_auto_zoom.view.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraXModule(private val view: AutoZoomScanView) {

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private lateinit var qrCodeAnalyzer: QRCodeAnalyzer
    private lateinit var mLifecycleOwner: LifecycleOwner


    fun bindWithCameraX(function: (Result) -> Unit, lifecycleOwner: LifecycleOwner) {
        mLifecycleOwner = lifecycleOwner
        val metrics = DisplayMetrics().also { view.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.i(TAG, "Preview aspect ratio: $screenAspectRatio")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(view.context)
        cameraProviderFuture.addListener(
            Runnable {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                // Preview
                val width = (view.measuredWidth * 1.5F).toInt()
                val height = (width * screenAspectRatio).toInt()
                preview = Preview.Builder()
                    // We request aspect ratio but no resolution
                    .setTargetResolution(Size(width, height))
                    // Set initial target rotation
                    .build()
                preview?.setSurfaceProvider(view.preView.createSurfaceProvider(null))

                cameraExecutor = Executors.newSingleThreadExecutor()
                qrCodeAnalyzer = QRCodeAnalyzer(this) { function(it) }
                // ImageAnalysis
                imageAnalyzer = ImageAnalysis.Builder()
                    // We request aspect ratio but no resolution
                    .setTargetResolution(Size(width, height))
                    // Set initial target rotation, we will have to call this again if rotation changes
                    // during the lifecycle of this use case
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {
                        it.setAnalyzer(cameraExecutor, qrCodeAnalyzer)
                    }

                // Must unbind the use-cases before rebinding them
                cameraProvider.unbindAll()

                try {
                    // A variable number of use-cases can be passed here -
                    // camera provides access to CameraControl & CameraInfo

                    camera = cameraProvider.bindToLifecycle(
                        mLifecycleOwner, cameraSelector, preview, imageAnalyzer
                    )
                    qrCodeAnalyzer.camera = camera
                    qrCodeAnalyzer.preview = preview
                    setFocus(view.width.toFloat() / 2, view.height.toFloat() / 2)
                    // camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.FLAG_AF)
                    // Attach the viewfinder's surface provider to preview use case
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(view.context)
        )
    }

    fun setFocus(x: Float, y: Float) {
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            view.width.toFloat(), view.height.toFloat()
        )
        //create a point on the center of the view
        val autoFocusPoint = factory.createPoint(x, y)

        camera?.cameraControl?.startFocusAndMetering(
            FocusMeteringAction.Builder(
                autoFocusPoint,
                FocusMeteringAction.FLAG_AF
            ).apply {
                //auto-focus every 1 seconds
                setAutoCancelDuration(1, TimeUnit.SECONDS)
            }.build()
        )
    }

    private fun aspectRatio(width: Int, height: Int): Double {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return RATIO_4_3_VALUE
        }
        return RATIO_16_9_VALUE
    }

    @SuppressLint("RestrictedApi")
    fun setZoomRatio(zoomRatio: Float) {
        if (zoomRatio > getMaxZoomRatio()) {
            return
        }
        val future: ListenableFuture<Void>? = camera?.cameraControl?.setZoomRatio(
            zoomRatio
        )
        future?.apply {
            Futures.addCallback(future, object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {}
                override fun onFailure(t: Throwable) {}
            }, CameraXExecutors.directExecutor())
        }
    }

    fun getZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 0F
    }

    fun getMaxZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 0F
    }

    fun stopCamera() {
        //   camera?.cameraControl?.
    }

    internal fun resetAnalyzer() {
        qrCodeAnalyzer.resetAnalyzer()
    }

    companion object {
        private const val TAG = "CameraXImp"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}