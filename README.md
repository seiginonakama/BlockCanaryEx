# BlockCanaryEx

detect the method which cost time in main thread and leading you app blocked, base on BlockCanary(now called AndroidPerformanceMonitor)

![sample](https://raw.githubusercontent.com/lqcandqq13/BlockCanaryEx/master/sample.jpeg)



#### what's the difference between BlockCanaryEx and BlockCanary?

- BlockCanaryEx base on BlockCanary, inherit it's ui and most of features;
- BlockCanaryEx add MethodSampler, knows every method's execute-info (like cost-time, called-times...) when blocked;
- BlockCanaryEx focus on the method which cost most of time when our app blocked, and display it directly to developer.

#### Basic Usage

 watch your app with BlockCanaryEx when debug mode 

1. apply plugin (TODO: upload to jcenter)

   ```
   classpath 'xxxx'
   ```

   ```
   apply plugin: 'blockcanaryex'
   ```

2. add dependencies (TODO: upload to jcenter)

   ```
   compile project(':BlockCanaryExJRT')
   ```

3. init BlockCanaryEx when your application created

   ```
   public class TestApplication extends Application {
       @Override
       public void onCreate() {
           super.onCreate();
           BlockCanaryEx.install(new Config(this));
       }
   }
   ```
