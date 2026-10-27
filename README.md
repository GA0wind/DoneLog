# 做完记

做完记是一个基于 `uni-app x` 和 `UTS` 开发的本地生活记录 App。它把「事件完成记录」和「日常记账」放在一起：一边提醒你该做什么，一边记录每天的钱花到哪里。

项目目前以 Android 为主要验证平台，数据默认保存在本地，没有后端服务和账号体系。

## 功能概览

### 事件管理

- 新增、编辑、复制、终止、删除事件
- 支持事件标签，多标签筛选
- 支持完成历史记录
- 支持三种事件周期
  - 按间隔周期：按天、按月、按年推进
  - 指定下次完成日期：完成后由用户决定是否安排下一次
  - 不重复：每天展示为待处理，完成后终止
- 新建事件时可选择「今天已经完成过」
- 事件详情页展示当前状态、重复方式、下次完成时间、提醒时间和完成记录
- 首页可快速完成，也可进入番茄钟开始专注

### 消息通知

- 每个事件可单独开启提醒通知
- 支持选择每天提醒时间
- 不重复事件开启提醒后：每天在指定时间提醒，直到完成并终止
- 重复事件开启提醒后：从应完成日开始每天在指定时间提醒，直到本次完成
- 完成后如果继续安排下一次，会重新同步下一次提醒
- Android 端使用 `AlarmManager` 和系统通知实现
- Android 13 及以上会请求 `POST_NOTIFICATIONS` 通知权限
- 通知点击后会唤起 App 并打开事件详情页

### 日历

- 月份切换和日期选择
- 展示选中日期的事件状态
- 展示选中日期的收入、支出和记账条数
- 待完成、已完成、已终止状态分开展示
- 兼容没有完成记录的待完成事件，避免空白页

### 记账

- 新增、编辑、删除收入/支出记录
- 每条记账记录选择一个标签
- 支持支付方式，默认包含支付宝、微信、银行卡
- 支持新增记录时直接创建标签
- 支持自动续费：每月指定日期生成固定金额支出
- 支持月度收入、支出和记录数概览
- 支持月预算与标签预算
- 预算超额会在页面中提示

### 统计与我的

- 事件完成统计：按周、按月查看标签完成数量
- 记账统计：按周、按月查看标签金额
- 分类图表页：按支出/收入查看标签占比
- 标签管理：事件标签和记账标签分开管理
- 支付方式管理：新增、编辑、删除并设置默认支付方式
- 支持重置默认标签

### 番茄钟

- 可从事件详情页进入番茄钟
- 支持关联事件
- 关联事件完成后会写入完成记录
- 不重复事件完成后自动终止

### Android 桌面组件

- 原生 AppWidget 位于 `uni_modules/donelogWidget`
- 桌面组件展示今日待处理事件
- 展示当日收支概览
- 点击组件事件项后唤起 App 并进入完成流程
- App 打开或回到前台时会刷新组件快照

## 技术栈

- 应用框架：`uni-app x`
- 页面语言：`.uvue`
- 逻辑语言：`UTS`
- 前端模式：Vue 3 setup 风格
- 本地存储：`uni.getStorageSync` / `uni.setStorageSync`
- Android 原生能力：UTS Android 插件
- 桌面组件：Android `AppWidgetProvider` + `RemoteViews`
- 本地提醒：Android `AlarmManager` + `BroadcastReceiver` + `Notification`
- 构建方式：HBuilderX CLI 导出资源 + Gradle 本地打包

## 项目结构

