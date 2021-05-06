//
//  CDViOSScanner.h
//  Dealr, Inc.
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import "CameraViewController.h"

@class UIViewController;

@interface CDViOSScanner : CDVPlugin {
    NSString *_callback;
    Boolean _scannerOpen;
}

@property (nonatomic, retain) CameraViewController* cameraViewController;

- (void) startScan:(CDVInvokedUrlCommand *)command;

@end
