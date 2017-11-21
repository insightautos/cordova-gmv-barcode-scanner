
cordova-gmv-barcode-scanner
===========================

Purpose of this Project
-----------------------

The purpose of this project is to provide a barcode scanner utilizing the Google Mobile Vision library for the Cordova framework on iOS and Android. The GMV library is incredibly performant and fast in comparison to any other barcode reader that I have used that are free. Additionally, I built it to perform live validity checks on VIN numbers for use as a VIN scanner. 

Installation
------------

To install, simply run the `cordova plugin add ` command on this git repository.

###Android Quirks

If you are using this plugin for the Android platform, it is important that you place the following code inside the `config.xml` file in the root of your Cordova project. This is a preference that will be utilized by the [`cordova-custom-config`](https://github.com/dpa99c/cordova-custom-config) library so that the theming is provided to Android. If you do not do this then the plugin will crash the application.

````xml
<preference name="android-manifest/application/@android:theme" value="@style/Theme.AppCompat" />
````

Usage
-----

To use the plugin simply call `CDV.scanner.scan(options, callback)`. See the sample below.

````javascript
CDV.scanner.scan({vinDetector: true}, function(err, data) {
	if(err) {
			return;
		}
		alert(JSON.stringify(data));
	});
````

###Plugin Options

The default options are shown below. Note that the `detectorSize.width` and `detectorSize.height` values must be floats. If the values are greater than 1 then they will not be visible on the screen. Use them as decimal percentages to determine how large you want the scan area to be.
````javascript
var options = {
	types: {
		Code128: true,
		Code39: true,
		Code93: true,
		CodaBar: true,
		DataMatrix: true,
		EAN13: true,
		EAN8: true,
		ITF: true,
		QRCode: true,
		UPCA: true,
		UPCE: true,
		PDF417: true,
		Aztec: true
	},
	detectorSize: {
		width: .5,
		height: .7
	},
	vinDetector: false
}
````


###Android Quirks

The `detectorSize` does not currently exclude the area around the detector from being scanned, which means that anything shown on the preview screen is up for grabs to the barcode detector. On iOS this is done automatically.