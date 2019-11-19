
cordova-plugin-barcode-detector
===========================

Purpose of this Project
-----------------------

The purpose of this project is to provide a barcode scanner utilizing the Google Mobile Vision library for the Cordova framework on iOS and Android. The GMV library is incredibly performant and fast in comparison to any other barcode reader that I have used that are free. Additionally, I built it to perform live validity checks on VIN numbers for use as a VIN scanner and for drivers license scanning through the PDF 417 barcode on most identification cards.

![iPhone X Screenshot](https://github.com/dealrinc/cordova-plugin-barcode-detector/raw/master/screenshots/iphone-x-screenshot.jpg "iPhone X Screenshot")

You can also check out a sample application [here](https://github.com/dealrinc/cordova-plugin-barcode-detector-sampleapp) if you'd like to see the scanner in action.

Installation
------------

````
cordova plugin add cordova-plugin-barcode-detector
````

Usage
-----

To use the plugin simply call `window.plugins.GMVBarcodeScanner.scan(options, callback)`. See the sample below.

````javascript
window.plugins.GMVBarcodeScanner.scan({}, function(err, result) {

	//Handle Errors
	if(err) return;

	//Do something with the data.
	alert(result);

});
````

You can also call `scanLicense` or `scanVIN` to use the other scanning abilities. Note that the only options available to these functions are `width` and `height` of the barcode detector.


````javascript
window.plugins.GMVBarcodeScanner.scanVIN(function(err, result) {
	//Handle Errors
	if(err) return;

	//Do something with the data.
	alert(result);

}, { width: .5, height: .7 });
````

````javascript
window.plugins.GMVBarcodeScanner.scanLicense(function(err, result) {
	//Handle Errors
	if(err) return;

	//Do something with the data.
	alert(result);

}, { width: .5, height: .7 });
````


### Output
For the `scan` and `scanVIN` functions the output will be a plain string of the value scanned. For `scanLicense` the result will be an object something along the lines of

```` json
{
    "LicenseNumber": "123456789",
    "FirstName": "Johnny",
    "MiddleName": "Allen",
    "LastName": "Appleseed",
    "BirthDate": "1/31/1990",
    "LicenseExpiration": "1/31/2025",
    "Address": {
        "Address": "1234 Main St.",
        "City": "Fairyland",
        "State": "AB",
        "Zip": "12345"
    },
    "LicenseState":"AB"
}

````

### Plugin Options

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
	}
}
````


### Android Quirks

The `detectorSize` option does not currently exclude the area around the detector from being scanned, which means that anything shown on the preview screen is up for grabs to the barcode detector. On iOS this is done automatically.

### VIN Scanning

VIN scanning works on both iOS and Android and utilizes both Code39 and Data Matrix formats. The scanner has a VIN checksum validator that ensures that the 9th VIN digit is correctly calculated. If it is not, the barcode will simply be skipped and the scanner will continue until it finds a valid VIN.

### Driver's License Scanning

Driver's license scanning works on both iOS and Android and scans the PDF417 format and decodes according to the AAMVA specification. It only pulls a few fields, but I believe they are the most important. The decoding is done in the Javascript portion of this plugin which means you could modify it if you'd like.

### Commercial Use
This VIN scanner is the primary reason I built out this project, and is used in a commercial application for my company. Additionally, PDF417 scanning on drivers licenses is a massive benefit to the speed of the GMV library. I'd ask that any competitors don't utilize the VIN scanner for vehicles or PDF417 scanner for drivers licenses in applications that offer similar service to the [dealr.cloud](http://dealr.cloud) application.

Maybe it's stupid for me to ask this, but I wanted to make this project MIT and open because I have always had trouble finding a good scanner for cordova and I wanted to help out other developers. Figured a bit of an ask is in order! :-)

Project Info
------------

I am not a native developer and basically hacked both of the implementations together. That being said, in testing the plugins look fantastic, significantly more modern than other scanners, and they scan incredibly quickly. Please send @forrestmid a private message, or just submit a pull request, if you have any inclination towards assisting the development of this project!
