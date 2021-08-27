/*
 Copyright 2016-present Google Inc. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

@import AVFoundation;
@import Vision;
@import MLKit;

#import "CameraViewController.h"
#import <MetalKit/MetalKit.h>


@interface CameraViewController ()<AVCaptureVideoDataOutputSampleBufferDelegate>

@property(nonatomic, weak) IBOutlet UIView *placeHolderView;
@property(nonatomic, weak) IBOutlet UIView *overlayView;
@property(nonatomic, strong) UIImageView *imageView;

@property(nonatomic, strong) AVCaptureSession *session;
@property(nonatomic, strong) AVCaptureVideoDataOutput *videoDataOutput;
@property(nonatomic, strong) dispatch_queue_t videoDataOutputQueue;
@property(nonatomic, strong) AVCaptureVideoPreviewLayer *previewLayer;

@property(nonatomic, strong) MLKBarcodeScanner *mlkBarcodeDetector;
@property(nonatomic, strong) MLKTextRecognizer *mlkTextDetector;

@property(nonatomic, strong) UIButton *torchButton;



@end

@implementation CameraViewController
@synthesize delegate;

- (BOOL)prefersStatusBarHidden
{
    return YES;
}

- (BOOL)prefersHomeIndicatorAutoHidden
{
    return YES;
}

-(BOOL) shouldAutorotate
{
    return NO;
}
static CIContext *gCIContext;

-(NSUInteger)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskPortrait;
}

- (id)initWithCoder:(NSCoder *)aDecoder {
    self = [super initWithCoder:aDecoder];
    if (self) {
        _videoDataOutputQueue = dispatch_queue_create("VideoDataOutputQueue",
                                                      DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];

    // Set up camera.
    self.session = [[AVCaptureSession alloc] init];
    self.session.sessionPreset = AVCaptureSessionPresetHigh;

    _videoDataOutputQueue = dispatch_queue_create("VideoDataOutputQueue",
                                                  DISPATCH_QUEUE_SERIAL);

    [self updateCameraSelection];

    // Set up video processing pipeline.
    [self setUpVideoProcessing];

    // Set up camera preview.
    [self setUpCameraPreview];


    //Parse Cordova settings.
    MLKBarcodeFormat *formats = 0;
    //If barcodeFormats == 0 then process as a VIN with VIN verifications.
    if([_barcodeFormats  isEqual: @0]) {
        NSLog(@"Running VIN style");
        //formats = @(MLKBarcodeFormatCode39|MLKBarcodeFormatDataMatrix|MLKBarcodeFormatQRCode);
        formats = [@(MLKBarcodeFormatCode39|MLKBarcodeFormatDataMatrix|MLKBarcodeFormatQRCode) integerValue];
    } else if([_barcodeFormats  isEqual: @1234]) {

    } else {
        formats = [_barcodeFormats integerValue];
    }
    //NSLog(@"_barcodeFormats %@, %@", _barcodeFormats, formats);

    // Initialize barcode detector.

    MLKBarcodeScannerOptions *barcodeScannerOptions = [[MLKBarcodeScannerOptions alloc] initWithFormats:formats];
    self.mlkBarcodeDetector = [MLKBarcodeScanner barcodeScannerWithOptions:barcodeScannerOptions];

    self.mlkTextDetector = [MLKTextRecognizer textRecognizer];


    gCIContext = [CIContext contextWithMTLDevice:MTLCreateSystemDefaultDevice()];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];

    self.previewLayer.frame = self.view.layer.bounds;
    self.previewLayer.position = CGPointMake(CGRectGetMidX(self.previewLayer.frame),
                                             CGRectGetMidY(self.previewLayer.frame));
}

- (void)viewDidUnload {
    [self cleanupCaptureSession];
    [super viewDidUnload];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    //Force portrait orientation.
    [[UIDevice currentDevice] setValue:
     [NSNumber numberWithInteger: UIInterfaceOrientationPortrait]
                                forKey:@"orientation"];

    [self.session startRunning];

}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self.session stopRunning];
}

#pragma mark - AVCaptureVideoDataOutputSampleBufferDelegate

- (void)captureOutput:(AVCaptureOutput *)captureOutput
didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
       fromConnection:(AVCaptureConnection *)connection {
    UIImage *image = [self imageFromSampleBuffer:sampleBuffer];

    //We're going to crop UIImage to the onscreen viewfinder's box size for faster processing.
    //Rotate the image.
    UIImage * portraitImage = [[UIImage alloc] initWithCGImage: image.CGImage
                                                         scale: 1.0
                                                   orientation: UIImageOrientationRight];
    //Define the crop coordinates.
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat screenWidth = screenRect.size.width;
    CGFloat screenHeight = screenRect.size.height;

    CGFloat frameWidth = screenWidth*_scanAreaHeight;
    CGFloat frameHeight = screenHeight*_scanAreaWidth;

    CGFloat imageWidth = image.size.width;
    CGFloat imageHeight = image.size.height;

    CGFloat actualFrameWidth = 0;
    CGFloat actualFrameHeight = 0;

    //Figure out which ratio is bigger and then subtract a value off the frame width in case some of the camera preview is hanging off screen.
    if(imageWidth/screenWidth < imageHeight/screenHeight){
        actualFrameWidth = frameWidth * imageWidth/screenWidth - frameWidth/(imageHeight/screenHeight);
        actualFrameHeight = frameHeight * imageHeight/screenHeight;
    } else {
        actualFrameWidth = frameWidth * imageWidth/screenWidth;
        actualFrameHeight = frameHeight * imageHeight/screenHeight - frameHeight/(imageWidth/screenWidth);
    }
    //Define crop rectangle.
    CGRect cropRect = CGRectMake(imageWidth/2 - actualFrameWidth/2, imageHeight/2 - actualFrameHeight/2, actualFrameWidth, actualFrameHeight);
    //Crop image
    UIImage *croppedImg = [self croppIngimageByImageName:portraitImage toRect:cropRect];

    //Test code to place the resultant cropped image onto the display to verify sizing.
    /*dispatch_async(dispatch_get_main_queue(), ^{
     UIImage *previewImage = [[UIImage alloc] initWithCGImage: croppedImg.CGImage
     scale: 1.0
     orientation: UIImageOrientationRight];
     self.imageView.image = previewImage;
     });//*/



    MLKVisionImage *visionImage = [[MLKVisionImage alloc] initWithImage:croppedImg];
    visionImage.orientation = croppedImg.imageOrientation;
    NSArray<MLKBarcode *> *mlkBarcodes = [self.mlkBarcodeDetector resultsInImage:visionImage error:nil];


    if(sizeof mlkBarcodes == 0) {
        croppedImg = [self rotateImage:croppedImg toOrientation:UIImageOrientationRight];
        visionImage.orientation = UIImageOrientationRight;
        mlkBarcodes = [self.mlkBarcodeDetector resultsInImage:visionImage error:nil];
    }

    NSArray<MLKTextBlock *> *mlkTextBlocks = nil;
    if ([_barcodeFormats  isEqual: @0]) {
        // Run OCR on VIN scans.
        visionImage.orientation = croppedImg.imageOrientation;
        MLKText *mlkTexts = [self.mlkTextDetector resultsInImage:visionImage error:nil];

        MLKVisionImage *visionImageRotated = [[MLKVisionImage alloc] initWithImage:croppedImg];
        visionImageRotated.orientation = UIImageOrientationDown;

        MLKText *mlkRotatedTexts = [self.mlkTextDetector resultsInImage:visionImageRotated error:nil];

        mlkTextBlocks = [mlkTexts.blocks arrayByAddingObjectsFromArray:mlkRotatedTexts.blocks];
        //mlkTextBlocks = [mlkTexts.blocks arrayByAddingObject:mlkRotatedTexts.blocks];
    }

    //Iterate through barcodes.
    dispatch_sync(dispatch_get_main_queue(), ^{
        for (MLKBarcode *barcode in mlkBarcodes) {
            if([barcode.rawValue length] < 17) {
                continue;
            }

            NSString * vin = barcode.rawValue;

            //Remove characters from the set that are not part of standardized VIN structure.
            NSCharacterSet *doNotWant = [NSCharacterSet characterSetWithCharactersInString:@"ioqIOQ"];
            vin = [[vin componentsSeparatedByCharactersInSet: doNotWant] componentsJoinedByString: @""];

            //Cut length of VIN down to 17 characters. In testing some barcodes added an 18th character to the result that was invalid, but the rest of the VIN was correct.
            vin = [vin substringToIndex: MIN(17, [vin length])];

            //If VIN is valid (check digit is correct) then cleanup the session and send results to Cordova.
            if([self validateVin:vin] == true) {
                NSLog(@"Scanner: VIN Barcode value: %@",vin);
                [ self cleanupCaptureSession];
                [_session stopRunning];
                [delegate sendResult:vin barcodeType: [NSString stringWithFormat:@"%li", (long)barcode.format]];
                break;
            }

        }

        if([_barcodeFormats  isEqual: @0]) {
            for(MLKTextBlock *textBlock in mlkTextBlocks) {
                NSString* val = textBlock.text;

                NSError *error = NULL;
                NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"([ABCDEFGHJKLMNPRSTUVWXYZ0-9]{8}[0-9X]{1}[ABCDEFGHJKLMNPRSTUVWXYZ0-9]{3}[0-9]{5})" options:0 error:&error];
                NSArray *matches = [regex matchesInString:val options:0 range:NSMakeRange(0, [val length])];
                NSUInteger matchCount = [matches count];
                if (matchCount == 0) continue;

                if (matchCount) {
                    for (NSUInteger matchIdx = 0; matchIdx < matchCount; matchIdx++) {
                        NSTextCheckingResult *match = [matches objectAtIndex:matchIdx];
                        NSRange matchRange = [match range];
                        NSString *vin = [val substringWithRange:matchRange];

                        if([self validateVin:vin] == true) {
                            NSLog(@"Scanner: Verified Text VIN value: %@",vin);
                            [ self cleanupCaptureSession];
                            [_session stopRunning];
                            [delegate sendResult:vin barcodeType:@"OCR"];
                            break;
                        }
                    }
                }
            }
        }
    });
}

