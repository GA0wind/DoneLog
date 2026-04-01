# DoneLog

DoneLog 是一个基于 `uni-app x` 与 `UTS` 开发的轻量级日常事件记录应用，用来追踪“上次什么时候做过”和“下次什么时候该做”。

项目适合管理这类重复性事项：

- 健身、拉伸、喂猫、浇花
- 学习复盘、资料整理、账单检查
- 设备保养、周期提醒、生活习惯打卡

应用当前采用本地存储方案，不依赖后端服务，打开即可使用。

## 功能特性

### 事件管理

- 新增事件，支持填写名称、备注、标签
- 支持两种计划方式
  - 按间隔天数重复
  - 指定下次完成日期
- 编辑已有事件
- 标记事件完成，自动生成完成记录
- 终止事件，终止后不再默认显示在进行中列表

### 列表与筛选

- 首页展示事件卡片列表
- 支持关键词搜索
- 支持按标签筛选
- 支持按状态筛选
  - 全部
  - 进行中
  - 已终止
- 自动计算“今天 / N 天后 / 已过期 N 天”等状态文案

### 标签管理

- 新增标签
- 编辑标签名称与颜色
- 删除标签
- 创建或编辑事件时可多选标签

### 日历视图

- 提供月历浏览
- 按日期查看当天相关事件
- 同时展示未来计划项与历史完成记录

### 历史记录

- 事件详情页可查看完成记录时间线
- 支持回看某个事件的历史完成时间

## 技术栈

- 框架：`uni-app x`
- 语言：`UTS`
- UI：原生页面与基础组件
- 数据存储：`uni.setStorageSync` / `uni.getStorageSync`
- Vue 版本：`Vue 3`

## 项目结构

```text
DoneLog/
├─ pages/
│  ├─ index/
│  │  └─ index.uvue         # 首页，事件列表、搜索与筛选
│  ├─ event/
│  │  ├─ add.uvue           # 新增事件
│  │  ├─ edit.uvue          # 编辑事件
│  │  └─ detail.uvue        # 事件详情与完成记录
│  ├─ label/
│  │  └─ index.uvue         # 标签管理
│  └─ calendar/
│     └─ index.uvue         # 日历视图
├─ utils/
│  ├─ storage.uts           # 本地存储与数据类型定义
│  ├─ eventManager.uts      # 事件与完成记录管理
│  └─ labelManager.uts      # 标签管理
├─ static/                  # 静态资源
├─ App.uvue                 # 应用入口组件
├─ main.uts                 # 应用入口脚本
├─ pages.json               # 页面路由与 tabBar 配置
├─ manifest.json            # 应用基础配置
└─ uni.scss                 # 全局样式变量
```

## 页面说明

### 首页

- 查看事件列表
- 搜索事件名称
- 根据标签和状态筛选
- 快速标记完成
- 点击卡片进入详情页

### 新增 / 编辑事件

- 输入事件名称
- 选择一个或多个标签
- 选择重复规则
- 填写备注
- 保存修改或终止事件

### 事件详情

- 查看事件基础信息
- 查看当前状态
- 查看历史完成记录
- 继续编辑或直接标记完成

### 标签管理

- 维护标签名称与颜色
- 为事件提供分类能力

### 日历页

- 按月份浏览日期
- 查看指定日期相关事件
- 辅助回顾历史与安排未来计划

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
  createTime: number
  updateTime: number
}
```

字段说明：

- `labelIdList`：以逗号分隔的标签 ID 字符串
- `intervalDays`：按间隔重复时使用
- `nextDueDate`：按指定日期重复时使用，时间戳格式
- `isTerminated`：事件是否已终止

### CompletionRecord

```ts
type CompletionRecord = {
  id: number
  eventId: number
  lastCompleteDate: number
  createTime: number
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

## 存储设计

项目当前将所有数据保存在本地缓存中，使用的存储键如下：

- `donelog_events`
- `donelog_completion_records`
- `donelog_labels`

这意味着：

- 不需要服务端即可运行
- 卸载应用、清理缓存后数据会丢失
- 当前不支持多端同步

## 运行方式

### 环境要求

- `HBuilderX`
- 支持 `uni-app x` 的开发环境

### 本地运行

1. 使用 `HBuilderX` 打开项目根目录
2. 等待 IDE 识别 `uni-app x` 项目
3. 选择运行到模拟器、真机或对应平台

由于当前项目没有 `package.json`，也没有额外 npm 依赖，通常不需要执行额外安装命令。

## 业务规则说明

- 一个事件同一天只能标记完成一次
- 若使用“间隔天数”模式，下次应完成时间会基于最近一次完成记录计算
- 若使用“指定日期”模式，则直接以设置的日期作为下次计划时间
- 已终止事件不会在默认进行中列表中展示
- 删除事件时，会一并删除对应的完成记录

## 当前实现特点

- 纯前端本地应用，部署成本低
- 结构简单，便于继续扩展
- 页面职责清晰，适合继续补充导出、提醒、同步等能力

## 可继续优化的方向

- 增加数据导入 / 导出
- 增加云端同步
- 接入本地通知提醒
- 完善标签排序能力
- 增加统计分析页
- 补充单元测试与页面截图

## 注意事项

- 当前 README 基于仓库现状整理，实际页面文案中仍有部分文件存在编码异常，后续可单独清理
- `manifest.json` 中应用描述等信息仍比较基础，可按发布需求继续完善
- 若计划发布到小程序或 App，建议补齐图标、启动图和平台参数

## License

MIT
