declare namespace CordovaPluginMLKitBarcodeScanner {
  interface BarcodeTypes {
    Aztec: boolean;
    CodaBar: boolean;
    Code39: boolean;
    Code93: boolean;
    Code128: boolean;
    DataMatrix: boolean;
    EAN8: boolean;
    EAN13: boolean;
    ITF: boolean;
    PDF417: boolean;
    QRCode: boolean;
    UPCA: boolean;
    UPCE: boolean;
  }

  interface Options {
    types?: BarcodeTypes;
    detectorSize?: number;
    mirrorCamera?: boolean;
  }

  interface Result {
    text: string;
    format: string | undefined;
    type: string | undefined;
  }

  interface Error {
    cancelled: boolean;
    message: string;
  }
}

interface CordovaPlugins {
  mlkit: {
    barcodeScanner: {
      scan(
        options: CordovaPluginMLKitBarcodeScanner.Options | undefined,
        successCallback: (result: CordovaPluginMLKitBarcodeScanner.Result) => any,
        callback: (error: CordovaPluginMLKitBarcodeScanner.Error) => any
      ): void;
    };
  };
}
