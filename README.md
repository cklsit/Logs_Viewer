# LogsViewer

一个**在游戏内查看服务器日志**的 Minecraft 插件。体积小、加载快、零依赖，适配 **1.8.x – 1.21.x** 全版本服务器。

玩家无需退出游戏 / 登录面板，直接在聊天栏输入命令即可查看 `logs/latest.log` 中最近发生的错误、警告或普通信息。

## 特性

- 🪶 **极致轻量**：单个 jar 约 7 KB，无任何外部依赖，按需读取日志，加载零开销
- 🌐 **全版本兼容**：Java 8 编译产物 + 仅使用最基础的 Bukkit API，1.8.x 到 1.21.x 通吃
- 📜 **智能过滤**：按 `error / warn / info` 级别过滤，支持关键词内容搜索
- ⏪ **倒序查看**：默认从最新一条往前翻，直击最新问题
- 🚫 **防刷屏**：单次最多显示 50 条，超出自动截断
- 🛡️ **权限可控**：默认仅 OP 可用，可自由授权给指定权限组

## 安装

1. 将 `LogsViewer.jar` 放入服务器根目录的 `plugins/` 文件夹
2. 重启服务器（无需任何配置）
3. 完成 🎉

## 命令

```
/logsview <error|warn|info> <amount> [content]
```

| 参数 | 必填 | 说明 |
|------|:----:|------|
| `<error\|warn\|info>` | ✅ | 要查看的日志类型（不区分大小写） |
| `<amount>` | ✅ | 查看多少条，从最新往旧数 |
| `[content]` | ❌ | 日志内容关键词，忽略大小写模糊匹配 |

### 示例

```
/logsview error 10           → 查看最近 10 条 ERROR 日志
/logsview warn 5 崩溃         → 查看最近 5 条包含“崩溃”的 WARN 日志
/logsview info 20            → 查看最近 20 条 INFO 日志
/logsview error 3 Exception  → 查看最近 3 条包含“Exception”的 ERROR 日志
```

> 提示：异常堆栈等多行内容会跟随其所属日志一并显示，方便定位问题全貌。

## 权限

| 权限节点 | 默认值 | 说明 |
|----------|:------:|------|
| `logsview.use` | `op` | 允许使用 `/logsview` 命令 |

给指定玩家/权限组授权示例（`permissions.yml` 或权限插件）：

```yaml
permissions:
  viewer:
    permissions:
      logsview.use: true
```

## 构建

要求：JDK 8+（任意版本，产物固定编译为 Java 8）与 Maven 3.x

```bash
mvn package
```

构建产物位于 `target/LogsViewer.jar`。

## 兼容性说明

- **编译目标**：`maven.compiler.release=8`，字节码版本 52，老版本 Java 8 服务器可直接运行
- **API 版本**：`api-version: 1.8`，新版本服务器以 1.8 兼容模式加载
- **依赖面**：仅使用 `org.bukkit` 最基础 API（命令、消息、日志文件定位），不依赖任何高版本特性
- **编码**：按服务器 JVM 默认编码读取日志，与写入编码保持一致，中文不乱码

## 工作原理

1. 玩家执行 `/logsview` 时，定位服务器根目录下 `logs/latest.log`
2. 按行解析，通过正则提取时间戳与日志级别，异常堆栈等续行自动并入所属日志
3. 从尾部（最新）向前筛选符合级别（与可选关键词）的日志
4. 按级别着色（ERROR 红 / WARN 黄 / INFO 绿）逐条发送到聊天栏

日志文件不存在或读取失败时，会在游戏内给出明确提示。

## 限制

- 仅查看当天的 `latest.log`（日志滚动产生的历史文件不在范围内）
- 单次最多显示 50 条，防止刷屏
- 权限默认 `op`，非 OP 玩家需管理员手动授权

## License

MIT
