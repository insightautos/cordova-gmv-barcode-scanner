
cordova-plugin-mlkit-barcode-scanner
===========================

Purpose of this Project
-----------------------

The purpose of this project is to provide a barcode scanner utilizing the Google MLKit Vision library for the Cordova framework on iOS and Android. The MLKit library is incredibly performant and fast in comparison to any other barcode reader that I have used that are free. Additionally, I built it to perform live validity checks on VIN numbers for use as a VIN scanner and for drivers license scanning through the PDF 417 barcode on most identification cards.

Installation
------------

````
cordova plugin add cordova-plugin-mlkit-barcode-scanner
````

Usage
-----

To use the plugin simply call `cordova.plugins.mlkit.barcodeScanner.scan(options, sucessCallback, failureCallback)`. See the sample below.

````javascript
cordova.plugins.mlkit.barcodeScanner.scan({}, function(err, result) {

	//Handle Errors
	if(err) return;

	//Do something with the data.
	alert(result);

});
````

### Output
For the `scan` and `scanVIN` functions the output will be a plain string of the value scanned.

### Plugin Options

The default options are shown below. Note that the `detectorSize.width` and `detectorSize.height` values must be floats. If the values are greater than 1 then they will not be visible on the screen. Use them as decimal percentages to determine how large you want the scan area to be.
````javascript
const options = {
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
	}
}
````


### Android Quirks

The `detectorSize` option does not currently exclude the area around the detector from being scanned, which means that anything shown on the preview screen is up for grabs to the barcode detector. On iOS this is done automatically.
