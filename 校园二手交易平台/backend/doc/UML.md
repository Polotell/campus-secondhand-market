# UML 图示（类图 / 模块图）

> 对应实验报告 **系统设计 · UML 类图 / 模块图** 章节。  
> 本仓库使用 [**PlantUML**](https://plantuml.com/zh/)（包图、类图等）文本描述；可由编辑器插件或官网渲染成 PNG/SVG，便于粘贴进 Word。

## 一、已有 PlantUML 源文件

| 文件 | 内容 |
|---|---|
| `uml/order-business-swimlanes.puml` | **业务流程泳道图**（UML 活动图）：买家 / 商家 / 平台系统三泳道，主干「下单→扣费→发货→确认→结算」 |
| `uml/backend-package-modules.puml` | **后端模块图（UML 包图）**：`com.campus.market` 下 controller / service / impl / mapper / entity / dto / vo / common / aspect / interceptor 等与 MySQL 的依赖方向 |
| `uml/order-domain-classes.puml` | **订单核心域类图**：实体、`CheckoutDTO`、`OrderController` 与核心 Service 关联 |
| `flow.md` 内嵌片段 | **时序图**（下单→发货→确认→结算/退货）；与上表泳道图同属 4.6 |

## 二、如何自动生成图片（任选其一）

### 方式 A：在线渲染（零安装）

1. 打开 [PlantUML Web Server](https://www.plantuml.com/plantuml/uml/)。
2. 将所需 `.puml`（如 `uml/backend-package-modules.puml`）全文粘贴进编辑器。
3. 导出 **PNG** 或 **SVG** 插入报告。

### 方式 B：VS Code / Cursor

1. 安装扩展 **PlantUML**（作者 jebbs 或等价）。
2. 本地可选安装 [Graphviz](https://graphviz.org/download/)（复杂图更清晰）。
3. 打开 `.puml` 文件，使用命令面板执行 **PlantUML: Preview Current Diagram** 或 **Export Current Diagram**。

### 方式 C：IntelliJ IDEA 从代码反向生成类图（自动化程度高）

适用于「希望图与当前 Java 代码完全一致」时的补充或核对：

1. 在 **Project** 视图中选中包，例如 `com.campus.market.entity` 或 `com.campus.market.service`。
2. 右键 → **Diagrams** → **Show Diagram** → **Java Class Diagram**。
3. 可拖拽加入关联类，调整范围后 **Export to Image**（PNG/SVG）。

> Ultimate 版内置该能力；Community 版可安装 **PlantUML integration** 等插件配合本仓库 `.puml` 使用。

### 方式 D：命令行（批量导出）

若已安装 Java 与 [plantuml.jar](https://plantuml.com/zh/download)：

```bash
java -jar plantuml.jar -charset UTF-8 backend/doc/uml/backend-package-modules.puml backend/doc/uml/order-domain-classes.puml backend/doc/uml/order-business-swimlanes.puml
```

会在同目录生成 `.png`（默认）。

## 三、模块图与物理架构的对照

- **模块图（包图）**：`uml/backend-package-modules.puml`，描述 **后端 JVM 内**各 Java 包如何依赖（与源码树一致）。
- **物理架构图**：`architecture.md` 4.1.1，描述 **浏览器 / 两个前端工程 / 后端进程 / MySQL / 本地磁盘** 的部署关系；二者互补，报告里可各放一张。
- **IDEA 核对**：可选对 `com.campus.market` 使用 **Diagrams → Show Diagram → Package Diagram** 与上图对照（Ultimate 版）。

## 四、与 E-R、物理架构的分工

- **E-R**：见 `ER.md`，刻画**表、主外键、基数**。  
- **模块图（包图）**：见 `uml/backend-package-modules.puml`，刻画 **Java 包依赖**。  
- **类图**：刻画 **类与接口协作**（示例见 `order-domain-classes.puml`）；实体与表列基本一致，用途是说明代码结构。
- **业务流程泳道图**：见 `uml/order-business-swimlanes.puml`，与 **`flow.md` 时序图**同属一段业务：前者强调角色分工与步骤，后者强调对象间消息调用顺序。
