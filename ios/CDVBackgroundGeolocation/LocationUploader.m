//
//  LocationUploader.m
//  CDVBackgroundGeolocation
//
//  Created by Marian Hello on 07/07/16.
//  Copyright © 2016 mauron85. All rights reserved.
//

#import "UIKit/UIKit.h"
#import "Logging.h"
#import "LocationUploader.h"
#import "SQLiteLocationDAO.h"

#define HOST @"http://192.168.81.15/notify"

@interface LocationUploader ()  <NSURLSessionDelegate, NSURLSessionTaskDelegate>
{
    NSURLSession *urlSession;
    NSMutableArray *tasks;
}
@end

@implementation LocationUploader

- (instancetype) init
{
    if(!(self = [super init])) return nil;
    
    NSURLSessionConfiguration *conf = [NSURLSessionConfiguration backgroundSessionConfiguration:@"com.marianhello.session"];
    conf.allowsCellularAccess = YES;
    urlSession = [NSURLSession sessionWithConfiguration:conf delegate:self delegateQueue:[NSOperationQueue mainQueue]];
    
    return self;
}

- (void)start
{
    __block UIBackgroundTaskIdentifier bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
    }];
    
    [self notify:@"start"];
    
    [urlSession getTasksWithCompletionHandler:^(NSArray *dataTasks, NSArray *uploadTasks, NSArray *downloadTasks) {
        for(NSURLSessionUploadTask *task in uploadTasks) {
            DDLogInfo(@"Restored upload task %zu for %@", (unsigned long)task.taskIdentifier, task.originalRequest.URL);
            [tasks addObject:task];
            [task resume];
        }
        
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
    }];
}

- (void)cancel
{
    for(NSURLSessionTask *task in tasks) {
        [task cancel];
    }
}

- (void)notify:(NSString*)what
{
    __block UIBackgroundTaskIdentifier task = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        DDLogInfo(@"Oops, notify expired");
        [[UIApplication sharedApplication] endBackgroundTask:task];
    }];
    
    DDLogDebug(@"Notifying %@", what);
    [NSURLConnection sendAsynchronousRequest:[NSURLRequest
        requestWithURL:[NSURL URLWithString:[NSString stringWithFormat: HOST @"?name=%@", what]]]
        queue:[NSOperationQueue mainQueue]
        completionHandler:^(NSURLResponse *response, NSData *data, NSError *connectionError) {
            DDLogDebug(@"Finished notifying %@", what);
            [[UIApplication sharedApplication] endBackgroundTask:task];
    }];
}

- (void) sync:(NSString*)url onLocationThreshold:(NSInteger)threshold;
{
    SQLiteLocationDAO* locationDAO = [SQLiteLocationDAO sharedInstance];
    NSNumber *locationsCount = [locationDAO getLocationsCount];
    
    if (locationsCount && [locationsCount integerValue] < threshold) return;
    
    NSArray *locations = [locationDAO getLocationsForSync];
    
    NSMutableArray *jsonArray = [[NSMutableArray alloc] initWithCapacity:[locations count]];
    for (Location *location in locations) {
        [jsonArray addObject:[location toDictionary]];
    }
    
    NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:jsonArray options:0 error:&error];
    
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    dateFormatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    dateFormatter.dateFormat = @"yyyyMMdd_HHmms";
    dateFormatter.timeZone = [NSTimeZone timeZoneForSecondsFromGMT:0];
    NSString *fileName = [NSString stringWithFormat:@"locations_%@.json", [dateFormatter stringFromDate:[NSDate date]]];
    NSURL *jsonUrl = [NSURL fileURLWithPath:[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)[0] stringByAppendingPathComponent:fileName]];
    [jsonData writeToFile:jsonUrl.path atomically:NO];
    uint64_t bytesTotalForThisFile = [[[NSFileManager defaultManager] attributesOfItemAtPath:jsonUrl.path error:nil] fileSize];
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:url]];
    [request setHTTPMethod:@"POST"];
    [request setValue:[NSString stringWithFormat:@"%llu", bytesTotalForThisFile] forHTTPHeaderField:@"Content-Length"];
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    
    NSURLSessionTask *task = [urlSession uploadTaskWithRequest:request fromFile:jsonUrl];
    task.taskDescription = fileName;
    [tasks addObject:task];
    DDLogInfo(@"Started upload for %@ as task %zu/%@/%@", jsonUrl.lastPathComponent, (unsigned long)task.taskIdentifier, task.taskDescription, task);
    [task resume];
    
}

// http://stackoverflow.com/a/572623/48125
NSString *stringFromFileSize(unsigned long long theSize)
{
    double floatSize = theSize;
    if (theSize<1023)
        return([NSString stringWithFormat:@"%lli bytes",theSize]);
    floatSize = floatSize / 1024;
    if (floatSize<1023)
        return([NSString stringWithFormat:@"%1.1f KB",floatSize]);
    floatSize = floatSize / 1024;
    if (floatSize<1023)
        return([NSString stringWithFormat:@"%1.1f MB",floatSize]);
    floatSize = floatSize / 1024;
    
    return([NSString stringWithFormat:@"%1.1f GB",floatSize]);
}

- (NSString*)status
{
    int64_t sent = 0, toSend = 0;
    for(NSURLSessionUploadTask *task in tasks) {
        sent += task.countOfBytesSent;
        toSend += task.countOfBytesExpectedToSend;
    }
    return [NSString stringWithFormat:@"%@ being uploaded (%@ of %@)\nFiles on disk: %@",
            [tasks valueForKeyPath:@"taskDescription"],
            stringFromFileSize(sent),
            stringFromFileSize(toSend),
            
            [[NSFileManager defaultManager]
             contentsOfDirectoryAtPath:NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)[0]
             error:NULL]
            ];
}


#pragma mark -
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error
{
    NSLog(@"Finished uploading task %zu %@: %@ %@, HTTP %ld", (unsigned long)[task taskIdentifier], task.originalRequest.URL, error ?: @"Success", task.response, (long)[(id)task.response statusCode]);
    [tasks removeObject:task];
    NSURL *fullPath = [NSURL fileURLWithPath:[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)[0] stringByAppendingPathComponent:task.taskDescription]];
    [[NSFileManager defaultManager] removeItemAtURL:fullPath error:NULL];
    
    [self notify:[NSString stringWithFormat:@"taskfinish-%ld", (unsigned long)[task taskIdentifier]]];
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveData:(NSData *)data
{
    NSLog(@"Response:: %@", [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]);
}

- (void)URLSession:(NSURLSession *)session didBecomeInvalidWithError:(NSError *)error
{
    NSLog(@"sadface :( %@", error);
}

- (void)URLSessionDidFinishEventsForBackgroundURLSession:(NSURLSession *)session
{
    NSLog(@"finihed events for bg session");
    [self notify:@"sessionfinish"];
}

@end