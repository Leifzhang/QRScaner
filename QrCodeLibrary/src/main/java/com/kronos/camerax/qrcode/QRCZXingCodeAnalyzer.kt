package com.kronos.camerax.qrcode

import android.graphics.ImageFormat
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import me.devilsen.czxing.code.BarcodeReader
import me.devilsen.czxing.code.CodeResult
import me.devilsen.czxing.utils.BarCodeUtil
import kotlin.math.abs
import kotlin.math.sqrt

class QRCZXingCodeAnalyzer(private val module: CameraXModule, function: (String) -> Unit) :
    ImageAnalysis.Analyzer, BarcodeReader.ReadCodeListener {

    private val qrReader = BarcodeReader.getInstance().apply {
        setReadCodeListener(this@QRCZXingCodeAnalyzer)
        prepareRead()
    }


    private val yuvFormats = mutableListOf(ImageFormat.YUV_420_888)

    private var pauseImage: ImageProxy? = null

    private val listener: QrCodeCallBack = object : QrCodeCallBack {

        override fun onQrCode(text: String) {
            function(text)
        }

    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormats.addAll(listOf(ImageFormat.YUV_422_888, ImageFormat.YUV_444_888))
        }
    }

    var camera: Camera? = null
    var preview: Preview? = null

    override fun analyze(image: ImageProxy) {
        if (image.format !in yuvFormats) {
            return
        }
        val startTime = System.currentTimeMillis()

        val data = image.planes[0].buffer.toByteArray()
        val height = image.height
        val width = image.width
        pauseImage = image
        try {
            val result = qrReader.read(data, 0, 0, width, height, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        image.close()
    }

    private fun zoomCamera(points: CodeResult, image: ImageProxy): Boolean {
        val qrWidth = calculateDistance(points.points)
        val imageWidth = image.width.toFloat()
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

    private fun calculateDistance(resultPoint: FloatArray): Int {
        var len = 0
        val points: FloatArray = resultPoint
        if (points.size > 3) {
            val point1X = points[0]
            val point1Y = points[1]
            val point2X = points[2]
            val point2Y = points[3]
            val xLen = abs(point1X - point2X)
            val yLen = abs(point1Y - point2Y)
            len = sqrt(xLen * xLen + yLen * yLen.toDouble()).toInt()
        }

        if (points.size > 5) {
            val point2X = points[2]
            val point2Y = points[3]
            val point3X = points[4]
            val point3Y = points[5]
            val xLen = abs(point2X - point3X)
            val yLen = abs(point2Y - point3Y)
            val len2 = sqrt(xLen * xLen + yLen * yLen.toDouble()).toInt()
            if (len2 < len) {
                len = len2
            }
        }
        return len
    }

    internal fun resetAnalyzer() {
        qrReader.prepareRead()
        pauseImage?.close()
    }

    override fun onAnalysisBrightness(isDark: Boolean) {

    }


    override fun onFocus() {

    }

    override fun onReadCodeResult(result: CodeResult?) {
        if (result == null) {
            return
        }
        BarCodeUtil.d("result : $result")
        if (!TextUtils.isEmpty(result.text)) {
            qrReader.stopRead()
            listener.onQrCode(result.text)
        } else if (result.points != null) {
            pauseImage?.let { zoomCamera(result, it) }
        }
    }

}