```text
DoneLog/
├─ App.uvue
├─ main.uts
├─ manifest.json
├─ pages.json
├─ build-local.ps1
├─ README.md
├─ static/
│  ├─ logo.png
│  ├─ app-icon.png
│  ├─ icons/android/
│  │  ├─ icon-hdpi.png
│  │  ├─ icon-xhdpi.png
│  │  ├─ icon-xxhdpi.png
│  │  └─ icon-xxxhdpi.png
│  └─ tab-*.png
├─ pages/
│  ├─ index/index.uvue
│  ├─ event/
│  │  ├─ add.uvue
│  │  ├─ edit.uvue
│  │  ├─ detail.uvue
│  │  └─ complete.uvue
│  ├─ account/index.uvue
│  ├─ calendar/index.uvue
│  ├─ pomodoro/index.uvue
│  ├─ my/
│  │  ├─ index.uvue
│  │  ├─ labels.uvue
│  │  └─ account-chart.uvue
│  └─ label/index.uvue
├─ utils/
│  ├─ storage.uts
│  ├─ eventManager.uts
│  ├─ accountManager.uts
│  └─ labelManager.uts
├─ uni_modules/
│  └─ donelogWidget/
│     └─ utssdk/
│        ├─ index.uts
│        └─ app-android/
│           ├─ index.uts
│           ├─ AndroidManifest.xml
│           └─ res/
└─ local-native-sdk/
   └─ uniappxnativepackage/
```

## 关键文件说明

| 文件 | 作用 |
| --- | --- |
| `App.uvue` | 初始化 ID、同步桌面组件数据、消费桌面组件/通知点击动作 |
| `manifest.json` | 应用名称、appid、发行图标、平台配置 |
| `pages.json` | 页面路由、导航栏、tabBar |
| `utils/storage.uts` | 数据类型定义、本地存储读写、旧数据兼容 |
| `utils/eventManager.uts` | 事件排序、周期计算、完成记录、提醒计划、组件快照 |
| `utils/accountManager.uts` | 记账记录、支付方式、自动续费、收支统计、预算统计、组件收支数据 |
| `utils/labelManager.uts` | 事件/记账标签的增删改查、排序、合并、重置 |
| `uni_modules/donelogWidget/utssdk/index.uts` | UTS 插件跨端导出入口 |
| `uni_modules/donelogWidget/utssdk/app-android/index.uts` | Android 桌面组件、通知权限、提醒闹钟和通知点击桥接 |
| `build-local.ps1` | 本地导出 uni-app x 资源并打包 Android debug APK |

## 页面说明

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 事件首页 | `pages/index/index` | 事件列表、标签筛选、状态筛选、快速完成 |
| 新增事件 | `pages/event/add` | 创建事件、选择周期、设置提醒、创建时完成 |
| 编辑事件 | `pages/event/edit` | 修改事件基础信息、周期、提醒、终止事件 |
| 事件详情 | `pages/event/detail` | 查看状态、历史记录、复制、编辑、开始番茄钟、完成 |
| 完成事件 | `pages/event/complete` | 固定日期事件完成后选择终止或继续安排 |
| 记账 | `pages/account/index` | 收支列表、记账弹窗、支付方式选择、自动续费、预算设置、标签预算 |
| 日历 | `pages/calendar/index` | 按日期查看事件和收支 |
| 我的 | `pages/my/index` | 周/月事件统计、数据备份与维护 |
| 标签管理 | `pages/my/labels` | 管理事件标签和记账标签 |
| 分类图表 | `pages/my/account-chart` | 记账标签占比图表 |
| 番茄钟 | `pages/pomodoro/index` | 专注计时和关联事件完成 |

## 数据模型

### Event

```ts
type Event = {
  id: number
  name: string
  labelIdList: string
  intervalDays: number
  repeatUnit: 'day' | 'month' | 'year'
  nextDueDate: number
  remark: string
  isTerminated: boolean
  terminatedAt: number
  notifyEnabled: boolean
  notifyTime: string
  createTime: number
  updateTime: number
}
```

字段要点：

- `labelIdList`：逗号分隔的标签 ID
- `intervalDays >= 0`：按间隔周期
- `intervalDays < 0`：不重复事件
- `repeatUnit`：间隔单位，支持天、月、年
- `nextDueDate > 0`：指定下次完成日期
- `notifyEnabled`：是否开启提醒
- `notifyTime`：提醒时间，格式为 `HH:mm`
- `terminatedAt`：终止时间，用于日历展示终止状态

