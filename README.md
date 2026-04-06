# DoneLog

DoneLog 是一个基于 `uni-app x` 与 `UTS` 的本地事项与记账应用。

它把两类需求放在同一个应用里：

- 事件管理：记录事情上次完成时间、下一次何时该做、是否已终止
- 记账管理：记录每日收入支出，并按标签统计

当前项目不依赖后端，数据全部保存在本地存储中。

## 当前状态

截至 `2026-04-05`，仓库内以下改动已经落地到代码：

- 事件支持三种重复方式
  - `按间隔天数`
  - `指定下次完成日期`
  - `不重复（每天提醒，完成即终止）`
- 新建事件时可选择“今天已经完成过”
- 完成事件时分流处理
  - `间隔天数` 与 `不重复`：可直接完成
  - `指定下次完成日期`：进入完成页决定是否继续下一周期
- 日历页已支持
  - 左右滑动月份与顶部月份显示同步
  - 选中日期后展示当天事件状态
  - 选中日期后展示当天收支汇总
- 日历事件状态已调整为
  - `已完成`
  - `已终止`
  - `待完成`
- 记账页已支持
  - 新增记录时直接新增标签
  - 标签多选
  - 小屏下减少按钮和文本遮挡
- `tabBar` 顺序已调整为
  - `事件`
  - `记账`
  - `日历`
  - `我的`
- Android 桌面组件代码已接入工程
  - 组件显示事件列表
  - 组件显示当天收支概览
  - 点击组件项后可唤起 App 并进入完成流程

未验证项：

- Android 桌面组件尚未做真机编译验证
- 本仓库没有自动化测试，当前仅完成代码级检查

## 功能说明

### 1. 事件管理

支持创建、编辑、终止、删除事件。

事件支持以下重复方式：

1. `按间隔天数`
2. `指定下次完成日期`
3. `不重复`

#### 间隔天数语义

项目当前采用“中间空几天”的定义：

- `0` 表示明天需要再次完成
- `1` 表示后天需要再次完成
- `2` 表示大后天需要再次完成

这套语义已经通过迁移逻辑兼容旧数据，避免已有事件整体顺延一天。

#### 不重复语义

`不重复` 事件没有具体的下一次日期。

行为如下：

- 在未完成前，每天都会显示为待处理
- 完成后会自动终止
- 终止后不再作为进行中事件展示

#### 创建时已完成

新增事件时可勾选“今天已经完成过”：

- 保存后会自动写入一条当天完成记录
- 如果事件是 `不重复`，会直接终止

### 2. 完成事件流程

当前完成逻辑分为两类：

- `按间隔天数`、`不重复`
  - 直接完成
- `指定下次完成日期`
  - 进入 `/pages/event/complete`
  - 可以选择终止，或直接安排下一周期

### 3. 日历页

日历页当前提供：

- 月份切换与滑动浏览
- 查看某一天的相关事件
- 查看某一天的收入、支出、记录数

当前展示规则：

- 事件标签不在日历卡片中展示
- 某天完成过，显示 `已完成`
- 某天是终止日，显示 `已终止`
- 未来日期不显示 `进行中`，统一显示 `待完成`
- 已终止事件只在终止当天显示终止状态；更早的完成日期仍显示 `已完成`

### 4. 记账管理

记账页当前支持：

- 新增、编辑、删除记录
- 区分 `支出` / `收入`
- 标签多选
- 在新增记录弹窗内直接新增记账标签
- 月度概览统计

### 5. 我的页

“我的”页当前提供：

- 按周 / 按月查看事件完成统计
- 按周 / 按月查看记账统计
- 事件标签管理入口
- 记账标签管理入口
- 桌面组件预览卡片

### 6. Android 桌面组件

仓库内已新增 Android UTS 插件：`uni_modules/donelogWidget`

当前组件能力：

- 展示若干条待处理事件
- 展示当天收支概览
- 点击组件事件项后唤起 App
- App 启动后消费组件动作并执行完成逻辑

当前限制：

- 组件交互仍依赖唤起 App 处理，不是纯原生静默写回
- 尚未完成真机编译验证

## 技术栈

- 框架：`uni-app x`
- 语言：`UTS`
- 页面：`.uvue`
- 存储：`uni.setStorageSync` / `uni.getStorageSync`
- 平台重点：Android App

## 项目结构

