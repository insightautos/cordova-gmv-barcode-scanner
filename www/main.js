// JavaScript Document
CDV = (typeof CDV == 'undefined' ? {} : CDV);
var cordova = window.cordova || window.Cordova;

(function () {
    function MLKitBarcodeScanner() { }
    MLKitBarcodeScanner.prototype.scan = function (params, success, failure) {
        // Default settings. Scan every barcode type.
        var settings = {
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
        };
        for (var key in params) {
            if (params.hasOwnProperty(key) && settings.hasOwnProperty(key)) {
                settings[key] = params[key];
            }
        }
        var detectorTypes = 0;
        // GMVDetectorConstants values allow us to pass an integer sum of all the desired barcode types to the scanner.
        var detectionTypes = {
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
        };
        for (var key in settings.types) {
            if (detectionTypes.hasOwnProperty(key) && settings.types.hasOwnProperty(key) && settings.types[key] == true) {
                detectorTypes += detectionTypes[key];
            }
        }
        // Order of this settings object is critical. It will be passed in a basic array format and must be in the order shown.
        var stngs = {
            //Position 1
            detectorType: detectorTypes,
            //Position 2
            detectorWidth: settings.detectorSize.width,
            //Position 3
            detectorHeight: settings.detectorSize.height
        };
        var sendSettings = [];
        for (var key in stngs) {
            if (stngs.hasOwnProperty(key)) {
                sendSettings.push(stngs[key]);
            }
        }
        this.sendScanRequest(sendSettings, success, failure);
    };
    MLKitBarcodeScanner.prototype.sendScanRequest = function (settings, success, failure) {
        cordova.exec(function (data) {
            success(data[0]);
        },
            function (err) {
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