- (nullable UIImage *)imageFromSampleBuffer:(CMSampleBufferRef)sampleBuffer {
  if (sampleBuffer == NULL) {
    NSLog(@"Sample buffer is NULL.");
    return nil;
  }
  CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
  if (imageBuffer == NULL) {
    NSLog(@"Invalid sample buffer.");
    return nil;
  }

  CVPixelBufferLockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);

  void *baseAddress = CVPixelBufferGetBaseAddress(imageBuffer);
  size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);
  size_t bitPerComponent = 8;  // TODO(zhoumi): This may vary on other formats.

  size_t width = CVPixelBufferGetWidth(imageBuffer);
  size_t height = CVPixelBufferGetHeight(imageBuffer);

  // TODO(zhoumi): Add more support for non-RGB color space.
  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();

  if (colorSpace == NULL) {
    NSLog(@"Failed to create RGB color space");
    CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
    return nil;
  }

  // TODO(zhoumi): Add more support for other formats.
  CGContextRef context =
      CGBitmapContextCreate(baseAddress, width, height, bitPerComponent, bytesPerRow, colorSpace,
                            kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
  if (context == NULL) {
    NSLog(@"Failed to create CGContextRef");
    CGColorSpaceRelease(colorSpace);
    CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
    return nil;
  }

  CGImageRef cgImage = CGBitmapContextCreateImage(context);
  if (cgImage == NULL) {
    NSLog(@"Failed to create CGImage");
    CGColorSpaceRelease(colorSpace);
    CGContextRelease(context);
    CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
    return nil;
  }

  CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
  CGContextRelease(context);
  CGColorSpaceRelease(colorSpace);

  UIImage *image = [UIImage imageWithCGImage:cgImage];
  CGImageRelease(cgImage);
  return image;
}

