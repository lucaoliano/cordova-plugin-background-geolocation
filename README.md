# cordova-plugin-mauron85-background-geolocation

## Description

Cross-platform geolocation for Cordova / PhoneGap with battery-saving "circular region monitoring" and "stop detection".

Plugin is both foreground and background geolocation service. It is far more battery and data efficient then html5 geolocation or cordova-geolocation plugin. But it can be used together with other geolocation providers (eg. html5 navigator.geolocation).

On Android you can choose from two location location providers:
* **ANDROID_DISTANCE_FILTER_PROVIDER** (forked from [cordova-plugin-background-geolocation](https://github.com/christocracy/cordova-plugin-background-geolocation))
* **ANDROID_ACTIVITY_PROVIDER**

See wiki [Which provider should I use?](https://github.com/mauron85/cordova-plugin-background-geolocation/blob/master/PROVIDERS.md) for more information about providers.

## Submitting issues

All new issues should follow instructions in [ISSUE_TEMPLATE.md](https://raw.githubusercontent.com/mauron85/cordova-plugin-background-geolocation/master/ISSUE_TEMPLATE.md). Properly filled issue report will significantly reduce number of follow up questions and decrease issue resolve time.

## Semantic Versioning
This plugin is following semantic versioning as defined http://semver.org

## Migration to 2.0

Warning: `option.url` for posting locations is very experimental and missing features like remote
server synchronization. Location database can get very big as currently there is no cleaning mechanism.
Use it at own risk. Proper server synchronization will be implemented in version 3.0.

As version 2.0 platform support for Windows Phone 8 was removed.
Some incompatible changes were introduced:

* option `stopOnTerminate` defaults to true
* option `locationService` renamed to `locationProvider`
* android providers are now **ANDROID_DISTANCE_FILTER_PROVIDER** and **ANDROID_ACTIVITY_PROVIDER**
* removed `locationTimeout` option (use `interval` in milliseconds instead)
* `notificationIcon` was replaced with two separate options (`notificationIconSmall` and `notificationIconLarge`)
* js object backgroundGeoLocation is deprecated use `backgroundGeolocation` instead
* iOS foreground mode witch automatic background mode switch
* iOS [switchMode][] allows to switch between foreground and background mode
* setPace on iOS is deprecated use switchMode instead


## Installing the plugin

```
cordova plugin add cordova-plugin-mauron85-background-geolocation
```

## Registering plugin for Adobe® PhoneGap™ Build

This plugin should work with Adobe® PhoneGap™ Build without any modification.
To register plugin add following line into your config.xml:

```
<gap:plugin name="cordova-plugin-mauron85-background-geolocation" source="npm"/>
```

NOTE: If you're using *hydration*, you have to download and reinstall your app with every new version of the plugin, as plugins are not updated.

## Compilation

### Android
You will need to ensure that you have installed the following items through the Android SDK Manager:

Name                       | Version
-------------------------- | -------
Android SDK Tools          | 24.4.1
Android SDK Platform-tools | 23.1
Android SDK Build-tools    | 23.0.1
Android Support Repository | 25
Android Support Library    | 23.1.1
Google Play Services       | 29
Google Repository          | 24

## Quick Example

```javascript
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady () {

    /**
    * This callback will be executed every time a geolocation is recorded in the background.
    */
    var callbackFn = function(location) {
        console.log('[js] BackgroundGeolocation callback:  ' + location.latitude + ',' + location.longitude);

        // Do your HTTP request here to POST location to your server.
        // jQuery.post(url, JSON.stringify(location));

        /*
        IMPORTANT:  You must execute the finish method here to inform the native plugin that you're finished,
        and the background-task may be completed.  You must do this regardless if your HTTP request is successful or not.
        IF YOU DON'T, ios will CRASH YOUR APP for spending too much time in the background.
        */
        backgroundGeolocation.finish();
    };

    var failureFn = function(error) {
        console.log('BackgroundGeolocation error');
    };

    // BackgroundGeolocation is highly configurable. See platform specific configuration options
    backgroundGeolocation.configure(callbackFn, failureFn, {
        desiredAccuracy: 10,
        stationaryRadius: 20,
        distanceFilter: 30,
        interval: 60000
    });

    // Turn ON the background-geolocation system.  The user will be tracked whenever they suspend the app.
    backgroundGeolocation.start();

    // If you wish to turn OFF background-tracking, call the #stop method.
    // backgroundGeolocation.stop();
}
```

## Example Application

Checkout repository [cordova-plugin-background-geolocation-example](https://github.com/mauron85/cordova-plugin-background-geolocation-example).

## API

### backgroundGeolocation.configure(success, fail, option)

Parameter | Type | Platform     | Description
--------- | ---- | ------------ | -----------
`success` | `Function` | all | Callback to be executed every time a geolocation is recorded in the background.
`fail` | `Function` | all | Callback to be executed every time a geolocation error occurs.
`option` | `JSON Object` | all |
`option.desiredAccuracy` | `Number` | all | Desired accuracy in meters. Possible values [0, 10, 100, 1000]. The lower the number, the more power devoted to GeoLocation resulting in higher accuracy readings.  1000 results in lowest power drain and least accurate readings. **@see** [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/desiredAccuracy)
`option.stationaryRadius` | `Number` | all | Stationary radius in meters. When stopped, the minimum distance the device must move beyond the stationary location for aggressive background-tracking to engage.
`option.debug` | `Boolean` | all | When enabled, the plugin will emit sounds for life-cycle events of background-geolocation! See debugging sounds table.
`option.distanceFilter` | `Number` | all | The minimum distance (measured in meters) a device must move horizontally before an update event is generated. **@see** [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/distanceFilter).
`option.stopOnTerminate` | `Boolean` | iOS, Android | Enable this in order to force a stop() when the application terminated (e.g. on iOS, double-tap home button, swipe away the app). (default true)
`option.startOnBoot` | `Boolean` | Android | Start background service on device boot. (default false)
`option.startForeground` | `Boolean` | Android | If false location service will not be started in foreground and no notification will be shown. (default true)
`option.interval` | `Number` | Android | The minimum time interval between location updates in milliseconds. **@see** [Android docs](http://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(long,%20float,%20android.location.Criteria,%20android.app.PendingIntent) for more information.
`option.notificationTitle` | `String` optional | Android | Custom notification title in the drawer.
`option.notificationText` | `String` optional | Android | Custom notification text in the drawer.
`option.notificationIconColor` | `String` optional| Android | The accent color to use for notification. Eg. **#4CAF50**.
`option.notificationIconLarge` | `String` optional | Android | The filename of a custom notification icon. See android quirks.
`option.notificationIconSmall` | `String` optional | Android | The filename of a custom notification icon. See android quirks.
`option.locationProvider` | `Number` | Android | Set location provider **@see** [wiki](https://github.com/mauron85/cordova-plugin-background-geolocation/wiki/Android-providers)
`option.activityType` | `String` | iOS | [AutomotiveNavigation, OtherNavigation, Fitness, Other] Presumably, this affects iOS GPS algorithm. **@see** [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/activityType) for more information
`option.url` | `String` | iOS, Android | Server url where to send HTTP POST with recorded locations
`option.httpHeaders` | `Object` | iOS, Android | Optional HTTP headers sent along in HTTP request
`option.saveBatteryOnBackground` | `Boolean` | iOS | If enabled it will automatically switch to less accurate significant changes and region monitory when in background (default)
`options.maxLocations` | `Number` | iOS | Limit maximum number of locations stored into db (default: 10000)

Following options are specific to provider as defined by locationProvider option
### ANDROID_ACTIVITY_PROVIDER provider options

Parameter | Type | Platform     | Description
--------- | ---- | ------------ | -----------
`option.interval` | `Number` | Android | Rate in milliseconds at which your app prefers to receive location updates. @see [android docs](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.html#getInterval())
`option.fastestInterval` | `Number` | Android | Fastest rate in milliseconds at which your app can handle location updates. **@see** [android  docs](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.html#getFastestInterval()).
`option.activitiesInterval` | `Number` | Android | Rate in milliseconds at which activity recognition occurs. Larger values will result in fewer activity detections while improving battery life.
`option.stopOnStillActivity` | `Boolean` | Android | stop() is forced, when the STILL activity is detected (default is true)

Success callback will be called with one argument - location object, which tries to mimic w3c [Coordinates interface](http://dev.w3.org/geo/api/spec-source.html#coordinates_interface).

Callback parameter | Type | Description
------------------ | ---- | -----------
`locationId` | `Number` | ID of location as stored in DB (or null)
`provider` | `String` | gps, network, passive or fused
`locationProvider` | `Number` | Location provider
`debug` | `Boolean` | true if location recorded as part of debug
`time` | `Number` |UTC time of this fix, in milliseconds since January 1, 1970.
`latitude` | `Number` | latitude, in degrees.
`longitude` | `Number` | longitude, in degrees.
`accuracy` | `Number` | estimated accuracy of this location, in meters.
`speed` | `Number` | speed if it is available, in meters/second over ground.
`altitude` | `Number` | altitude if available, in meters above the WGS 84 reference ellipsoid.
`bearing` | `Number` | bearing, in degrees.


### backgroundGeolocation.start()
Platform: iOS, Android

Start background geolocation.

### backgroundGeolocation.stop()
Platform: iOS, Android

Stop background geolocation.

### backgroundGeolocation.isLocationEnabled(success, fail)
Platform: iOS, Android

One time check for status of location services. In case of error, fail callback will be executed.

Success callback parameter | Type | Description
-------------------------- | ---- | -----------
`enabled` | `Boolean` | true/false (true when location services are enabled)

### backgroundGeolocation.showAppSettings()
Platform: iOS >= 8.0

Show app settings to allow change of app location permissions.

### backgroundGeolocation.showLocationSettings()
Platform: iOS, Android

Show system settings to allow configuration of current location sources.

### backgroundGeolocation.watchLocationMode(success, fail)
Platform: iOS, Android

Method can be used to detect user changes in location services settings.
If user enable or disable location services then success callback will be executed.
In case or error (SettingNotFoundException) fail callback will be executed.

Success callback parameter | Type | Description
-------------------------- | ---- | -----------
`enabled` | `Boolean` | true/false (true when location services are enabled)

### backgroundGeolocation.stopWatchingLocationMode()
Platform: iOS, Android

Stop watching for location mode changes.

### backgroundGeolocation.getLocations(success, fail)
Platform: iOS, Android

Method will return all stored locations.

Success callback parameter | Type | Description
-------------------------- | ---- | -----------
`locations` | `Array` | collection of stored locations

```javascript
backgroundGeolocation.getLocations(
  function (locations) {
    console.log(locations);
  }
);
```

### backgroundGeolocation.deleteLocation(locationId, success, fail)
Platform: iOS, Android

Delete location with locationId.

Note: Locations are not actually deleted from database to avoid gaps in locationId numbering.
Instead locations are marked as deleted. Locations marked as deleted will not appear in output of `backgroundGeolocation.getLocations`.

### backgroundGeolocation.deleteAllLocations(success, fail)
Deprecated warning: This method is **deprecated** and should not be used.
Plugin manages number of locations automatically and location count never exceeds number
as defined by `option.maxLocations`.

Platform: iOS, Android

Delete all stored locations.

### backgroundGeolocation.switchMode(modeId, success, fail)
Platform: iOS

Normally plugin will handle switching between **BACKGROUND** and **FOREGROUND** mode itself.
Calling switchMode you can override plugin behavior and force plugin to switch into other mode.

In **FOREGROUND** mode plugin uses iOS local manager to receive locations and behavior is affected
by `option.desiredAccuracy` and `option.distanceFilter`.

In **BACKGROUND** mode plugin uses significant changes and region monitoring to receive locations
and uses `option.stationaryRadius` only.

```
// switch to FOREGROUND mode
backgroundGeolocation.switchMode(backgroundGeolocation.mode.FOREGROUND);

// switch to BACKGROUND mode
backgroundGeolocation.switchMode(backgroundGeolocation.mode.BACKGROUND);
```

## Real world example

``` javascript
backgroundGeolocation.configure(callbackFn, failureFn, {
    desiredAccuracy: 10,
    stationaryRadius: 20,
    distanceFilter: 30,
    url: 'http://192.168.81.15:3000/locations',
    httpHeaders: { 'X-FOO': 'bar' },
    maxLocations: 1000,
    // Android only section
    locationProvider: backgroundGeolocation.provider.ANDROID_ACTIVITY_PROVIDER,
    interval: 60000,
    fastestInterval: 5000,
    activitiesInterval: 10000,
    notificationTitle: 'Background tracking',
    notificationText: 'enabled',
    notificationIconColor: '#FEDD1E',
    notificationIconLarge: 'mappointer_large',
    notificationIconSmall: 'mappointer_small'
});

backgroundGeolocation.watchLocationMode(
  function (enabled) {
    if (enabled) {
      // location service are now enabled
      // call backgroundGeolocation.start
      // only if user already has expressed intent to start service
    } else {
      // location service are now disabled or we don't have permission
      // time to change UI to reflect that
    }
  },
  function (error) {
    console.log('Error watching location mode. Error:' + error);
  }
);

backgroundGeolocation.isLocationEnabled(function (enabled) {
  if (enabled) {
    backgroundGeolocation.start(
      function () {
        // service started successfully
        // you should adjust your app UI for example change switch element to indicate
        // that service is running
      },
      function (error) {
        // Tracking has not started because of error
        // you should adjust your app UI for example change switch element to indicate
        // that service is not running
        if (error.code === 2) {
          if (window.confirm('Not authorized for location updates. Would you like to open app settings?')) {
            backgroundGeolocation.showAppSettings();
          }
        } else {
          window.alert('Start failed: ' + error.message);  
        }
      }
    );
  } else {
    // Location services are disabled
    if (window.confirm('Location is disabled. Would you like to open location settings?')) {
      backgroundGeolocation.showLocationSettings();
    }
  }
});
```

## HTTP locations posting

If `option.url` is not defined, all locations updates are recorded in local db. When App is in foreground or background in addition to storing location in local db, location callback function is triggered. Number of location stored in db is limited by `option.maxLocations` a never exceeds this number. Instead old locations are replaced by new ones.

When `option.url` is defined. Location updates are also stored in local db. In addition, each location is also immediately posted to url defined by `option.url`. If post is successful, the location is marked as deleted in local db. All failed to post locations will be coalesced and send in some time later in one single batch. [TO_BE_DEFINED]~~Batch sync takes place only as per interval defined in IntervalForPost config option.~~[/TO_BE_DEFINED].

Request body of posted locations is always array, even when only one location is sent.

### Example of express (nodejs) server
```javascript
var express    = require('express');
var bodyParser = require('body-parser');

var app = express();

// parse application/json
app.use(bodyParser.json({ type : '*/*' })); // force json

app.post('/locations', function(request, response){
    console.log('Headers:\n', request.headers);
    console.log('Body:\n', request.body);
    console.log('------------------------------');
    response.sendStatus(200);
});

app.listen(3000);
console.log('Server started...');
```

## Quirks

### iOS

On iOS the plugin will execute your configured ```callbackFn```. You may manually POST the received ```Geolocation``` to your server using standard XHR. The plugin uses iOS Significant Changes API, and starts triggering ```callbackFn``` only when a cell-tower switch is detected (i.e. the device exits stationary radius). The function ```changePace(isMoving, success, failure)``` is provided to force the plugin to enter "moving" or "stationary" state.

#### `stationaryRadius`

Since the plugin uses **iOS** significant-changes API, the plugin cannot detect the exact moment the device moves out of the stationary-radius.  In normal conditions, it can take as much as 3 city-blocks to 1/2 km before stationary-region exit is detected.

### Android

Android **WILL** execute your configured ```callbackFn```. This is the main difference from original christocracy plugin.

On Android devices it is required to have a notification in the drawer because it's a "foreground service".  This gives it high priority, decreasing probability of OS killing it. Check [wiki](https://github.com/mauron85/cordova-plugin-background-geolocation/wiki/Android-implementation) for explanation.

#### Custom ROMs

Plugin should work with custom ROMS at least ANDROID_DISTANCE_FILTER_PROVIDER. But ANDROID_ACTIVITY_PROVIDER provider depends on Google Play Services.
Usually ROMs don't include Google Play Services libraries. Strange bugs may occur, like no GPS locations (only from network and passive) and other. When posting issue report, please mention that you're using custom ROM.

#### Multidex
Note: Following section was kindly copied from [phonegap-plugin-push](https://github.com/phonegap/phonegap-plugin-push/blob/master/docs/INSTALLATION.md#multidex). Visit link for resolving issue with facebook plugin.

If you have an issue compiling the app and you're getting an error similar to this (`com.android.dex.DexException: Multiple dex files define`):

```
UNEXPECTED TOP-LEVEL EXCEPTION:
com.android.dex.DexException: Multiple dex files define Landroid/support/annotation/AnimRes;
	at com.android.dx.merge.DexMerger.readSortableTypes(DexMerger.java:596)
	at com.android.dx.merge.DexMerger.getSortedTypes(DexMerger.java:554)
	at com.android.dx.merge.DexMerger.mergeClassDefs(DexMerger.java:535)
	at com.android.dx.merge.DexMerger.mergeDexes(DexMerger.java:171)
	at com.android.dx.merge.DexMerger.merge(DexMerger.java:189)
	at com.android.dx.command.dexer.Main.mergeLibraryDexBuffers(Main.java:502)
	at com.android.dx.command.dexer.Main.runMonoDex(Main.java:334)
	at com.android.dx.command.dexer.Main.run(Main.java:277)
	at com.android.dx.command.dexer.Main.main(Main.java:245)
	at com.android.dx.command.Main.main(Main.java:106)
```

Then at least one other plugin you have installed is using an outdated way to declare dependencies such as `android-support` or `play-services-gcm`.
This causes gradle to fail, and you'll need to identify which plugin is causing it and request an update to the plugin author, so that it uses the proper way to declare dependencies for cordova.
See [this for the reference on the cordova plugin specification](https://cordova.apache.org/docs/en/5.4.0/plugin_ref/spec.html#link-18), it'll be usefull to mention it when creating an issue or requesting that plugin to be updated.

Common plugins to suffer from this outdated dependency management are plugins related to *facebook*, *google+*, *notifications*, *crosswalk* and *google maps*.

#### Android Permissions

Android 6.0 "Marshmallow" introduced a new permissions model where the user can turn on and off permissions as necessary. When user disallow location access permissions, error configure callback will be called with error code: 20.


#### Notification icons

**NOTE:** Only available for API Level >=21.

To use custom notification icons, you need to put icons into *res/drawable* directory **of your app**. You can automate the process  as part of **after_platform_add** hook configured via [config.xml](https://github.com/mauron85/cordova-plugin-background-geolocation-example/blob/master/config.xml). Check [config.xml](https://github.com/mauron85/cordova-plugin-background-geolocation-example/blob/master/config.xml) and [scripts/res_android.js](https://github.com/mauron85/cordova-plugin-background-geolocation-example/blob/master/scripts/res_android.js) of example app for reference.

With Adobe® PhoneGap™ Build icons must be placed into ```locales/android/drawable``` dir at the root of your project. For more information go to [how-to-add-native-image-with-phonegap-build](http://stackoverflow.com/questions/30802589/how-to-add-native-image-with-phonegap-build/33221780#33221780).

### Intel XDK

Plugin will not work in XDK emulator ('Unimplemented API Emulation: BackgroundGeolocation.start' in emulator). But will work on real device.

## Debugging

When `option.debug` is true, plugin logs all activity into database.
You can attach your device to the computer and print logs to console.

* For iOS open Safari and select from menu `Develop` ➜ `Your Device name`
* For Android launch Chrome `about:inspect`

Following snippet will print colored log of last 100 entries.

```javascript
function printLogs(logEntries) {
  function padLeft(nr, n, str) {
    return Array(n-String(nr).length+1).join(str||'0')+nr;
  }  
  var COLORS = [];
  COLORS[1] = 'background:white;color:red';
  COLORS[2] = 'background:black;color:yellow';
  COLORS[4] = 'background:white;color:blue';
  COLORS[8] = 'background:white;color:black';
  COLORS[16] = 'background:white;color:black';
  var logLines = [], logLinesColor = [];
  [].forEach.call(logEntries, function(logEntry) {
      var d = new Date(logEntry.timestamp * 1000);
      var dateFormatted = [
        [d.getFullYear(), padLeft(d.getMonth()+1,2), padLeft(d.getDate(),2)].join('/'),
        [padLeft(d.getHours(),2), padLeft(d.getMinutes(),2), padLeft(d.getSeconds(),2)].join(':')
      ].join(' ');
      logLines.push('%c[' + dateFormatted + '] %c' + logEntry.message);
      logLinesColor.push('background:white;color:black');
      logLinesColor.push(COLORS[logEntry.level]);
  });
  console.log.apply(console, [logLines.join('\n')].concat(logLinesColor));
}

backgroundGeolocation.getLogEntries(100, printLogs);
```

You can also post logs to your server:

``` javascript
function postLogs(logEntries) {
  function jQueryPost(logEntries) {
    logEntries = logEntries || [];
    console.log('Posting ' + logEntries.length + ' log entries...');
    jQuery.ajax({
      type: 'POST',
      url: 'http://192.168.81.15:3000/debug',
      data: JSON.stringify(logEntries),
      timeout: 5000
    })
    .done(function () {
      console.log('Logs has been successfully posted.');
    })
    .fail(function(xhr) {
      console.log('Posting of logs failed with status: ' + xhr.status);
    });
  }
  // will load jQuery if you not using it in your app
  if (!window.jQuery) {
    var script = document.createElement('script');    
    script.src = 'https://ajax.googleapis.com/ajax/libs/jquery/3.0.0/jquery.min.js';
    script.onload = function () { jQueryPost(logEntries); };
    document.getElementsByTagName('head')[0].appendChild(script);
  } else {
    jQueryPost(logEntries);
  }
}

backgroundGeolocation.getLogEntries(100, postLogs);
```

### Debugging sounds
|    | *ios* | *android* |
| ------------- | ------------- | ------------- |
| Exit stationary region  | Calendar event notification sound  | dialtone beep-beep-beep  |
| Geolocation recorded  | SMS sent sound  | tt short beep |
| Aggressive geolocation engaged | SIRI listening sound |  |
| Passive geolocation engaged | SIRI stop listening sound |  |
| Acquiring stationary location sound | "tick,tick,tick" sound |  |
| Stationary location acquired sound | "bloom" sound | long tt beep |

**NOTE:** For iOS  in addition, you must manually enable the *Audio and Airplay* background mode in *Background Capabilities* to hear these debugging sounds.

## Geofencing
There is nice cordova plugin [cordova-plugin-geofence](https://github.com/cowbell/cordova-plugin-geofence), which does exactly that. Let's keep this plugin lightweight as much as possible.

## Changelog

See [CHANGES.md](/CHANGES.md)

## Licence ##

[Apache License](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2013 Christopher Scott, Transistor Software

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
