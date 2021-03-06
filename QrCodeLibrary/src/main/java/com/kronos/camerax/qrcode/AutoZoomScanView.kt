package com.kronos.camerax.qrcode

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.view_qr_scan_auto_zoom.view.*

class AutoZoomScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cameraXModule: CameraXModule
    private var resultListener: OnQrResultListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_qr_scan_auto_zoom, this)
        cameraXModule = CameraXModule(this)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun bindWithLifeCycle(lifecycleOwner: LifecycleOwner) {
        preView.post {
            cameraXModule.bindWithCameraX({
                try {
                    preView.post {
                        resultListener?.onSuccess(this, it.text)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, lifecycleOwner)
        }
        apply { }
    }

    fun stopCamera() {
        // cameraXModule.
    }

    fun reStart() {
        cameraXModule.resetAnalyzer()
    }

    fun setOnQrResultListener(block: (View, String) -> Unit) {
        resultListener = object : OnQrResultListener {
            override fun onSuccess(view: View, qrResult: String) {
                block.invoke(view, qrResult)
            }
        }
    }

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                cameraXModule.setZoomRatio(cameraXModule.getZoomRatio() + 1)
                return super.onDoubleTap(e)
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                e?.apply {
                    cameraXModule.setFocus(x, y)
                }
                return super.onSingleTapUp(e)
            }
        })

}