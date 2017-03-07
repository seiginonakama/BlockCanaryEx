

# BlockCanaryEx

**detect the method which cost time in main thread and leading you app blocked, base on BlockCanary(now called AndroidPerformanceMonitor)**

<img src="https://raw.githubusercontent.com/lqcandqq13/BlockCanaryEx/master/sample.jpeg" width = "480" alt="sample" align=center />


#### what's the difference between BlockCanaryEx and BlockCanary?

- BlockCanaryEx base on BlockCanary, inherit it's ui and most of features;
- BlockCanaryEx add MethodSampler, knows every method's execute-info (like cost-time, called-times...) when blocked;
- BlockCanaryEx focus on the method which cost most of time when our app blocked, and display it directly to developer.

#### Basic Usage

 watch your app with BlockCanaryEx when debug mode 

- apply BlockCanaryExPlugin (TODO: upload to jcenter)

```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath xxxx
    }
}
```

```
apply plugin: 'blockcanaryex'
```
- add BlockCanaryExJRT dependencies (TODO: upload to jcenter)

```
compile project(':BlockCanaryExJRT')
```
- init BlockCanaryEx when your application created

```
public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BlockCanaryEx.install(new Config(this));
    }
}
```
done, now BlockCanaryEx be enabled when you app in debug mode.

#### Advance Usage

customize BlockCanaryEx

* customize MethodSampler scope in model build.gradle

  ```
  apply plugin: 'blockcanaryex'

  block {
      debugEnabled true //enable MethodSampler when debug mode, default true
      releaseEnabled false //enable MethodSampler when release mode, default false
      excludePackages [] //exclude the package you don't want to inject MethodSampler, eg: ['com.android', 'android.support']
      excludeClasses [] //exclude the class you don't want to inject MethodSampler
      includePackages [] //only include the package you want to inject MethodSampler, packages which don't included will not be injected

      scope {
          project true //inject MethodSampler for app project, default true
          projectLocalDep false //inject MethodSampler for app libs(eg: .jar), default false
          subProject true //inject MethodSampler for subProject of app project, default true
          subProjectLocalDep false //inject MethodSampler for subProject libs, default false
          externalLibraries false //inject MethodSampler external libs, default false
      }
  }
  ```

* override more Config method to customize BlockCanaryEx runtime

  ```
  public class TestApplication extends Application {
      @Override
      public void onCreate() {
          super.onCreate();
          BlockCanaryEx.install(new Config(this) {
              /**
               * provide the looper to watch, default is Looper.mainLooper()
               *
               * @return the looper you want to watch
               */
              public Looper provideWatchLooper() {
                  return Looper.getMainLooper();
              }

              /**
               * If need notification to notice block.
               *
               * @return true if need, else if not need.
               */
              public boolean displayNotification() {
                  return true;
              }

              /**
               * judge whether the loop is blocked, you can override this to decide
               * whether it is blocked by your logic
               *
               * Note: running in none ui thread
               *
               * @param startTime in mills
               * @param endTime in mills
               * @param startThreadTime in mills
               * @param endThreadTime in mills
               * @return true if blocked, else false
               */
              public boolean isBlock(long startTime, long endTime, long startThreadTime, long endThreadTime) {
                  long costRealTime = endTime - startTime;
                  return costRealTime > 100L && costRealTime < 2 * (endThreadTime - startThreadTime);
              }

              /**
               * judge whether the method is heavy method, we will print heavy method in log
               *
               * Note: running in none ui thread
               *
               * @param methodInfo {@link com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo}
               * @return true if it is heavy method, else false
               */
              public boolean isHeavyMethod(com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo methodInfo) {
                  return methodInfo.getCostThreadTime() >= 1L;
              }

              /**
               * judge whether the method is called frequently, we will print frequent method in log
               *
               * Note: running in none ui thread
               *
               * @param frequentMethodInfo the execute info of same method in this loop {@link FrequentMethodInfo}
               * @return true if it is frequent method, else false
               */
              public boolean isFrequentMethod(FrequentMethodInfo frequentMethodInfo) {
                  return frequentMethodInfo.getTotalCostRealTimeMs() > 1L && frequentMethodInfo.getCalledTimes() > 1;
              }

              /**
               * Path to save log, like "/blockcanary/", will save to sdcard if can, else we will save to
               * "${context.getFilesDir()/${provideLogPath()}"}"
               *
               * Note: running in none ui thread
               *
               * @return path of log files
               */
              public String provideLogPath() {
                  return "/blockcanaryex/" + getContext().getPackageName() + "/";
              }

              /**
               * Network type to record in log, you should impl this if you want to record this
               *
               * @return {@link String} like 2G, 3G, 4G, wifi, etc.
               */
              public String provideNetworkType() {
                  return "unknown";
              }

              /**
               * unique id to record in log, you should impl this if you want to record this
               *
               * @return {@link String} like imei, account id...
               */
              public String provideUid() {
                  return "unknown";
              }

              /**
               * Implement in your project.
               *
               * @return Qualifier which can specify this installation, like version + flavor.
               */
              @TargetApi(Build.VERSION_CODES.DONUT)
              public String provideQualifier() {
                  PackageInfo packageInfo = ProcessUtils.getPackageInfo(getContext());
                  ApplicationInfo applicationInfo = getContext().getApplicationInfo();
                  if(packageInfo != null) {
                      return applicationInfo.name + "-" + packageInfo.versionName;
                  }
                  return "unknown";
              }

              /**
               * Block listener, developer may provide their own actions
               *
               * @param blockInfo {@link BlockInfo}
               */
              @Override
              public void onBlock(BlockInfo blockInfo) {
              }
          });
      }
  }
  ```