```text
DoneLog/
├─ pages/
│  ├─ index/
│  │  └─ index.uvue              # 事件首页
│  ├─ event/
│  │  ├─ add.uvue                # 新增事件
│  │  ├─ edit.uvue               # 编辑事件
│  │  ├─ detail.uvue             # 事件详情
│  │  └─ complete.uvue           # 固定日期事件完成后的下一周期处理
│  ├─ account/
│  │  └─ index.uvue              # 记账页
│  ├─ calendar/
│  │  └─ index.uvue              # 日历页
│  ├─ my/
│  │  ├─ index.uvue              # 统计与管理页
│  │  └─ labels.uvue             # 标签管理页
│  └─ label/
│     └─ index.uvue              # 旧标签管理页/兼容页面
├─ utils/
│  ├─ storage.uts                # 数据模型与本地存储
│  ├─ eventManager.uts           # 事件与完成记录管理
│  ├─ accountManager.uts         # 记账管理
│  └─ labelManager.uts           # 标签管理
├─ uni_modules/
│  └─ donelogWidget/
│     └─ utssdk/
│        └─ app-android/         # Android 桌面组件实现
├─ static/                       # 图标与静态资源
├─ App.uvue                      # 应用入口，负责同步组件数据
├─ main.uts                      # 应用入口脚本
├─ pages.json                    # 页面与 tabBar 配置
├─ manifest.json                 # 应用配置
└─ README.md
```

## 页面概览

### 事件首页 `/pages/index/index`

- 搜索事件
- 按标签筛选
- 按状态筛选
- 快速完成事件
- 进入详情页

### 新增事件 `/pages/event/add`

- 事件名称
- 多选标签
- 选择重复方式
- 设置间隔天数或指定日期
- 选择“今天已经完成过”
- 可直接新增标签

### 编辑事件 `/pages/event/edit`

- 修改名称、标签、备注、重复方式
- 终止事件

### 事件详情 `/pages/event/detail`

- 查看当前状态
- 查看历史完成记录
- 快速完成
- 跳转编辑

### 完成事件 `/pages/event/complete`

仅用于 `指定下次完成日期` 事件的后续处理：

- 完成并终止
- 完成并继续安排下一周期
- 可改为指定日期或间隔天数

### 记账页 `/pages/account/index`

- 查看月度收支概览
- 新增 / 编辑 / 删除记录
- 标签多选
- 在弹窗内新增标签

### 日历页 `/pages/calendar/index`

- 切换月份
- 查看指定日期事件
- 查看指定日期收支

### 我的页 `/pages/my/index`

- 周 / 月统计切换
- 事件统计
- 记账统计
- 标签管理入口
- 小组件预览

## 数据模型

### Event

```ts
type Event = {
  id: number
  name: string
  labelIdList: string
  intervalDays: number
  nextDueDate: number
  remark: string
  isTerminated: boolean
  terminatedAt: number
  createTime: number
  updateTime: number
}
```

字段说明：

- `labelIdList`：逗号分隔的标签 ID 列表
- `intervalDays`：
  - `>= 0` 表示按间隔天数
  - `< 0` 当前用于表示 `不重复`
- `nextDueDate`：固定日期事件的目标时间戳；为 `0` 表示未使用固定日期模式
- `terminatedAt`：终止时间，用于日历中判断终止展示日

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
  remark: string
  occurredAt: number
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

### AppMeta

```ts
type AppMeta = {
  intervalSemanticsVersion: number
}
```

## 本地存储键

当前使用以下存储键：

- `donelog_events`
- `donelog_completion_records`
- `donelog_event_labels`
- `donelog_account_labels`
- `donelog_account_entries`
- `donelog_app_meta`

兼容处理：

- 老版本 `donelog_labels` 会迁移到事件标签存储
- 老版本记账记录的单标签字段会迁移到 `labelIdList`

## 运行方式

### 环境要求

- `HBuilderX`
- 支持 `uni-app x` / `UTS` 的运行环境
- 如需验证桌面组件，需要 Android 编译环境

### 本地运行

1. 用 `HBuilderX` 打开项目目录
2. 等待 IDE 完成项目索引
3. 选择运行到 Android 模拟器或真机
4. 如需验证桌面组件，编译 Android App 后手动添加到桌面

项目当前没有 `package.json`，通常不需要执行额外依赖安装。

## 业务规则

- 同一个事件同一天只能完成一次
- `间隔天数` 使用“中间空几天”的语义
- `指定下次完成日期` 完成后不自动推导下一次，需用户决定后续安排
- `不重复` 事件在完成后直接终止
- 删除事件时会同时删除其完成记录
- 删除标签时会从事件或记账记录中移除对应标签 ID

## 已知限制

- 当前没有自动化测试
- README 反映的是仓库代码现状，不代表所有平台都已验证通过
- Android 桌面组件已接入代码，但未完成真机编译验收
- 当前数据完全保存在本地，卸载应用或清缓存后会丢失
- 当前不支持云同步、系统通知、数据导出导入

## 建议的下一步

1. 在 HBuilderX 上编译 Android 包并验证 `uni_modules/donelogWidget`
2. 补一轮真机 UI 回归，重点看小屏、日历状态和记账弹窗
3. 为 `eventManager` / `accountManager` 补最基本的行为测试

## License

MIT
