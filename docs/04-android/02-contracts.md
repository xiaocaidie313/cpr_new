# 02 · 对接契约（与第 2 / 3 部分）

> 这是协作最重要的文档。第 2、3 部分只要实现下面的接口、产出下面的数据模型，
> 即可无缝接入 Android 终端。**Android 端不关心你们内部怎么实现。**

所有契约位于 `core/contract/`。

---

## A. 第 3 部分（感知）→ 第 4 部分

### 数据模型：`PerceptionEvent`

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `timestampMs` | `Long` | 事件时间戳 |
| `handPosition` | `HandPosition` | 手位：CORRECT/TOO_HIGH/TOO_LOW/OFF_CENTER/UNKNOWN |
| `compressionRate` | `Float?` | 按压频率（次/分），无法判定填 null |
| `compressionDepthScore` | `Float?` | 深度充分性 0~1 |
| `isInterrupted` | `Boolean` | 是否检测到中断 |
| `confidence` | `Float` | 整体置信度 0~1 |
| `rawSignals` | `Map<String,Float>` | 可扩展原始信号（关键点等） |

> 低于 `MIN_RELIABLE_CONFIDENCE`（默认 0.5）的事件，Android 端会做 UI 兜底，且不喂给 Agent。

### 接口：`CprPerceptionSource`

```kotlin
interface CprPerceptionSource {
    val events: Flow<PerceptionEvent>   // 冷流，订阅后开始产出
    fun start()                         // 幂等
    fun stop()
    val isReady: Boolean                // 模型是否加载完成
}
```

### 可选：`FrameSink`（由 Android 统一管摄像头时）

若希望由第 4 部分用 CameraX 取流再喂给算法，实现 `FrameSink.onFrame(frame, meta)`，
帧格式通过 `FrameMeta.format` 约定（如 `NV21`）。否则感知源可自管采集。

---

## B. 第 2 部分（Agent）→ 第 4 部分

### 数据模型：`GuidanceAction`

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `id` | `String` | 动作唯一标识（用于去抖/日志） |
| `priority` | `GuidancePriority` | INFO/WARNING/CRITICAL（CRITICAL 打断 TTS） |
| `phase` | `CprPhase` | 当前阶段，驱动 UI 主视图 |
| `spokenText` | `String` | TTS 文本（空=不播报） |
| `displayText` | `String` | 屏幕大字 |
| `haptic` | `HapticPattern?` | 震动模式 |
| `targetRate` | `Int?` | 节拍器目标频率 |
| `metadata` | `Map<String,String>` | 可扩展透传 |

### 接口：`GuidanceAgent`

```kotlin
interface GuidanceAgent {
    suspend fun onSessionStart(): GuidanceAction
    fun guidance(perception: Flow<PerceptionEvent>): Flow<GuidanceAction>
    suspend fun buildHandover(log: SessionLog): HandoverReport
    val isReady: Boolean
}
```

- `guidance()` 入参是感知流、出参是动作流，天然支持流式与取消。
- 建议在实现内部做**去抖**（相同提示不重复），避免 TTS 拥塞（Mock 已示范）。

---

## C. 会话日志与交接报告

- 第 4 部分在会话中累积 `SessionLogEntry`，结束时打包成不可变 `SessionLog`（含聚合 `metrics`）。
- 调用 `agent.buildHandover(log)` 得到 `HandoverReport` 并展示。
- `HandoverReport.location` 用轻量 `GeoSnapshot`，避免算法端依赖 Android `Location`。

---

## D. 接入步骤（第 2/3 部分照做）

1. 新建实现类，分别实现 `CprPerceptionSource` / `GuidanceAgent`。
2. 在 App 启动处（或 `MainActivity`）替换工厂：

```kotlin
ServiceLocator.perceptionFactory = { ctx -> RealPerceptionSource(ctx) }
ServiceLocator.agentFactory      = { ctx -> GemmaGuidanceAgent(ctx) }
```

3. 完成。UI 与编排代码无需任何改动。

> 调试建议：可先只替换其中一个（如先接真实 Agent，感知仍用 Mock），逐步联调。
