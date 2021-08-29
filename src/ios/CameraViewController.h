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

@import UIKit;

#import <CoreMedia/CoreMedia.h>
#import <UIKit/UIKit.h>

// View controller demonstraing how to use the barcode detector with the AVFoundation
// video pipeline.
@protocol senddataProtocol <NSObject>

-(void)closeScanner;
-(void)sendResult:(NSString *)result barcodeType:(NSString *)type;
/**
 * Crops `CMSampleBuffer` to a specified rect. This will not alter the original data. Currently this
 * method only handles `CMSampleBufferRef` with RGB color space.
 *
 * @param sampleBuffer The original `CMSampleBuffer`.
 * @param rect The rect to crop to.
 * @return A `CMSampleBuffer` cropped to the given rect.
 */
+ (CMSampleBufferRef)croppedSampleBuffer:(CMSampleBufferRef)sampleBuffer withRect:(CGRect)rect;

@end

@interface CameraViewController : UIViewController

@property(nonatomic,assign)id delegate;
@property(nonatomic,assign) NSNumber *barcodeFormats;
@property(nonatomic,assign) CGFloat scanAreaWidth;
@property(nonatomic,assign) CGFloat scanAreaHeight;
@property(nonatomic,assign) CGFloat scanAreaZoom;

@end

