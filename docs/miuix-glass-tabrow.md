# Miuix Glass TabRow 样式与适配说明

本文档整理了 `miuix-glass` 的三种 `TabRow` 视觉样式、上游等宽实现的局限及崩溃原因，以及本项目中的自适应封装 [`AdaptiveTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt) 切换方式。

## 三种 `miuix-glass` TabRow 样式对比

| `AdaptiveTabRowStyle` | 对应组件 / 配色 | 默认高度 | 默认间距与内边距 | 适用场景 |
| --- | --- | --- | --- | --- |
| `Segmented`（当前默认） | [`AdaptiveGlassSegmentedTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt#L354)（对照 `GlassSegmentedTabRow` + `GlassSegmentedTabRowDefaults`） | `40.dp` | 轨道内边距 `3.dp`，标签间距 `8.dp`，标签左右内边距 `16.dp`；共享磨砂胶囊轨道配滑动高亮指示块 | 当前在 `AppDetailScreen` 和 `SnapshotScreen` 中统一使用，兼具整体胶囊玻璃质感与文本自适应宽度 |
| `Primary` | [`AdaptiveGlassTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt#L181) + `GlassTabRowDefaults.primaryColors()` | `40.dp` | 标签间距 `12.dp`，标签内边距水平 `16.dp`、垂直 `6.dp`；独立玻璃胶囊配主题主色选中态 | 强调主色选中态的独立分组标签 |
| `Neutral` | [`AdaptiveGlassTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt#L181) + `GlassTabRowDefaults.neutralColors()` | `35.dp` | 标签间距 `12.dp`，标签内边距水平 `16.dp`、垂直 `6.dp`；更紧凑的中性独立胶囊 | 需要更紧凑高度（`35.dp`）的次级筛选标签（如 ABI 次级标签） |

样式默认值参考自 `miuix-glass` 源码（`GlassTabRowDefaults` 与 `GlassSegmentedTabRowDefaults`）：

- **Primary / Neutral（`GlassTabRowDefaults`）**
  - `Height = 40.dp`、`NeutralHeight = 35.dp`
  - `TabGap = 12.dp`、`TabPaddingHorizontal = 16.dp`、`TabPaddingVertical = 6.dp`
  - 圆角形状：`GlassShape(height / 2, smoothing = 0f)`
- **Segmented（`GlassSegmentedTabRowDefaults`）**
  - `Height = 40.dp`、`TrackPadding = 3.dp`、`TabGap = 8.dp`、`TabPaddingHorizontal = 16.dp`
  - 圆角形状：`GlassShape(height / 2, smoothing = 0f)`
  - 指示块背景：`indicatorColor()`（`IndicatorAlpha = 0.133f`），未选中轨道：`restingTrackColor()`

## 上游限制与问题分析

1. **固定等宽切分截断长短不一的标签**：上游 `GlassTabRow` 与 `GlassSegmentedTabRow` 按 `(trackWidth - gap * (tabs.size - 1)) / tabs.size` 将容器宽度均分给每个标签；当标签数量多（如组件页 `Activities` / `Broadcast Receivers` / `Native Libraries`）或时间戳较长时，短标签留白过多而长标签被严重截断。
2. **直接嵌套 `horizontalScroll` 导致布局崩溃**：上游实现内部依赖父级有限宽度来等分计算子项尺寸；若外层直接套 `Modifier.horizontalScroll(...)`，横轴测量约束变为无界（`Constraints.Infinity`），触发：
   ```text
   java.lang.IllegalStateException: Size(2147483647 x 110) is out of range. Each dimension must be between 0 and 16777215.
   ```

## 本项目自适应实现

[`AdaptiveTabRow.kt`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt) 针对三种样式提供了可横向滚动、按文本自然宽度布局的 OS4 实现，并在 OS3 模式下回退至 Miuix `TabRow`：

- **[`AdaptiveGlassSegmentedTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt#L354)**（第三种，`AdaptiveTabRowStyle.Segmented`）：
  - 外层有限宽容器持有单层 `glass` 轨道背景与视口可用宽度；当标签自然总宽小于容器宽度时按最小保底宽均衡拓展并铺满屏宽（例如 ABI 双标签 `arm64-v8a` / `armeabi-v7a`），超出容器宽度时切换为单行文本自适应宽度与 `horizontalScroll`（`maxLines = 1, softWrap = false`）。
  - 通过 `Animatable` 驱动指示块位置与宽度（`GlassMotion.default()`），并在 `LaunchedEffect` 中自动将选中标签平滑滚动居中。
  - 外层对可滚动左右边缘施加 `14.dp` 的 `BlendMode.DstOut` 渐隐裁剪。
- **[`AdaptiveGlassTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt#L181)**（第一/二种，`Primary` 与 `Neutral`）：
  - 同样复用 [`distributeAdaptiveTabWidths`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt) 策略：窄于屏宽时均衡扩展铺满容器，超出屏宽时按各胶囊自然文本宽度横向滑动并自动居中，同时保留 `GlassShadows.Float` 独立胶囊玻璃效果。

## 如何切换回其他样式

1. **全局更改默认样式**：修改 [`AdaptiveTabRow`](../app/src/main/kotlin/com/absinthe/libchecker/ui/compose/component/AdaptiveTabRow.kt#L90) 的默认参数 `style: AdaptiveTabRowStyle = AdaptiveTabRowStyle.Segmented`：
   - 改为 `AdaptiveTabRowStyle.Primary`：40.dp 主色独立胶囊
   - 改为 `AdaptiveTabRowStyle.Neutral`：35.dp 紧凑中性独立胶囊
2. **按页面或层级单独指定**：在调用处传入 `style` 参数，例如次级 ABI 标签或快照时间标签单独改为 `Neutral`：
   ```kotlin
   AdaptiveTabRow(
     tabs = tabs,
     selectedTabIndex = selectedIndex,
     onTabSelected = { ... },
     style = AdaptiveTabRowStyle.Neutral,
     backdrop = pageState.topBarBackdrop
   )
   ```
