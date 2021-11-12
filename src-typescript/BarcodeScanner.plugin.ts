import { barcodeFormat, barcodeType } from './Detector';
import {
  IBarcodeFormats,
  IConfig,
  IError,
  IOptions,
  IResult,
} from './Interface';
import { getKeyByValue } from './ObjectUtils';
import { defaultOptions } from './Options';

export class MLKitBarcodeScanner {
  private getBarcodeFormat(format: number): string {
    return getKeyByValue(barcodeFormat, format);
  }

  private getBarcodeType(type: number): string {
    return getKeyByValue(barcodeType, type);
  }

  private getBarcodeFormatFlags(barcodeFormats?: IBarcodeFormats): number {
    let barcodeFormatFlag = 0;
    let key: keyof typeof barcodeFormat;
    const formats = barcodeFormats || defaultOptions.barcodeFormats;

    // eslint-disable-next-line no-restricted-syntax
    for (key in formats) {
      if (
        barcodeFormat.hasOwnProperty(key) &&
        formats &&
        formats.hasOwnProperty(key) &&
        formats[key] === true
      ) {
        barcodeFormatFlag += barcodeFormat[key];
      }
    }
    return barcodeFormatFlag;
  }

  scan(
    userOptions: IOptions,
    success: (result: IResult) => unknown,
    failure: (error: IError) => unknown,
  ): void {
    const config: IConfig = {
      ...defaultOptions,
      ...userOptions,
      barcodeFormats: this.getBarcodeFormatFlags(userOptions.barcodeFormats),
    };
    this.sendScanRequest(config, success, failure);
  }

  private sendScanRequest(
    config: IConfig,
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
      [config],
    );
  }
}

const barcodeScanner = new MLKitBarcodeScanner();
module.exports = barcodeScanner;
