//
//  CDViOSScanner.m
//  Dealr, Inc.
//

//#import <AVFoundation/AVFoundation.h>
#import "CDViOSScanner.h"

@class UIViewController;

@interface CDViOSScanner ()
{
    NSInteger _previousStatusBarStyle;
}
@end


@implementation CDViOSScanner

- (void)pluginInitialize
{
    _previousStatusBarStyle = -1;
}


- (void) startScan:(CDVInvokedUrlCommand *)command
{
    //Force portrait orientation.
    [[UIDevice currentDevice] setValue:
     [NSNumber numberWithInteger: UIInterfaceOrientationPortrait]
                                forKey:@"orientation"];
    dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"Arguments %@", command.arguments);
        if(self->_scannerOpen == YES) {
            //Scanner is currently open, throw error.
            NSArray *response = @[@"SCANNER_OPEN", @"", @""];
            CDVPluginResult *pluginResult=[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:response];

            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } else {
            //Open scanner.
            self->_scannerOpen = YES;
            self.cameraViewController = [[CameraViewController alloc] init];

            self.cameraViewController.delegate = self;

            //Provide settings to the camera view.
            NSNumberFormatter *f = [[NSNumberFormatter alloc] init];
            f.numberStyle = NSNumberFormatterDecimalStyle;
            NSNumber * barcodeFormats = [command argumentAtIndex:0 withDefault:@1234];
            self.cameraViewController.scanAreaWidth = (CGFloat)[[command argumentAtIndex:1 withDefault:@.5] floatValue];
            self.cameraViewController.scanAreaHeight = (CGFloat)[[command argumentAtIndex:2 withDefault:@.7] floatValue];
            self.cameraViewController.barcodeFormats = barcodeFormats;
            self.cameraViewController.modalPresentationStyle = UIModalPresentationFullScreen;

            NSLog(@"Test %@, width: %f, height: %f, barcodeFormats: %@",[command.arguments objectAtIndex:2], self.cameraViewController.scanAreaWidth, self.cameraViewController.scanAreaHeight, self.cameraViewController.barcodeFormats);

            [self.viewController presentViewController:self.cameraViewController animated: NO completion:nil];
            self->_callback = command.callbackId;
        }
    });

}

-(void)sendResult:(NSString *)value
{
    [self.cameraViewController dismissViewControllerAnimated:NO completion:nil];
    _scannerOpen = NO;

    NSArray *response = @[value, @"", @""];
    CDVPluginResult *pluginResult=[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:response];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:_callback];
}

-(void)closeScanner
{
    [self.cameraViewController dismissViewControllerAnimated:NO completion:nil];
    _scannerOpen = NO;

    NSArray *response = @[@"USER_CANCELLED", @"", @""];
    CDVPluginResult *pluginResult=[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:response];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:_callback];

}


- (void)show:(CDVInvokedUrlCommand*)command
{
    if (self.cameraViewController == nil) {
        NSLog(@"Tried to show scanner after it was closed.");
        return;
    }
    if (_previousStatusBarStyle != -1) {
        NSLog(@"Tried to show scanner while already shown");
        return;
    }

    _previousStatusBarStyle = [UIApplication sharedApplication].statusBarStyle;

    __block UINavigationController* nav = [[UINavigationController alloc]
                                           initWithRootViewController:self.cameraViewController];
    //nav.orientationDelegate = self.cameraViewController;
    nav.navigationBarHidden = YES;
    nav.modalPresentationStyle = UIModalPresentationFullScreen;

    __weak CDViOSScanner* weakSelf = self;

    // Run later to avoid the "took a long time" log message.
    dispatch_async(dispatch_get_main_queue(), ^{
        if (weakSelf.cameraViewController != nil) {
            CGRect frame = [[UIScreen mainScreen] bounds];
            UIWindow *tmpWindow = [[UIWindow alloc] initWithFrame:frame];
            UIViewController *tmpController = [[UIViewController alloc] init];
            [tmpWindow setRootViewController:tmpController];
            [tmpWindow setWindowLevel:UIWindowLevelNormal];

            [tmpWindow makeKeyAndVisible];
            [tmpController presentViewController:nav animated:NO completion:nil];
        }
    });
}

@end
