const cordova = window.cordova || window.Cordova;

const defaultSettings = Object.freeze({
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
});
const detectorFormat = Object.freeze({
  Code128: 1,
  Code39: 2,
  Code93: 4,
  CodaBar: 8,
  DataMatrix: 16,
  EAN13: 32,
  EAN8: 64,
  ITF: 128,
  QRCode: 256,
  UPCA: 512,
  UPCE: 1024,
  PDF417: 2048,
  Aztec: 4096
});

const detectorType = Object.freeze({
  CONTACT_INFO: 1,
  EMAIL: 2,
  ISBN: 3,
  PHONE: 4,
  PRODUCT: 5,
  SMS: 6,
  TEXT: 7,
  URL: 8,
  WIFI: 9,
  GEO: 10,
  CALENDAR_EVENT: 11,
  DRIVER_LICENSE: 12
});

function getBarcodeFormat(format) {
  const formatString = Object.keys(detectorFormat).find(key => detectorFormat[key] === format);
  return formatString || format;
}

function getBarcodeType(type) {
  const typeString = Object.keys(detectorType).find(key => detectorType[key] === type);
  return typeString || type;
}

(function () {
  function MLKitBarcodeScanner() { }

  MLKitBarcodeScanner.prototype.scan = function (params, success, failure) {
    // Default settings. Scan every barcode type.
    const settings = Object.assign({}, defaultSettings, params);

    // GMVDetectorConstants values allow us to pass an integer sum of all the desired barcode types to the scanner.
    let detectorTypes = 0;
    for (const key in settings.types) {
      if (detectorFormat.hasOwnProperty(key) && settings.types.hasOwnProperty(key) && settings.types[key] == true) {
        detectorTypes += detectorFormat[key];
      }
    }
    // Order of this settings object is critical. It will be passed in a basic array format and must be in the order shown.
    const args = {
      //Position 1
      detectorType: detectorTypes,
      //Position 2
      detectorWidth: settings.detectorSize.width,
      //Position 3
      detectorHeight: settings.detectorSize.height
    };
    const sendSettings = [];
    for (const key in args) {
      if (args.hasOwnProperty(key)) {
        sendSettings.push(args[key]);
      }
    }
    this.sendScanRequest(sendSettings, success, failure);
  };

  MLKitBarcodeScanner.prototype.sendScanRequest = function (settings, success, failure) {
    cordova.exec((data) => {
      success({
        cancelled: false,
        text: data[0],
        format: getBarcodeFormat(data[1]),
        type: getBarcodeType(data[2])
      });
    }, (err) => {
      switch (err[0]) {
        case null:
        case "USER_CANCELLED":
          failure({ cancelled: true, message: "The scan was cancelled." });
          break;
        case "SCANNER_OPEN":
          failure({ cancelled: false, message: "Scanner already open." });
          break;
        default:
          failure({ cancelled: false, message: err });
          break;
      }
    }, 'cordova-plugin-mlkit-barcode-scanner', 'startScan', settings);
  };

  module.exports = new MLKitBarcodeScanner();
})();
