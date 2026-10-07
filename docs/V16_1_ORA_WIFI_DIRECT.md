# v16.1：好猫强制全屏版的 Android 8.1 Wi-Fi Direct 适配

基于好猫 v16，移植通用 v15 的旧系统 Wi-Fi Direct 修复，保留欧拉图标、强制全屏及既有音频行为。
最低 Android 8.1（API 27），Intel x86 / x86_64；包名 `com.shihab.diplay.ora81`、签名不变。
versionCode **46**，版本 `0.2.10-ora-android81-test16.1-wifi-direct`，可以覆盖 v16 / v12 并保留设置。

## 兼容内容

- Android 8.1 / 9 的两个连接设置入口均提供 Wi-Fi Direct，选择后保存，进程重启不会重置为车载热点。
- 控制器使用真正的 Wi-Fi Direct 后端，不再把旧系统的请求转换成 LocalOnlyHotspot。
- API 27 / 28 使用 `createGroup(Channel, ActionListener)`，读取实际组的 SSID、密码和网络接口。
- 旧系统不调用 Android 10 才有的配置 Builder、三参数 createGroup、requestP2pState 或 getFrequency。
- IPv4 地址可由系统连接信息获取；没有可用地址或凭据时不会提交不完整的连接信息。
- 无法读取信道时保留未知（信道 0），不把普通 Wi-Fi 的频率当成直连信道，不会因缺少频率一直等待或在 CarPlay 确认时出错。
- 系统 BUSY 有界重试一次；不支持、缺少权限或创建回调超时不会重复发起盲目创建。关闭时取消等待，只清理确认属于本应用的活动组。
- Android 10 及以上保留原来的频率策略；不把 Wi-Fi Direct 固定写成 5 GHz。
- v16 强制全屏清单与实现未变；保留音频焦点、音乐/导航音道、Siri 导航音道、方向盘切歌和开机热点等待。

此版只增加 Wi-Fi Direct 兼容，不包含通用 v14/v15 的同一局域网模式，亦不更换欧拉品牌或 Intel 架构配置。

## 使用方法

1. 下载 `Diplay-ORA-Android8.1-x86-v16.1-wifi-direct.apk`，直接覆盖安装。
2. 打开车机 Wi-Fi，在 DiPlay → 设置 → 连接设置中选择 **Wi-Fi Direct**。
3. 允许 DiPlay 使用位置权限；若固件要求，开启系统位置服务。保留手机蓝牙和 Wi-Fi，再连接 CarPlay。
4. 切换后断开重连使模式生效。原来的车载热点名称和密码不会被覆盖，仍可切回车载热点。

Android 8.1 / 9 的公开接口由系统分配网络名称、密码和频段，**不能指定或保证 5 GHz**。
旧系统可能保留 Wi-Fi Direct 组配置，关闭时只移除活动组。
不强行更改车机热点开关；若固件不允许热点与 Wi-Fi Direct 同时运行，可在车机设置中关闭普通热点后再连接。
如果车机驱动没有 Wi-Fi Direct 能力，APK 无法补出该硬件能力，可继续使用车载热点。

## 验证范围

API 27 / 28 自动回归覆盖旧接口、系统凭据、IPv4、未知信道、权限、BUSY、超时、取消、组归属和控制器模式。
Android 8.1 宽屏模拟器验证覆盖 v16、原设置保留、Wi-Fi Direct 选择与重启保存，以及全屏行为。
构建、测试数量、lint、签名和原生库记录见发布附件 `VALIDATION-v16.1.zip`。
**模拟器不代表哈曼无线驱动。本次按 Android 8.1 API 完成兼容适配，但真实好猫与 iPhone 直连、原车侧栏收起仍需实车确认，因此作为 Pre-release 发布。**

接口依据：[WifiP2pManager](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager)、
[WifiP2pGroup](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pGroup)。
保留原 GPL/AGPL 与素材许可；运行资源和签名沿用 v16，源码不含认证资产、签名私钥或原厂 APK。