- (CMSampleBufferRef)croppedSampleBuffer:(CMSampleBufferRef)sampleBuffer withRect:(CGRect)rect {
  CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);

  CVPixelBufferLockBaseAddress(imageBuffer, 0);

  size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);
  size_t width = CVPixelBufferGetWidth(imageBuffer);
  size_t bytesPerPixel = bytesPerRow / width;
  void *baseAddressStart = CVPixelBufferGetBaseAddress(imageBuffer);

  NSUInteger cropX = rect.origin.x;
  NSUInteger cropY = rect.origin.y;

  // Start pixel in RGB color space can't be odd.
  if (cropX % 2 != 0) {
    cropX++;
  }

  NSUInteger cropStartOffset = cropY * bytesPerRow + cropX * bytesPerPixel;

  CVPixelBufferRef pixelBuffer = NULL;
  CVReturn error;

  // Initiates pixelBuffer.
  OSType pixelFormat = CVPixelBufferGetPixelFormatType(imageBuffer);
  NSDictionary<NSString *, NSNumber *> *options = @{
    (__bridge NSString *)kCVPixelBufferCGImageCompatibilityKey : @YES,
    (__bridge NSString *)kCVPixelBufferCGBitmapContextCompatibilityKey : @YES,
    (__bridge NSString *)kCVPixelBufferWidthKey : @(rect.size.width),
    (__bridge NSString *)kCVPixelBufferHeightKey : @(rect.size.height),
  };

  error = CVPixelBufferCreateWithBytes(kCFAllocatorDefault,                 // allocator
                                       rect.size.width,                     // width
                                       rect.size.height,                    // height
                                       pixelFormat,                         // pixelFormatType
                                       &baseAddressStart[cropStartOffset],  // baseAddress
                                       bytesPerRow,                         // bytesPerRow
                                       NULL,                                // releaseCallback
                                       NULL,                                // releaseRefCon
                                       (__bridge CFDictionaryRef)options,   // pixelBufferAttributes
                                       &pixelBuffer);                       // pixelBuffer
  if (error != kCVReturnSuccess) {
    NSLog(@"Crop CVPixelBufferCreateWithBytes error %d", (int)error);
    return NULL;
  }

  // Cropping using CIImage.
  CIImage *ciImage = [CIImage imageWithCVImageBuffer:imageBuffer];
  ciImage = [ciImage imageByCroppingToRect:rect];
  // CIImage is not in the original point after cropping. So we need to pan.
  ciImage = [ciImage imageByApplyingTransform:CGAffineTransformMakeTranslation(-cropX, -cropY)];

  [gCIContext render:ciImage toCVPixelBuffer:pixelBuffer];

  // Prepares sample timing info.
  CMSampleTimingInfo sampleTime = {
      .duration = CMSampleBufferGetDuration(sampleBuffer),
      .presentationTimeStamp = CMSampleBufferGetPresentationTimeStamp(sampleBuffer),
      .decodeTimeStamp = CMSampleBufferGetDecodeTimeStamp(sampleBuffer)};

  CMVideoFormatDescriptionRef videoInfo;
  error =
      CMVideoFormatDescriptionCreateForImageBuffer(kCFAllocatorDefault, pixelBuffer, &videoInfo);
  if (error != kCVReturnSuccess) {
    NSLog(@"CMVideoFormatDescriptionCreateForImageBuffer error %d", (int)error);
    CVPixelBufferRelease(pixelBuffer);
    CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
    return NULL;
  }

  // Creates `CMSampleBufferRef`.
  CMSampleBufferRef resultBuffer = NULL;
  error = CMSampleBufferCreateForImageBuffer(kCFAllocatorDefault, pixelBuffer, true, NULL, NULL,
                                             videoInfo, &sampleTime, &resultBuffer);
  if (error != kCVReturnSuccess) {
    NSLog(@"CMSampleBufferCreateForImageBuffer error %d", (int)error);
  }
  CFRelease(videoInfo);
  CVPixelBufferRelease(pixelBuffer);
  CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly);
  return resultBuffer;
}



