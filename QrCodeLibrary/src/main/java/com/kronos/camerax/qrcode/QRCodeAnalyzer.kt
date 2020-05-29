package com.kronos.camerax.qrcode

import android.graphics.ImageFormat.*
import android.os.Build
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.detector.Detector
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt


class QRCodeAnalyzer(private val module: CameraXModule, function: (String) -> Unit) :
    ImageAnalysis.Analyzer {

    private val map = mapOf<DecodeHintType, Collection<BarcodeFormat>>(
        Pair(DecodeHintType.POSSIBLE_FORMATS, arrayListOf(BarcodeFormat.QR_CODE))
    )
    private val reader: MultiFormatReader = MultiFormatReader().apply {
        setHints(map)
    }

    private val yuvFormats = mutableListOf(YUV_420_888)
    private var pauseImage: ImageProxy? = null

    private val listener: QrCodeCallBack = object : QrCodeCallBack {


        override fun onQrCode(text: String) {
            function(text)
        }

    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormats.addAll(listOf(YUV_422_888, YUV_444_888))
        }
    }

    var camera: Camera? = null
    var preview: Preview? = null

    override fun analyze(image: ImageProxy) {
        if (image.format !in yuvFormats) {
            return
        }
        val startTime = System.currentTimeMillis();

        val data = image.planes[0].buffer.toByteArray()
        val height = image.height
        val width = image.width
        val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
        val binarizer = HybridBinarizer(source)
        val bitmap = BinaryBitmap(binarizer)
        try {
            val detectorResult = Detector(bitmap.blackMatrix).detect(map)
            if (zoomCamera(detectorResult.points, bitmap)) {
                image.close()
                return
            }
            val result = reader.decode(bitmap)
            Log.i(
                "BarcodeAnalyzer",
                "resolved!!! = $result  timeUsage:${System.currentTimeMillis() - startTime}"
            )
            pauseImage = image
            listener.onQrCode(result.text)
        } catch (e: Exception) {
            // e.printStackTrace()
            image.close()
        }
    }

    private fun zoomCamera(points: Array<ResultPoint>, image: BinaryBitmap): Boolean {
        val qrWidth = calculateDistance(points) * 2
        val imageWidth = image.blackMatrix.width.toFloat()
        val zoomInfo = camera?.cameraInfo?.zoomState?.value
        zoomInfo?.apply {
            if (qrWidth < imageWidth / 8) {
                Log.i("BarcodeAnalyzer", "resolved!!! = $qrWidth  imageWidth:${imageWidth}")
                val maxScale = zoomInfo.maxZoomRatio
                val curValue = zoomInfo.zoomRatio
                val gap = maxScale - curValue
                val upgradeRatio = if (gap / 4F * 3 > 3F) 3F else maxScale / 4F * 3
                module.setZoomRatio(curValue + upgradeRatio)
                return true
            }
        }
        return false
    }

    private fun calculateDistance(resultPoint: Array<ResultPoint>): Int {
        val point1X = resultPoint[0].x.toInt()
        val point1Y = resultPoint[0].y.toInt()
        val point2X = resultPoint[1].x.toInt()
        val point2Y = resultPoint[1].y.toInt()
        return sqrt(
            (point1X - point2X.toDouble()).pow(2.0) + (point1Y - point2Y.toDouble()).pow(2.0)
        ).toInt()
    }

    internal fun resetAnalyzer() {
        pauseImage?.close()
    }

}

interface QrCodeCallBack {
    fun onQrCode(text: String)
}

internal fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}