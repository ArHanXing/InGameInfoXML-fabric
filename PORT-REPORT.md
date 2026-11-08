# InGame Info XML — Fabric 1.21.1 移植完成报告

> 目标项目：`fabric/`（本仓库内）
> 源项目：NeoForge 1.21.1（仓库根目录）
> 结论：**移植完成并已实机验证通过**（编译、服务端启动、客户端进世界、HUD 正常渲染）

---

## 1. 成果

| 项目 | 结果 |
|---|---|
| 构建 | `mc_gradle build` **BUILD SUCCESSFUL** |
| 产物 | `fabric/build/libs/ingameinfoxml-2.9.2.101.jar` |
| 源码 | 70 个 Java 文件 / 8074 行，**零 `net.neoforged` 引用** |
| 服务端 | `runServer` 正常启动，`ingameinfoxml common initialised`，无客户端类加载 |
| 客户端 | `runClient` 进世界，`Registered 624 tags`，HUD **实机正常渲染**（用户确认） |
| Mixin | 4 个注入点全部生效（运行时日志确认） |
| 配置 | Cloth Config 正常落盘 `config/ingameinfoxml.json`，26 项全量 |

## 2. 版本坐标（全部经上游核对）

```properties
minecraft_version=1.21.1
loader_version=0.20.1              # meta.fabricmc.net stable
loom_version=1.17.19
fabric_api_version=0.116.17+1.21.1 # Modrinth 最新 release
cloth_config_version=15.0.140      # maven.shedaniel.me
modmenu_version=11.0.5             # compileOnly + localRuntime
```

- **scaffold 变通**：`mc_scaffold` 无 fabric 1.21.1 钉选值（严格钉选会报错退出），故用 `minecraftVersion="1.21"` 生成后重定向坐标。
- Gradle JVM 用 JDK 21（`org.gradle.java.home`），因本机默认 JDK 25 而 1.21.1 属 Java 21 线。

## 3. 实际改动清单

**逐字保留（~6,700 行）**：`tag/`（除 TagMisc）、`value/`、`parser/`、`util/`、`gui/overlay/`、`reference/`、`InGameInfoCore`。因源项目本就是 mojmap，这些文件**一行未改**。

**新写/重写（10 个文件，约 775 行）**：

| 文件 | 替换的 NeoForge 机制 |
|---|---|
| `InGameInfoXML.java` | `@Mod` 构造器 → `ModInitializer`（双端） |
| `InGameInfoXMLClient.java` | 客户端初始化（原 `FMLEnvironment.dist == CLIENT` 分支） |
| `handler/ConfigurationHandler.java` | `ModConfigSpec` ×30 → Cloth Config AutoConfig（保留同名静态字段，其他文件零改动） |
| `handler/Ticker.java` | `RenderGuiEvent`/`ClientTickEvent` → `HudRenderCallback`/`ClientTickEvents` |
| `handler/ClientEventHandler.java` | `NeoForgeClientEventHandler`（tick + 客户端命令） |
| `handler/KeyInputHandler.java` | `RegisterKeyMappingsEvent` → `KeyBindingHelper` |
| `network/ServerNetworkHandler.java` | `PacketDistributor.sendToPlayer` + `IPayloadContext` |
| `network/ClientNetworkHandler.java` | 同上（客户端侧） |
| `mixin/GuiMixin.java` | **新增开发**：`RenderGuiLayerEvent` 的替代（见 §4） |
| `compat/ModMenuIntegration.java` | `IConfigScreenFactory`/`ConfigurationScreen` 的替代 |

**4 个 payload record** 保留 vanilla `CustomPacketPayload`+`StreamCodec`，仅剥离 NeoForge 的 `handleMessage`（把客户端类拉进服务端）。

## 4. 唯一新增开发：GUI 层抑制 Mixin

NeoForge 的 `RenderGuiLayerEvent.Pre` + `VanillaGuiLayers` 在 Fabric 1.21.1 **无对应**（多图层 HUD API 在 0.116.16 backport、**0.116.17 又撤回**）。故用 4 个 `@Inject` 实现，注入点经**真实 1.21.1 jar 反查确认**：

| NeoForge 图层 | 1.21.1 mojmap 方法 | intermediary（运行时确认） |
|---|---|---|
| PLAYER_HEALTH + ARMOR_LEVEL + AIR_LEVEL | `renderPlayerHealth` | `method_1760` |
| FOOD_LEVEL | `renderFood` | `method_58477` |
| VEHICLE_HEALTH | `renderVehicleHealth` | `method_1741` |
| EFFECTS | `renderEffects` | `method_1765` |

> 关键发现：**护甲与氧气气泡都在 `renderPlayerHealth` 内部绘制**（`renderArmor` 为 private static、氧气由 `AIR_SPRITE` 绘制），所以 6 个原版图层只需 4 个注入点。

## 5. 移植中发现的真实 API 落差