#pragma mark - VIN Validator

- (int)transliterate:(NSString *) c {
    NSString *values = @"0123456789.ABCDEFGH..JKLMN.P.R..STUVWXYZ";
    NSRange range = [values rangeOfString:c];

    return (int)range.location % 10;
}
- (NSString *)getCheckDigit:(NSString *) vin {
    NSString *map = @"0123456789X";
    NSString *weights = @"8765432X098765432";
    NSInteger *sum = 0;
    for (int i = 0; i < 17; i++)
    {
        NSString * weight = [weights substringWithRange:NSMakeRange(i, 1)];
        NSRange mapIndex = [map rangeOfString:weight];
        NSString * vinChar = [vin substringWithRange:NSMakeRange(i, 1)];
        NSInteger transliteration = [self transliterate:vinChar];

        sum = ((long)sum)+((long)transliteration * (unsigned long)mapIndex.location);
    }
    NSInteger checkIndex = ((int)sum) % 11;
    NSString * checkDigit = [map substringWithRange:NSMakeRange(checkIndex, 1)];
    return checkDigit;
}

- (Boolean)validateVin:(NSString *) vin {
    vin = [vin substringToIndex: MIN(17, [vin length])];
    if([vin length] != 17) {
        return false;
    }

    NSString *checkDigit = [vin substringWithRange:NSMakeRange(8, 1)];
    NSString *correctCheckDigit = [self getCheckDigit:vin];
    NSLog(@"VIN check digit is %@ and correct check digit is %@.", checkDigit, correctCheckDigit);
    return checkDigit == correctCheckDigit;
}


