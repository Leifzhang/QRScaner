# QRScaner


##  简介

基于CameraX beta版本和zxing对二维码扫描进行封装

修正了preview分辨率过低的问题，同时参照camreaview的方式对scanview进行了一次调整

## 功能介绍

* 二维码距离远自动放大

* 自动对焦

* 单击focus

* 双击放大



## 简单使用

1. 引入依赖

~~~ gradle
implementation 'com.github.leifzhang:QrCodeLibrary:0.0.1'
~~~


2. 在布局xml中加入AutoZoomScanView

~~~ xml
    <com.kronos.camerax.qrcode.AutoZoomScanView
        android:id="@+id/scanView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
~~~

3. 先申请camera权限并绑定lifecycle

~~~ kotlin
    AndPermission.with(this)
            .runtime()
            .permission(Permission.Group.CAMERA)
            .onGranted { permissions: List<String?>? ->
                scanView.bindWithLifeCycle(this@MainActivity)
            }
            .onDenied { permissions: List<String?>? -> }
            .start()
~~~

4. 二维码结果回调，之后重新打开分析逻辑

~~~ kotlin
 scanView.setOnQrResultListener { view: View, s: String ->
            Toast.makeText(
                this@MainActivity, s,
                Toast.LENGTH_LONG
            ).show()
            scanView.reStart()
        }
~~~

简单的可以直接参考sample内