### CompletionRecord

```ts
type CompletionRecord = {
  id: number
  eventId: number
  lastCompleteDate: number
  createTime: number
}
```

### AccountEntry

```ts
type AccountEntry = {
  id: number
  type: 'expense' | 'income'
  amount: number
  labelIdList: string
  paymentMethodId: number
  autoRenewalId: number
  remark: string
  occurredAt: number
  createTime: number
  updateTime: number
}
```

### PaymentMethod

```ts
type PaymentMethod = {
  id: number
  name: string
  isDefault: boolean
  sortOrder: number
  createTime: number
  updateTime: number
}
```

### AutoRenewal

```ts
type AutoRenewal = {
  id: number
  name: string
  amount: number
  paymentMethodId: number
  labelIdList: string
  chargeDay: number
  nextChargeDate: number
  isEnabled: boolean
  remark: string
  createTime: number
  updateTime: number
}
```

### Label

```ts
type Label = {
  id: number
  name: string
  color: string
  sortOrder: number
}
```

### BudgetSettings

```ts
type BudgetSettings = {
  monthlyExpenseBudget: number
  labelMonthlyBudgets: {
    labelId: number
    amount: number
  }[]
  updateTime: number
}
```

## 本地存储

项目当前使用以下 key：

| Key | 内容 |
| --- | --- |
| `donelog_events` | 事件列表 |
| `donelog_completion_records` | 完成记录 |
| `donelog_event_labels` | 事件标签 |
| `donelog_account_labels` | 记账标签 |
| `donelog_account_entries` | 记账记录 |
| `donelog_payment_methods` | 支付方式 |
| `donelog_auto_renewals` | 自动续费项目 |
| `donelog_budget_settings` | 预算设置 |
| `donelog_app_meta` | 应用迁移元信息 |

兼容逻辑：

- 老版本事件缺少字段时会补默认值
- 老版本 `donelog_labels` 会迁移为事件标签
- 老版本记账单标签字段会迁移为 `labelIdList`
- 老版本记账记录缺少支付方式时会补为当前默认支付方式
- 周期语义通过 `intervalSemanticsVersion` 控制迁移

## 业务规则

- 同一个事件同一天只能完成一次
- 按天间隔使用「中间空几天」语义：`0` 表示明天，`1` 表示后天
- 按月/按年间隔使用自然月、自然年推进
- 指定日期事件完成后，需要用户决定终止或安排下一次
- 不重复事件每天视为待完成，完成后自动终止
- 删除事件时同步删除对应完成记录
- 删除/合并标签时会同步更新事件或记账记录引用
- 删除/合并记账标签时会同步更新自动续费标签引用
- 自动续费以月为周期，到达下次扣款日期后补记支出，并推进到下个月同日
- 删除支付方式时，相关账单和自动续费会改用剩余默认支付方式
- 开启提醒的事件会在新增、编辑、完成、终止、删除后同步系统提醒计划

## Android 桌面组件

桌面组件入口：

- `uni_modules/donelogWidget/utssdk/app-android/AndroidManifest.xml`
- `uni_modules/donelogWidget/utssdk/app-android/index.uts`
- `uni_modules/donelogWidget/utssdk/app-android/res/layout/donelog_widget.xml`
- `uni_modules/donelogWidget/utssdk/app-android/res/xml/donelog_widget_info.xml`

交互流程：

1. App 启动或回到前台时生成组件快照
2. 快照写入 Android `SharedPreferences`
3. AppWidget 使用 `RemoteViews` 渲染事件和收支
4. 点击组件事件后进入 `WidgetActionBridgeActivity`
5. Bridge Activity 写入待处理动作并唤起 App
6. `App.uvue` 读取动作并进入完成流程

## Android 提醒通知

提醒入口也放在 `donelogWidget` 模块里。

核心流程：