这几个是**编译探针实测**出来的（非推测），属于 NeoForge 打补丁过的 vanilla 表面：

1. **`MinecraftServer.getTickTime(ResourceKey<Level>)`** —— NeoForge 补丁方法，vanilla 无。改用 `getTickTimesNanos()`（同一份逐 tick 纳秒数组；tick 时间是全局的，非按维度）。
2. **`KeyMapping.getKey()`** —— NeoForge 补丁方法。改用 vanilla `getTranslatedKeyMessage()`（已本地化，正是原代码 `Component.translatable(name)` 的效果）。
3. **`ArgumentTypeInfos.registerByClass`** —— NeoForge 补丁方法，vanilla 无 public 版本。因本项目命令是**客户端命令**，原 `InGameInfoArgumentTypes` 整个删除（客户端命令的 argument type 无需注册同步）。
4. **Fabric 客户端命令来源类型是 `FabricClientCommandSource`**，不是 `CommandSourceStack` —— `InGameInfoCommand` 相应改类型（该文件所有输出走 `Minecraft.getInstance().gui.getChat()`，不用 source，故改动是机械替换）。
5. **`fabric.mod.json` 不认 `credits` 键** —— 改为 `contributors` 数组。
6. **Fabric 客户端网络类在 `net.fabricmc.fabric.api.client.networking.v1`**（不在 `.api.networking.v1`），且接收器运行在 **netty 线程**，主线程调度用 `context.server().execute(...)` / `context.client().execute(...)` 对应 NeoForge 的 `enqueueWork`。
7. **Gradle `expand()` 会破坏 JSON 转义（踩坑）**：`processResources` 里的 `expand` 底层是 Groovy `SimpleTemplateEngine`，它会把**输出中**的 `\n` 当成转义序列写成真实换行，导致 `fabric.mod.json` 的 `description` 变成非法 JSON，loader 直接报 `invalid entries in its mod json` 并拒载。已改用 `ReplaceTokens` 过滤器（`beginToken: '${'`, `endToken: '}'`）只替换版本号 token，不再触碰其余文本。

## 6. 配置方案说明（含被否决方案）

**已采纳：Cloth Config AutoConfig。**

关键理由：NeoForge 侧同时提供「持久化」和「自动生成配置界面」两项能力，Fabric 都没有。Cloth Config **一个库同时补上这两块**（`GsonConfigSerializer` 持久化 + `AutoConfig.getConfigScreen` 界面），且**在公共 maven 上**（`maven.shedaniel.me`），无需 vendor jar。

**被否决：forge-config-api-port（你建议的方案）。** 我实际查证后的结论是它在 1.21.1 上不可行：
- v21.1.6-1.21.1-Fabric **确实存在**（Modrinth，767 万下载，2025-11 更新）；
- 但该分支上游 README 明确标注 **Maven: n/a**（1.21.1 线状态 "Bugfixes only"），**根本没有发布 maven 构件**；
- 采用它就意味着要 vendor 一个 633 KB 第三方 jar + 引入硬依赖，且它**只解决配置后端、不解决配置界面**。

它是**保留 `ModConfigSpec` 语义**的最佳选择，如果将来接受 vendor jar，仍可切换——届时 `ConfigurationHandler` 可基本还原为源文件，代码量还能再降。

配置项名称与 NeoForge 版**完全一致**（`showInChat`、`scale`、`healthDigit` 等 26 项），仅文件格式由 `ingameinfoxml-client.toml` 变为 `config/ingameinfoxml.json`，迁移是复制粘贴级。

## 7. 兼容性保留

- `{neoforgeversion}` 标签**保留为别名**（指向 Fabric loader 版本），同时新增 `{fabricloaderversion}`，老配置 XML 不会失效。
- 标签总数与行为不变（624 个，实机确认）。
- mod id / 版本号 / 包名与 NeoForge 版一致（`ingameinfoxml` / 2.9.2.101 / `df11zomgraves.ingameinfo`）。

## 8. 与前期《工作量分析》的对照

分析报告预估 **6–9 人日**且把「NeoForge 补丁过的 vanilla 成员」列为首要风险；实际执行结果：

- 首次编译 **仅 4 个错误**（正是预估的补丁表面），修完即通过 —— 预估方向准确；
- 「零 Mixin」的判断成立，唯一新增开发（GUI 抑制）确如预估是最贵的一项；
- 实际耗时远低于 6 人日，主要因为编译探针一次就暴露了全部落差，没有反复试错。

## 9. 复现方式

```bash
cd fabric
./gradlew build                 # 产物 build/libs/ingameinfoxml-2.9.2.101.jar
./gradlew runClient             # 开发客户端
./gradlew runServer             # 开发服务端（run/eula.txt 已置 true）
```

放入正式环境时，客户端需同时安装：Fabric Loader（≥0.20.1）、Fabric API、Cloth Config（≥15.0.0）；ModMenu 可选（提供配置界面入口）。