#pragma mark - Camera setup

- (void)cleanupVideoProcessing {
    if (self.videoDataOutput) {
        [self.session removeOutput:self.videoDataOutput];
    }
    self.videoDataOutput = nil;
}

- (void)cleanupCaptureSession {
    [self.session stopRunning];
    [self cleanupVideoProcessing];
    self.session = nil;
    [self.previewLayer removeFromSuperlayer];
}

- (void)setUpVideoProcessing {
    self.videoDataOutput = [[AVCaptureVideoDataOutput alloc] init];
    NSDictionary *rgbOutputSettings = @{
                                        (__bridge NSString*)kCVPixelBufferPixelFormatTypeKey : @(kCVPixelFormatType_32BGRA)
                                        };
    [self.videoDataOutput setVideoSettings:rgbOutputSettings];

    if (![self.session canAddOutput:self.videoDataOutput]) {
        [self cleanupVideoProcessing];
        NSLog(@"Failed to setup video output");
        return;
    }
    [self.videoDataOutput setAlwaysDiscardsLateVideoFrames:YES];
    [self.videoDataOutput setSampleBufferDelegate:self queue:self.videoDataOutputQueue];
    [self.session addOutput:self.videoDataOutput];
}

- (void)setUpCameraPreview {
    self.previewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:self.session];
    [self.previewLayer setBackgroundColor:[UIColor blackColor].CGColor];
    [self.previewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];

    self.previewLayer.frame = self.view.superview.bounds;
    [self.view.layer addSublayer:self.previewLayer];

    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat screenWidth = screenRect.size.width;
    CGFloat screenHeight = screenRect.size.height;

    CGFloat frameWidth = screenWidth*_scanAreaWidth;
    CGFloat frameHeight = screenHeight*_scanAreaHeight;

    UILabel* _label1 = [[UILabel alloc] init];
    _label1.frame = CGRectMake(screenWidth/2 - frameWidth/2, screenHeight/2 - frameHeight/2, frameWidth, frameHeight);
    _label1.layer.masksToBounds = NO;
    _label1.layer.cornerRadius = 30;
    _label1.userInteractionEnabled = YES;
    _label1.layer.borderColor = [UIColor whiteColor].CGColor;
    _label1.layer.borderWidth = 3.0;
    UITapGestureRecognizer* tapScanner = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(focusAtPoint:)];
    [_label1 addGestureRecognizer:tapScanner];

    CGFloat buttonSize = 45.0;


    UIButton *_cancelButton = [[UIButton alloc] init];
    [_cancelButton addTarget:self
                      action:@selector(closeView:)
            forControlEvents:UIControlEventTouchUpInside];

    NSString * cancelBase64String = @"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAQAAABpN6lAAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAAAmJLR0QAAKqNIzIAAAAJcEhZcwAADdcAAA3XAUIom3gAAAAHdElNRQfhCxMVEyaNvw4TAAADNElEQVR42u2dv1LqQBSHv1DwBjY0FjyAA7wCtna2ttrwLr6GY4PWKfQBJGNtb2PDMFQ2ewvmDheBC0l295zjYVO6Cb/vC4b82c0p+Lf1GDNkwAVfzKh4oyRguxWMGTFgyBnvVMwo+dzd8Y4F4cfyQl+aoFXr87LFtOCW4mfHc8qtjqtlyWS7u4lWMGG5h6rkfBN/vqfjapnSlaap3bpM/8s0Xyso9u799fJkTEGXp4NM5d9v9t3BrtYUHIMfCNwC9HYc+mwrOBY/sKAHN0d2tqLgePxA4Abua3TXr6AefuAeXmutoFtBXfzAKwd+AC0pqI8fmMNH7ZV0KmiCH/iAhwar6VPQDD/w0KFq9IFXPCpS0OWRq0ZrVnDZyJymb0HTvR8IXEKx43rJkoI2+C+rk+H+3msm/Qra4C/Xl/qTxhuRVdAGPzBZb6g4cOmoU0E7/OnmXY52G5NQED2xLQVJ0tpRkCypDQVJU+pXkDyhbgVZ0ulVkC2ZTgVZU+lTkD2RLgUiafQoEEuiQ4FoCnkF4glkA4jjy4ZQgS8XRA2+TBhV+PkDqcPPG0olfr5gavHzhFONnz6gevy0IU3gpwtqBj9NWFP48QObw48b2iR+vOBm8eOEN40fQ4Fx/PYKzONLKVCEL6FAGX5uBQrxcypQip9LgWL8HAqU46dWYAA/pQIj+KkUGMJPocAYfmwFBvFjKjCKH0uBYfwYCpLjd6QN/e7m/F/A+UHQ+c+g8xMh56fCzi+GnF8OO78h4vyWmPObos5vizt/MOL80Zjzh6POH487HyDhfIiM80FSzofJOR8o6XyorPPB0s6HyzufMOF8yozzSVPOp805nzjpfOqs88nTzqfPiwfw/v4I0RQ68MWS6MEXSaMLP3siffhZU+nEz5ZML36WdLrxkyfUj580pQ38ZEnt4CdJaws/emL3r9Z2/nJ156/Xd19gwXmJjQ6jhh/7zDXf0uwAfHPNc8N1R+7L7JwKLZ1KbTkvtla30pSeQ9+uVv9wWJ0KLp5Kbrovunoqu4v7wssrBa5Lb6+6uyy+vrlve4wZMuCCL2ZUvFESpBlatoIxIwYMOeOdihkln+s//wFFdoCM42fEswAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAxNy0xMS0xOVQyMToxOTozOCswMTowMPNH2M8AAAAldEVYdGRhdGU6bW9kaWZ5ADIwMTctMTEtMTlUMjE6MTk6MzgrMDE6MDCCGmBzAAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAAABJRU5ErkJggg==";

    NSURL *cancelImageUrl = [NSURL URLWithString:cancelBase64String];
    NSData *cancelImageData = [NSData dataWithContentsOfURL:cancelImageUrl];
    UIImage *cancelIcon = [UIImage imageWithData:cancelImageData];
    [_cancelButton setImage:cancelIcon
                   forState:UIControlStateNormal];

    CGFloat screenOffset = (screenWidth/2 - frameWidth/2)/2 - buttonSize/2;
    NSLog(@"screenOffset %f", screenOffset);

    _cancelButton.frame = CGRectMake(screenOffset, screenHeight-screenOffset-buttonSize, buttonSize, buttonSize);
    _cancelButton.backgroundColor = [UIColor colorWithWhite:1 alpha:0.4];
    _cancelButton.transform=CGAffineTransformMakeRotation(M_PI / 2);
    _cancelButton.layer.cornerRadius = buttonSize/2;
    _cancelButton.contentEdgeInsets = UIEdgeInsetsMake(15, 15, 15, 15);

    [self.view addSubview:_cancelButton];




    self.torchButton = [[UIButton alloc] init];
    [self.torchButton addTarget:self
                         action:@selector(toggleFlashlight:)
               forControlEvents:UIControlEventTouchUpInside];

    NSString * torchBase64String = @"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAQAAABpN6lAAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAAAmJLR0QAAKqNIzIAAAAJcEhZcwAADdcAAA3XAUIom3gAAAAHdElNRQfhCxMVAzOqoPipAAADM0lEQVR42u2dv2sTcRyG38RWzGYHhxYHp+B+Lk7iIlhFXZvdPeAouOuS1X/AVCiiCBU6OjgIOffG+gvEgCIqlsQi9JxEbC/XJt8fz136eTJecve+T+6Su8t9czX5oqmWEiVa9DbHfAZKlaqrfuDlTERNbQ2VRXwM1VaNrv2v/nrU8n8f62VR0EbqZ8rUpqtLUjPyyv//htB0jV93FrCiBia/oRYv4BxWX5IS1xm4f4x8Cv7FV8RAS7SADKzvoYH7JlBxTAAdgMYE0AFoTAAdwDAMwzAMo7q8ws4IZsr0wjW++57gFqrfeenuAt6YAJLXvADbBFABJVgDPmobrP+DF5BpAxPwyH0WPk6IPKmyAB8/MJ/UZ80D9T/ojPtMfKwB3/UcqO/l/fd1TvAxIuAhstRclrQT/ShgzU/0Y17m8lMLOh9V+UjX3L8CfbKgr1Hf/zt04f3EvFLorU7QdfdzXFuR6v/SRbpsPjci1b9MFx3P7eD1d3SFLlnM3cD1r9IFD+Z+wJW/AvWluh4EqT/UJbraYZnTmvf627pA12IV3KQrTa7gqcf6z+g603BKXzzV/+Z6NSjFiicB9+gi07PhRcBZusb0XPdQ3/nXP5J5D58Dt8JGDHuZ3G8Pp60C//AS+jrBl85zeBc24FxgAe/HTulrVT2lkhIlao0d/RNYQGhO527Xu+rsGWnUUEe7uc+tOPXc+su5z13OVVB59lfqjH1u5ygI2CwYZtfQZmwB8a8WX9Vo7LSRurHjxBfQK5yaRs8TnL2rdPEow8XZ3wRKRnwBicPUmRBQPNY4uoDwlPxrML6AI78jZLvC5ToYCv83JOMqHPZwOHBCTkBJEtqOEB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgTQAWhMAB2AxgQEX8IAfHUpBLgNhg0+lDa8gJ7Tq2dgLLHLbXk93Fa3DEz/d6ttOrofpr01d2lure1DwaQ3Z492c/V4jptqKVFy4F2KB0qVqqt+nFh/ADjJgLiaxweIAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDE3LTExLTE5VDIxOjAzOjUxKzAxOjAwdhzbkgAAACV0RVh0ZGF0ZTptb2RpZnkAMjAxNy0xMS0xOVQyMTowMzo1MSswMTowMAdBYy4AAAAZdEVYdFNvZnR3YXJlAHd3dy5pbmtzY2FwZS5vcmeb7jwaAAAAAElFTkSuQmCCconv";

    NSURL *torchImageUrl = [NSURL URLWithString:torchBase64String];
    NSData *torchImageData = [NSData dataWithContentsOfURL:torchImageUrl];
    UIImage *torchIcon = [UIImage imageWithData:torchImageData];
    [self.torchButton setImage:torchIcon
                      forState:UIControlStateNormal];

    self.torchButton.frame = CGRectMake(screenWidth-screenOffset-buttonSize, screenHeight-screenOffset-buttonSize, buttonSize, buttonSize);
    self.torchButton.backgroundColor = [UIColor colorWithWhite:1 alpha:0.4];
    self.torchButton.transform=CGAffineTransformMakeRotation(M_PI / 2);
    self.torchButton.layer.cornerRadius = buttonSize/2;
    self.torchButton.contentEdgeInsets = UIEdgeInsetsMake(10, 10, 10, 10);

    [self.view addSubview:self.torchButton];

    [self.view addSubview:_label1];

    self.imageView = [[UIImageView alloc] initWithImage:nil];

    UIView *catView = [[UIView alloc] initWithFrame:CGRectMake(0,0,frameWidth,frameHeight)];
    self.imageView.frame = catView.bounds;

    // add the imageview to the superview
    [catView addSubview:self.imageView];

    //add the view to the main view

    [self.view addSubview:catView];

}