1. 页面保存事件提醒配置
2. `EventManager.syncEventNotifications()` 计算所有有效提醒
3. Android 端取消旧闹钟并重新设置 `AlarmManager`
4. 到点后 `EventNotificationReceiver` 发系统通知
5. 通知点击后打开事件详情
6. 每次通知触发后，如果事件仍未完成，会安排下一天同一时间

相关权限：

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.RECEIVE_BOOT_COMPLETED`

## 图标与名称

- 应用中文名：`做完记`
- 主图标：`static/logo.png`
- 图标源预览：`static/app-icon.png`
- Android 发行图标：`static/icons/android/*.png`
- Android adaptive icon：`local-native-sdk/uniappxnativepackage/app/src/main/res/drawable*/ic_launcher_*`

图标设计含义：

- 日历页：表示周期和日期
- 记录页：表示完成日志
- 勾选：表示完成
- 青绿色主色：延续应用当前界面风格

## 构建

### 环境要求

- HBuilderX 5.x，需支持 `uni-app x`
- JDK 21，本项目脚本默认路径为 `D:\jdk-21_windows-x64_bin\jdk-21.0.2`
- Android SDK，本项目脚本默认路径为 `D:\Android\Sdk`
- Gradle 8.14.3，本项目脚本默认路径为 `D:\Android\gradle\gradle-8.14.3\bin\gradle.bat`

### 完整打包

```powershell
.\build-local.ps1
```

脚本会执行：

1. 调用 HBuilderX CLI 导出 Android 资源
2. 同步导出的 assets 和 Kotlin 源码到本地原生工程
3. 调用 Gradle 构建 debug APK

输出路径：

```text
local-native-sdk/uniappxnativepackage/app/build/outputs/apk/debug/app-debug.apk
```

### 仅打包原生工程

```powershell
.\build-local.ps1 -BuildOnly
```

仅适合没有改 `.uvue`、`.uts`、`manifest.json`、`pages.json` 等 uni-app x 源码时使用。

## 真机调试

### 安装 APK

```powershell
adb install -r local-native-sdk/uniappxnativepackage/app/build/outputs/apk/debug/app-debug.apk
```

### 抓取白屏或崩溃日志

```powershell
adb logcat -c
adb shell am force-stop com.example.donelog
adb shell monkey -p com.example.donelog 1
adb logcat -d -v time > donelog-logcat.txt
```

重点搜索：

```text
FATAL EXCEPTION
AndroidRuntime
UTS
__UNI__7A2B275
com.example.donelog
```

### 提醒不响时检查

- 系统设置里允许「做完记」发送通知
- Android 13 及以上首次开启事件提醒时同意通知权限
- 允许应用后台运行或自启动
- 如系统限制精确提醒，允许「闹钟和提醒」或「精确闹钟」权限
- 省电模式可能导致提醒延迟

### 桌面组件添加

荣耀 MagicOS 常见路径：

```text
桌面双指捏合 > 桌面卡片 > 经典小工具 > 做完记
```

如果看不到组件：

- 先打开一次 App
- 确认安装的是新 APK
- 回到桌面重新进入小工具列表
- 检查系统是否隐藏了小工具入口

## 已验证

- `.\build-local.ps1` 完整构建通过
- Android debug APK 已生成
- 待完成事件无完成记录时，日历页和事件详情页不再直接访问空数组
- `leakcanary-android` 依赖已移除

## 已知限制

- 当前没有自动化测试
- 当前没有云同步
- 当前没有账号体系
- 当前提醒为本地提醒，卸载 App 或清除数据后提醒配置会丢失
- Android 厂商省电策略可能让通知出现延迟
- 桌面组件点击后仍需要唤起 App 完成业务动作

## 后续建议

- 给 `eventManager` 增加周期计算单元测试
- 增加备份导入导出的用户入口
- 增加提醒权限状态提示页
- 将 Android 包名、签名和发布配置从示例值改为正式发布配置

## License

MIT
