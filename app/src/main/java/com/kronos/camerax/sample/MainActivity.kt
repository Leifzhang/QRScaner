package com.kronos.camerax.sample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndPermission.with(this)
            .runtime()
            .permission(Permission.Group.CAMERA)
            .onGranted { permissions: List<String?>? ->
                scanView.bindWithLifeCycle(this@MainActivity)
            }
            .onDenied { permissions: List<String?>? -> }
            .start()
        scanView.setOnQrResultListener { view: View, s: String ->
            Toast.makeText(
                this@MainActivity, s,
                Toast.LENGTH_LONG
            ).show()
            scanView.reStart()
        }
    }
}
