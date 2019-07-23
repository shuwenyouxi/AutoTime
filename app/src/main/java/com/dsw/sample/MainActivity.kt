package com.dsw.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dsw.autotime.AutoTime
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        btn_print.setOnClickListener {
            tv_device_time.text = format.format(System.currentTimeMillis())
            tv_server_time.text = format.format(AutoTime.currentTimeMillis()) + "hasSynced: ${AutoTime.hasInitialized()}"
        }
    }
}