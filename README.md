# :camera: cordova-plugin-mlkit-barcode-scanner

## Purpose of this Project

The purpose of this project is to provide a barcode scanner utilizing the Google ML Kit Vision library for the Cordova framework on iOS and Android. The MLKit library is incredibly performant and fast in comparison to any other barcode reader that I have used that are free.

## Plugin Dependencies

Dependency | Version | Info
---------- | ------- | --------
`cordova-android` | `>=8.0.0`
`cordova-ios` | `>=4.5.0`
`cordova-plugin-androidx` | ` ^3.0.0` | If cordova-android <= 9.0.0
`cordova-plugin-androidx-adapter` | ` ^1.1.3`

## Prerequisites

If your cordova-android version is below 9.0.0, you have to install `cordova-plugin-androidx` first beforce installing this plugin. Execute this command in your terminal:
```bash
cordova plugin add cordova-plugin-androidx
```
## Installation

Run this command in your project root.
```bash
cordova plugin add cordova-plugin-mlkit-barcode-scanner
```

## Supported Platforms

- Android
- iOS/iPadOS

## Barcode Support

1d formats:
- :+1: Codabar
- :+1: Code 39
- :+1: Code 93
- :+1: Code 128
- :+1: EAN-8
- :+1: EAN-13
- :+1: ITF
- :+1: UPC-A
- :+1: UPC-E

2D formats:
- :+1: Aztec
- :+1: Data Matrix
- :+1: PDF417
- :+1: QR Code

:information_source: Note that this API does not recognize barcodes in these forms:
- 1D Barcodes with only one character
- Barcodes in ITF format with fewer than six characters
- Barcodes encoded with FNC2, FNC3 or FNC4
- QR codes generated in the ECI mode

## Usage

To use the plugin simply call `cordova.plugins.mlkit.barcodeScanner.scan(options, sucessCallback, failureCallback)`. See the sample below.

```javascript
cordova.plugins.mlkit.barcodeScanner.scan(options, (error, result) => {
  if (error) {
    // Error handling
  }

  // Do something with the data
  alert(result);
});
```

### Plugin Options

The default options are shown below. Note that the `detectorSize` value must be a float. If the values are greater than 1 then they will not be visible on the screen. Use them as decimal percentages to determine how large you want the scan area to be. All values are optional.

```javascript
const defaultOptions = {
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
  detectorSize: 0.6
}
```

### Output/Return value

```javascript
result: {
  cancelled: boolean;
  text: string;
  format: string | undefined;
  type: string | undefined;
}
```

## Run the test app

Install cordova
```
npm i -g cordova
```

Go to test app
```
cd test/scan-test-app
```

Install node modules
```
npm i
```

Prepare Cordova
```
cordova platform add android && cordova plugin add ../../ --link --force
```

Build and run the project Android
```
cordova build android && cordova run android
```
and iOS
```
cordova build ios && cordova run ios
```

## To-dos <small>(2021-May)</small>

- [X] Android: Migrate from deprecatd Camera API to CameraX
- [X] Android: Get Viewfinder (detectorSize) to work
- [ ] Android: Test functionality with Android 5.1 (API 22)