#pragma mark - Helper Functions

- (void)focusAtPoint:(id) sender{
    NSLog(@"captured touch");
    CGPoint touchPoint = [(UITapGestureRecognizer*)sender locationInView:self.view];
    double focus_x = touchPoint.x/self.previewLayer.frame.size.width;
    double focus_y = (touchPoint.y+66)/self.previewLayer.frame.size.height;

    NSError *error;
    NSArray *devices = [AVCaptureDevice devices];
    for (AVCaptureDevice *device in devices){
        NSLog(@"Device name: %@", [device localizedName]);
        if ([device hasMediaType:AVMediaTypeVideo]) {
            if ([device position] == AVCaptureDevicePositionBack) {
                NSLog(@"Device position : back");
                CGPoint point = CGPointMake(focus_y, 1-focus_x);
                if ([device isFocusModeSupported:AVCaptureFocusModeContinuousAutoFocus] && [device lockForConfiguration:&error]){
                    [device setFocusPointOfInterest:point];

                    for (UIView *subView in self.view.subviews)
                    {
                        if (subView.tag == 99)
                        {
                            [subView removeFromSuperview];
                        }
                    }

                    CGRect rect = CGRectMake(touchPoint.x-30, touchPoint.y-30, 60, 60);
                    UIView *focusRect = [[UIView alloc] initWithFrame:rect];
                    focusRect.layer.borderColor = [UIColor colorWithRed:0.98 green:0.80 blue:0.18 alpha:.7].CGColor;
                    focusRect.layer.borderWidth = 1;
                    focusRect.tag = 99;
                    [self.view addSubview:focusRect];

                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
                        [focusRect removeFromSuperview];
                    });

                    [device setFocusMode:AVCaptureFocusModeAutoFocus];
                    [device unlockForConfiguration];
                }
            }
        }
    }
}
static inline double radians (double degrees) {return degrees * M_PI/180;}
- (UIImage*) rotateImage:(UIImage*)src toOrientation:(UIImageOrientation) orientation
{
    UIGraphicsBeginImageContext(src.size);

    CGContextRef context = UIGraphicsGetCurrentContext();

    if (orientation == UIImageOrientationRight) {
        CGContextRotateCTM (context, radians(90));
    } else if (orientation == UIImageOrientationLeft) {
        CGContextRotateCTM (context, radians(-90));
    } else if (orientation == UIImageOrientationDown) {
        // NOTHING
    } else if (orientation == UIImageOrientationUp) {
        CGContextRotateCTM (context, radians(90));
    }

    [src drawAtPoint:CGPointMake(0, 0)];

    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)croppIngimageByImageName:(UIImage *)imageToCrop toRect:(CGRect)rect
{
    //CGRect CropRect = CGRectMake(rect.origin.x, rect.origin.y, rect.size.width, rect.size.height+15);

    
    CGImageRef imageRef = CGImageCreateWithImageInRect([imageToCrop CGImage], rect);
    UIImage *cropped = [UIImage imageWithCGImage:imageRef];
    CGImageRelease(imageRef);
    
    return cropped;
}

