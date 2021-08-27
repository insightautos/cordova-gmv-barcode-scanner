import { detectorFormat, detectorType } from './detector';
import { Error, Options, Result, Settings } from './interface';
import { defaultOptions } from './options';

class MLKitBarcodeScanner {
  private options: Options = {
    ...defaultOptions
  };

  private getBarcodeFormat(format: number) {
    const formatString = Object.values(detectorFormat)
      .find((value: number) => value === format);
    return formatString || format;
  }

  private getBarcodeType(type: number) {
    const typeString = Object.values(detectorType)
      .find((value: number) => value === type);
    return typeString || type;
  }

  private getDetectorTypes() {
    let detectorTypes = 0;
    let key: keyof typeof detectorFormat;
    for (key in this.options.types) {
      if (detectorFormat.hasOwnProperty(key) && this.options.types.hasOwnProperty(key) && this.options.types[key] === true) {
        detectorTypes += detectorFormat[key];
      }
    }
    return detectorTypes;
  }

  private getSettings() {
    // Order of this settings object is critical. It will be passed in a basic array format and must be in the order shown.
    const args = {
      // Position 1
      detectorTypes: this.getDetectorTypes(),
      // Position 2
      detectorSize: this.options.detectorSize,
      // Position 3 (Android only)
      mirrorCamera: this.options.mirrorCamera
    };
    const settings: Settings = [];
    for (const [key, value] of Object.entries(args)) {
      if (args.hasOwnProperty(key)) {
        settings.push(value);
      }
    }
    return settings;
  }

  scan(userOptions: Options, success: (result: Result) => any, failure: (error: Error) => any) {
    this.options = {
      ...this.options,
      ...userOptions
    };
    this.sendScanRequest(this.getSettings(), success, failure);
  }

  private sendScanRequest(settings: Settings, successCallback: (result: Result) => unknown, failureCallback: (error: Error) => unknown) {
    cordova.exec((data) => {
      const [text, format, type] = data;
      successCallback({
        text,
        format: this.getBarcodeFormat(format),
        type: this.getBarcodeType(type)
      });
    }, (err) => {
      switch (err[0]) {
        case null:
        case 'USER_CANCELLED':
          failureCallback({ cancelled: true, message: 'The scan was cancelled.' });
          break;
        case 'SCANNER_OPEN':
          failureCallback({ cancelled: false, message: 'Scanner already open.' });
          break;
        default:
          failureCallback({ cancelled: false, message: err });
          break;
      }
    }, 'cordova-plugin-mlkit-barcode-scanner', 'startScan', settings);
  }

}

export default new MLKitBarcodeScanner();
