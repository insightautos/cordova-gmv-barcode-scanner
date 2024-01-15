import { MLKitBarcodeScanner } from './BarcodeScanner.plugin';

declare global {
  // eslint-disable-next-line @typescript-eslint/naming-convention
  interface CordovaPlugins {
    mlkit: {
      barcodeScanner: MLKitBarcodeScanner;
    };
  }
}

export { MLKitBarcodeScanner } from './BarcodeScanner.plugin';
export { IBarcodeFormats, IError, IOptions, IResult } from './Interface';
