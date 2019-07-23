# AutoTime
Sync server time by NTP, this project adopts the second plan.

此库用来获取到真实的时间, 不受用户修改系统时间影响. 

一共想了三个方案,最终权衡下选择方案二.
___

Download
========
```gradle
dependencies {
    implementation 'com.github.shuwenyouxi:AutoTime:v1.0'
}

```



## 方案一: TimeManager(否决)
[TimeManager](https://developer.android.com/reference/com/google/android/things/device/TimeManager.html#setautotimeenabled)

该类提供了开启系统同步时间的方法
> Sets whether or not wall clock time should sync with automatic time updates from NTP.
```java
void setAutoTimeEnabled (boolean enabled)
```
 但是该类需要**gms**支持,国内基本用不了,只能pass了

## 方案二: SntpClient + SystemClock(该项目采用)

1. 在application初始化的时候, 用`SntpClient`从ntp服务器上获取服务器时间戳. 获取成功后, 将结果记为`sntpTime`, 同时, 用一个变量`syncTime`记录`SystemClock.elapsedRealtime()`. 若调用失败, 则这两个变量均为默认值0.
2. 当需要获取正确时间戳的时候, 判断`sntpTime`和`syncTime`是不是为0. 若非0, 则当前服务器时间值为`SystemClock.elapsedRealtime() - syncTime + sntpTime`, 否则直接返回当前系统时间戳`System.currentTimeMillis()`.

3. 在网络恢复连接时检查同步情况, 发现未同步则进行一次同步

**ps:** `SystemClock.elapsedRealtime()`记录的是系统自启动以来的时间间隔, 用户无法修改. 官网介绍如下: 
> return the time since the system was booted, and include deep sleep. This clock is guaranteed to be monotonic, and continues to tick even when the CPU is in power saving modes, so is the recommend basis for general purpose interval timing.

### SntpClient选择
- [官网提供](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/net/SntpClient.java)
java.net包下提供一个SntpClient, 不过该类是`{@hide}`, 要使用的话只能用反射或者直接拷贝源码. 在比较新的代码里面这个方法被标记过时了, 而且官方似乎有意不让用户调用
```java
@Deprecated
@UnsupportedAppUsage
public boolean requestTime(String host, int timeout) {
    Log.w(TAG, "Shame on you for calling the hidden API requestTime()!");
    return false;
}
```

- [github开源](https://github.com/instacart/truetime-android/blob/master/library/src/main/java/com/instacart/library/truetime/SntpClient.java)核心代码相似, 估计是官方的老代码. 自己跑了一遍没问题, 但是不确定靠不靠谱. 

### NTP服务器
阿里提供了七个免费的,外网可用
- ntp1.aliyun.com
- ntp2.aliyun.com
- ntp3.aliyun.com
- ntp4.aliyun.com
- ntp5.aliyun.com
- ntp6.aliyun.com
- ntp7.aliyun.com

## 方案三: OkHttp + SystemClock
大致思路同方案二, 区别在于方案二获取服务器时间用`SntpClient`, 方案三用`Okhttp`的拦截器获取response的header的Date
```kotlin
class TimeCalibrationInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val responseTime = System.nanoTime() - startTime

        val headers = response.headers()
        calibration(responseTime, headers)
        return response
    }

    private fun calibration(responseTime: Long, headers: Headers?) {
        headers?.get("Date")
                ?.takeIf { it.isNotEmpty() }
                ?.also {
                    val parse = HttpDate.parse(it)
                    TimeManager.getInstance().considerSyncTime(parse)
                }
    }
}
```
**ps** 用AtomicLong记录服务器时间戳,线程安全,且效率比锁高,因此不用担心频繁更新的问题