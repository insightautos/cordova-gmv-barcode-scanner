# :camera: cordova-plugin-mlkit-barcode-scanner

## Purpose of this Project

The purpose of this project is to provide a barcode scanner utilizing the Google ML Kit Vision library for the Cordova framework on iOS and Android.
The MLKit library is incredibly performant and fast in comparison to any other barcode reader that I have used that are free.

## Plugin Dependencies

| Dependency                        | Version   | Info                       |
| --------------------------------- | --------- | -------------------------- |
| `cordova-android`                 | `>=8.0.0` |                            |
| `cordova-ios`                     | `>=4.5.0` |                            |
| `cordova-plugin-androidx`         | ` ^3.0.0` | If cordova-android < 9.0.0 |
| `cordova-plugin-androidx-adapter` | ` ^1.1.3` |                            |

## Prerequisites

If your `cordova-android` version is below `9.0.0`, you have to install `cordova-plugin-androidx` first before installing this plugin.
Execute this command in your terminal:

```bash
npx cordova plugin add cordova-plugin-androidx
```

## Installation

Run this command in your project root:

```bash
npx cordova plugin add cordova-plugin-mlkit-barcode-scanner
```

## Supported Platforms

- Android
- iOS/iPadOS

## Barcode Support

| 1d formats   | Android | iOS |
| ------------ | ------- | --- |
| Codabar      | ✓       | ✓   |
| Code 39      | ✓       | ✓   |
| Code 93      | ✓       | ✓   |
| Code 128     | ✓       | ✓   |
| EAN-8.       | ✓       | ✓   |
| EAN-13       | ✓       | ✓   |
| ITF          | ✓       | ✓   |
| MSI          | ✗       | ✗   |
| RSS Expanded | ✗       | ✗   |
| RSS-14       | ✗       | ✗   |
| UPC-A        | ✓       | ✓   |
| UPC-E        | ✓       | ✓   |

| 2d formats  | Android | iOS |
| ----------- | ------- | --- |
| Aztec       | ✓       | ✓   |
| Codablock   | ✗       | ✗   |
| Data Matrix | ✓       | ✓   |
| MaxiCode    | ✗       | ✗   |
| PDF417      | ✓       | ✓   |
| QR Code     | ✓       | ✓   |

:information_source: Note that this API does not recognize barcodes in these forms:

- 1D Barcodes with only one character
- Barcodes in ITF format with fewer than six characters
- Barcodes encoded with FNC2, FNC3 or FNC4
- QR codes generated in the ECI mode

## Usage

To use the plugin simply call `cordova.plugins.mlkit.barcodeScanner.scan(options, sucessCallback, failureCallback)`. See the sample below.

```javascript
cordova.plugins.mlkit.barcodeScanner.scan(
  options,
  (result) => {
    // Do something with the data
    alert(result);
  },
  (error) => {
    // Error handling
  },
);
```

### Plugin Options

The default options are shown below.
All values are optional.

Note that the `detectorSize` value must be between `0` and `1`, because it determines how many percent of the screen should be covered by the detector.
If the value is greater than 1 the detector will not be visible on the screen.

```javascript
const defaultOptions = {
  barcodeFormats: {
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
    Aztec: true,
  },
  beepOnSuccess: false,
  vibrateOnSuccess: false,
  detectorSize: 0.6,
  rotateCamera: false,
};
```

### Output/Return value

```javascript
result: {
  text: string;
  format: string;
  type: string;
}
```

## Known Issues

On some devices the camera may be upside down.

Here is a list of devices with this problem:

- Zebra MC330K (Manufacturer: Zebra Technologies, Model: MC33)

Current Solution:
if your device has this problem, you can call the plugin with the option `rotateCamera` set to `true`.
This will rotate the camera stream by 180 degrees.

## Development

### Build Process

This project uses npm scripts for building:

```shell
# lint the project using eslint
npm run lint

# removes the generated folders
npm run clean

# build the project
# (includes clean and lint)
npm run build

# publish the project
# (includes build)
npm publish
```

A VS Code task for `build` is also included.

## Run the test app

Install cordova:

```
npm i -g cordova
```

Go to test app:

```
cd test/scan-test-app
```

Install node modules:

```
npm i
```

Prepare Cordova:

```
cordova prepare && cordova plugin add ../../ --link --force
```

Build and run the project Android:

```
cordova build android && cordova run android
```

and iOS:

```
cordova build ios && cordova run ios
```

### Versioning

⚠️ Before incrementing the version in `package.json`, remember to increment the version in `plugin.xml` by hand.

### VS Code Extensions

This project is intended to be used with Visual Studio Code and the recommended extensions can be found in [`.vscode/extensions.json`](.vscode/extensions.json).
When you open this repository for the first time in Visual Studio Code you should get a prompt asking you to install the recommended extensions.
