package com.medibox

import android.app.Application

/**
 * Robolectric 专用测试 Application。
 *
 * 目的只有一个：避免单元测试在启动时执行生产 MediBoxApp.onCreate() 的数据库播种副作用。
 */
class TestMediBoxApplication : Application()
