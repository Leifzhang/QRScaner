package com.kronos.camerax.qrcode

import android.view.View

interface OnQrResultListener {
    fun onSuccess(view: View, qrResult: String)
}