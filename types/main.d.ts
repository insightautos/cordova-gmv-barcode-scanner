import { Error, Options, Result } from './interface';
declare class MLKitBarcodeScanner {
    private options;
    private getBarcodeFormat;
    private getBarcodeType;
    private getDetectorTypes;
    private getSettings;
    scan(userOptions: Options, success: (result: Result) => any, failure: (error: Error) => any): void;
    private sendScanRequest;
}
declare const _default: MLKitBarcodeScanner;
export default _default;
