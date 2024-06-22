/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <React/RCTViewManager.h>

@interface RNCWebViewManager : RCTViewManager
@property (nonatomic, copy) NSArray<NSDictionary *> * _Nullable menuItems;
@property (nonatomic, copy) RCTDirectEventBlock onCustomMenuSelection;

/// 设置使用X5内核
+ (void)useX5;

/// 是否使用腾讯X5配合(获取之后每次都会重置为false)
+ (BOOL)isUseX5;

@end