- (void) toggleFlashlight:(id)sender
{
    // check if flashlight available
    Class captureDeviceClass = NSClassFromString(@"AVCaptureDevice");
    if (captureDeviceClass != nil) {
        AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
        if ([device hasTorch] && [device hasFlash]){
            
            [device lockForConfiguration:nil];
            if (device.torchMode == AVCaptureTorchModeOff)
            {
                self.torchButton.backgroundColor = [UIColor colorWithWhite:1 alpha:1];
                [device setTorchMode:AVCaptureTorchModeOn];
                [device setFlashMode:AVCaptureFlashModeOn];
                //torchIsOn = YES;
            }
            else
            {
                self.torchButton.backgroundColor = [UIColor colorWithWhite:1 alpha:.4];
                [device setTorchMode:AVCaptureTorchModeOff];
                [device setFlashMode:AVCaptureFlashModeOff];
                // torchIsOn = NO;
            }
            [device unlockForConfiguration];
        }
    } }

- (void) closeView :(id)sender{
    
    [ self cleanupCaptureSession];
    
    [_session stopRunning];
    
    [delegate closeScanner];
}


- (void)updateCameraSelection {
    [self.session beginConfiguration];
    
    // Remove old inputs
    NSArray *oldInputs = [self.session inputs];
    for (AVCaptureInput *oldInput in oldInputs) {
        [self.session removeInput:oldInput];
    }
    
    AVCaptureDevicePosition desiredPosition = AVCaptureDevicePositionBack;
    AVCaptureDeviceInput *input = [self captureDeviceInputForPosition:desiredPosition];
    if (!input) {
        // Failed, restore old inputs
        for (AVCaptureInput *oldInput in oldInputs) {
            [self.session addInput:oldInput];
        }
    } else {
        // Succeeded, set input and update connection states
        [self.session addInput:input];
    }
    [self.session commitConfiguration];
}

- (AVCaptureDeviceInput *)captureDeviceInputForPosition:(AVCaptureDevicePosition)desiredPosition {
    for (AVCaptureDevice *device in [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo]) {
        if (device.position == desiredPosition) {
            NSError *error = nil;
            AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:device
                                                                                error:&error];
            if (error) {
                NSLog(@"Could not initialize for AVMediaTypeVideo for device %@", device);
            } else if ([self.session canAddInput:input]) {
                return input;
            }
        }
    }
    return nil;
}



@end


