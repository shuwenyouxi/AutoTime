package com.dsw.sample

import android.app.Application
import com.dsw.autotime.AutoTime

/**
 * Created by Shuwen Dai on 2019-07-23
 * @author shuwenyouxi@gmail.cn
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initAutotime()
    }

    private fun initAutotime() {
        AutoTime.subscribeNetworkChange(this).easyInitialize()
    }
}