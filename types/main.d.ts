declare namespace CordovaBarcodeDetector {
  interface BarcodeTypes {
    Aztec: boolean,
    CodaBar: boolean,
    Code39: boolean,
    Code93: boolean,
    Code128: boolean,
    DataMatrix: boolean,
    EAN8: boolean,
    EAN13: boolean,
    ITF: boolean,
    PDF417: boolean,
    QRCode: boolean,
    UPCA: boolean,
    UPCE: boolean
  }

  interface Options {
    types?: BarcodeTypes;
    detectorSize?: {
      width: number;
      height: number;
    }
  }
}

interface CordovaPlugins {
  GMVBarcodeScanner: {
    scan(options: CordovaBarcodeDetector.Options | undefined, callback: ({ (error: Error & { cancelled?: boolean }, result: string): any })): void;
  };
}
