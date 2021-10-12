import { detectorFormat, detectorType } from './Detector';
import { IError, IOptions, IResult, ISettings } from './Interface';
import { defaultOptions } from './Options';

function getValues<TObject>(obj: { [s: string]: TObject }): TObject[] {
  return Object.keys(obj).map((key) => obj[key] as TObject);
}

export class MLKitBarcodeScanner {
  private options: IOptions = {
    ...defaultOptions,
  };

  private getBarcodeFormat(format: number): string {
    getValues(detectorFormat).indexOf(format);
    const index = getValues(detectorFormat).indexOf(format);
    return Object.keys(detectorFormat)[index] || format.toString();
  }

  private getBarcodeType(type: number): string {
    getValues(detectorType).indexOf(type);
    const index = getValues(detectorType).indexOf(type);
    return Object.keys(detectorType)[index] || type.toString();
  }

  private getDetectorTypes(): number {
    let detectorTypes = 0;
    let key: keyof typeof detectorFormat;
    // eslint-disable-next-line no-restricted-syntax
    for (key in this.options.types) {
      if (
        detectorFormat.hasOwnProperty(key) &&
        this.options.types.hasOwnProperty(key) &&
        this.options.types[key] === true
      ) {
        detectorTypes += detectorFormat[key];
      }
    }
    return detectorTypes;
  }

  private getSettings(): ISettings {
    // Order of this settings object is critical. It will be passed in a basic array format and must be in the order shown.
    const args = {
      // Position 1
      detectorTypes: this.getDetectorTypes(),
      // Position 2
      detectorSize: this.options.detectorSize,
      // Position 3 (Android only)
      mirrorCamera: this.options.mirrorCamera,
    };
    const settings: ISettings = [];

    for (const value of getValues(args)) {
      settings.push(value);
    }

    return settings;
  }

  scan(
    userOptions: IOptions,
    success: (result: IResult) => unknown,
    failure: (error: IError) => unknown,
  ): void {
    this.options = {
      ...this.options,
      ...userOptions,
    };
    this.sendScanRequest(this.getSettings(), success, failure);
  }

  private sendScanRequest(
    settings: ISettings,
    successCallback: (result: IResult) => unknown,
    failureCallback: (error: IError) => unknown,
  ): void {
    cordova.exec(
      (data: [string, number, number]) => {
        const [text, format, type] = data;
        successCallback({
          text,
          format: this.getBarcodeFormat(format),
          type: this.getBarcodeType(type),
        });
      },
      (err: (string | null)[]) => {
        switch (err[0]) {
          case null:
          case 'USER_CANCELLED':
            failureCallback({
              cancelled: true,
              message: 'The scan was cancelled.',
            });
            break;
          case 'SCANNER_OPEN':
            failureCallback({
              cancelled: false,
              message: 'Scanner already open.',
            });
            break;
          default:
            failureCallback({
              cancelled: false,
              message: err[0] || 'Unknown Error',
            });
            break;
        }
      },
      'cordova-plugin-mlkit-barcode-scanner',
      'startScan',
      settings,
    );
  }
}

const barcodeScanner = new MLKitBarcodeScanner();
module.exports = barcodeScanner;
