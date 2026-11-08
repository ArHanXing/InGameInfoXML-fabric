# InGame Info XML — 扩展 API

面向第三方模组：让你的数据变成一个 `{标签}`，用户可以写进 InGame Info XML 的配置里显示。

> 适用版本：Fabric 1.21.1 线（`ingameinfoxml` 2.9.2.101+）
> API 入口：`df11zomgraves.ingameinfo.api.InGameInfoAPI`

---

## 1. 最小例子：30 行加一个标签

```java
package com.example.mymod;

import df11zomgraves.ingameinfo.api.InGameInfoAPI;
import df11zomgraves.ingameinfo.tag.Tag;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class MyModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 软依赖：没装 InGameInfoXML 就什么都不做
        if (!InGameInfoAPI.isModLoaded("ingameinfoxml")) {
            return;
        }
        InGameInfoAPI.registerTag(new TagMyLevel().setName("mymod_level"));
    }

    /** 用户写 {mymod_level} 就能显示返回值。 */
    public static class TagMyLevel extends Tag {
        @Override
        public String getCategory() {
            return "mymod";      // 仅用于分类/调试
        }

        @Override
        public String getValue() {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return "-";
            }
            return String.valueOf(player.experienceLevel);
        }
    }
}
```

用户可以这样用：

```xml
<line><str>MyMod Lv: </str><var>mymod_level</var></line>
```

---

## 2. 为什么必须用 `InGameInfoAPI` 而不是直接调注册表

本模组在自己的 client initializer 里注册内置标签。第三方模组**无法预知**自己的 initializer 是先跑还是后跑。直接调 `TagRegistry.INSTANCE.register(...)` 会变成时序赌博。

`InGameInfoAPI.registerTag(...)` **在任何时刻调用都正确**：

| 调用时机 | 行为 |
|---|---|
| 内置标签初始化**之前** | 进队列，等初始化完自动补注册 |
| 内置标签初始化**之后** | 立即注册 |

所以你在自己的 `onInitializeClient()` 里直接写就行，不用管顺序。

---

## 3. API 参考

### 3.1 注册

| 方法 | 说明 |
|---|---|
| `registerTag(Tag tag)` | 注册一个标签。名称必须已用 `setName(...)` 设好 |
| `registerValue(Value value)` | 注册一个值/运算符类型（用于 `<max>` `<if>` `<operation>` 等元素内部） |
| `onReady(Runnable callback)` | 等内置注册表就绪后再执行；已就绪则立刻执行 |

### 3.2 查询

| 方法 | 说明 |
|---|---|
| `isReady()` | 内置注册表是否已初始化 |
| `isTagRegistered(String name)` | 名字是否已被占用（`{mana}` 和 `mana` 都接受）。**注册前先查，可避免撞名被拒** |
| `getRegisteredTagNames()` | 全部已注册名称（含别名），已排序，可用于做配置编辑器 |
| `isModLoaded(String modId)` | 软依赖判断的便捷封装 |

### 3.3 `Tag` 基类

必须实现两个方法：

| 方法 | 返回 |
|---|---|
| `String getCategory()` | 分类字符串，仅用于调试/分类 |
| `String getValue()` | 显示文本。**每帧都会调用**，别做重活 |

可选覆写 / 可用成员：

| 成员 | 用途 |
|---|---|
| `setName(String)` / `setAliases(String...)` | 主名与别名（大小写不敏感，均会注册） |
| `isIndexed()` + `getMaximumIndex()` | 声明为索引型标签，如 `{potioneffect0}`…`{potioneffect47}` |
| `protected static Minecraft minecraft` | 现成的 `Minecraft.getInstance()` |
| `protected static LocalPlayer player` | 当前玩家，**每 tick 自动更新**，进世界前为 `null` |
| `protected static ClientLevel world` | 当前世界，同上 |
| `protected static MBlockPos playerPosition` | 玩家位置（已缓存） |
| `protected static Vector3f playerMotion` | 玩家位移速度 |
| `Tag.getIconTag(Info)` | 生成 `{ICON|...}` 占位插入图标 |

