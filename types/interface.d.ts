export declare type Settings = (string | number | boolean | undefined)[];
export interface BarcodeTypes {
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
export interface Options {
    types: BarcodeTypes;
    detectorSize: number;
    mirrorCamera?: boolean;
}
export interface Result {
    text: string;
    format: number;
    type: number;
}
export interface Error {
    cancelled: boolean;
    message: string;
}
