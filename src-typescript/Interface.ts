export interface IBarcodeFormats {
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

export interface IOptions {
  barcodeFormats?: IBarcodeFormats;
  detectorSize?: number;
  rotateCamera?: boolean;
}

export interface IConfig {
  barcodeFormats: number;
  detectorSize: number;
  rotateCamera?: boolean;
}

export interface IResult {
  text: string;
  format: string;
  type: string;
}

export interface IError {
  cancelled: boolean;
  message: string;
}