> **重要**：`getValue()` 被调用的时机是"每帧渲染"，且每次渲染会对每行每个 `<var>` 各调一次。**绝不要在里面做文件 IO、网络请求、区块扫描**——那会直接毁掉帧率。慢数据请在 tick 回调里算好、缓存到静态字段，`getValue()` 只读缓存。

---

## 4. 想加运算符/值类型

`Value` 是 XML 里 `<max>`、`<if>`、`<operation>` 这类元素的实现。加自定义类型：

```java
InGameInfoAPI.registerValue(new ValueMyOp().setName("myop"));
```

约束：注册的实例只作为**原型**——只记录它的类和别名，实际使用时反射 `newInstance()`，因此**必须有 public 无参构造器**。

对绝大多数需求，直接用内置的 `<max>` `<min>` `<pct>` `<if>` `<round>` `<operation>` 组合就够了，**先确认真的需要扩展**再动手。

---

## 5. 真实例子：Botania 玩家魔力

以下签名来自 Botania `1.21.1-porting` 分支源码（`ManaItem.java`、`ManaItemsCallback.java`），非臆测：

```java
import vazkii.botania.api.mana.ManaItem;                      // getMana() / getMaxMana()
import vazkii.botania.api.fabric.mana.ManaItemsCallback;      // 收集饰品栏等非背包槽位

public static class TagBotaniaMana extends Tag {
    @Override public String getCategory() { return "botania"; }

    @Override public String getValue() {
        if (player == null) return "0";

        // 1) 让别的模组把它们的魔力槽位塞进来（饰品/戒指等）
        List<ItemStack> stacks = new ArrayList<>();
        ManaItemsCallback.EVENT.invoker().getManaItems(player, stacks);

        // 2) 背包本身不在该事件里，要自己补
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            stacks.add(player.getInventory().getItem(i));
        }

        // 3) 累加
        int total = 0;
        for (ItemStack stack : stacks) {
            ManaItem mi = ManaItem.LOOKUP.find(stack);   // @Nullable
            if (mi != null) {
                total += mi.getMana();
            }
        }
        return String.valueOf(total);
    }
}
```

依赖声明（编译期可见、运行期可选）：

```gradle
modCompileOnly "vazkii.botania:Botania:<版本>-fabric"
```

注册时务必加门禁：

```java
if (InGameInfoAPI.isModLoaded("botania")) {
    InGameInfoAPI.registerTag(new TagBotaniaMana().setName("botania_mana").setAliases("mana"));
}
```

---

## 6. 注意事项

1. **撞名会被拒**：已有同名标签时注册失败并只写一条 error 日志，不会静默替换内置标签。**先 `isTagRegistered(...)` 判断**。
2. **`getValue()` 抛异常不会崩游戏，但会显示 `null`**：`Value.getVariableValue` 里捕获了异常并返回字符串 `"null"`。调试时看不到明显报错，建议自己在 `getValue()` 里 try/catch 并打日志。
3. **未注册的标签不会报错**：用户写了 `{nosuchtag}` 会**原样显示** `{nosuchtag}`。所以写错名字的表现是"信息不更新"，不是崩溃。
4. **名称大小写不敏感**：`{Mana}` 和 `{mana}` 等价。
5. **服务端不要注册**：本模组的标签系统是纯客户端的（`onInitializeClient`）。请在 `ClientModInitializer` 里注册，别放 `ModInitializer`。
6. **线程**：注册预期发生在客户端主线程。队列本身是并发安全的，但顺序只在单线程下有意义。

---

## 7. 目前**不支持**的能力

说清楚边界，避免走错路：

| 需求 | 现状 |
|---|---|
| 自定义**渲染元素**（光栅图、曲线、任意绘制） | ❌ 不支持。`Tag` 只能返回字符串/插入内置图标 |
| 自定义**图标贴图** | ⚠️ 只能通过 `{ICON|}` 用原版精灵，无法注册自定义 sprite |
| 世界扫描类数据（矿脉、结构分析） | ❌ 无内建支持。需自己在模组侧扫描+缓存，`getValue()` 只读缓存 |
| 配置里做**数据驱动**的复杂查询 | ❌ 无查询引擎，只有"取标签值 + 条件/算术" |

如果你的需求落在这几行里，那不是"适配"，而是在本模组之上**新增子系统**，工程量另计。
