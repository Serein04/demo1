# 个人财务管理器

## 项目概述
这是一个智能的个人财务管理器应用程序，帮助用户跟踪费用，设置储蓄目标，并使用人工智能分析消费习惯。应用程序采用Java开发，具有简单直观的图形用户界面。

## 主要功能
1. **交易记录管理**：手动输入交易或从CSV文件导入数据
2. **智能分类系统**：AI自动分类交易，用户可手动校正
3. **支出分析与预测**：基于历史数据提供支出洞察和预算建议
4. **本地化财务背景**：适应中国特定的预算习惯和季节性消费模式

## 技术规格
- 开发语言：Java
- 用户界面：JavaFX
- 数据存储：文本文件（JSON/CSV）
- AI功能：调用大模型API

## 项目结构
```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   ├── financemanager/
│   │   │   │   │   ├── model/       # 数据模型
│   │   │   │   │   ├── view/        # 用户界面
│   │   │   │   │   ├── controller/  # 业务逻辑
│   │   │   │   │   ├── util/        # 工具类
│   │   │   │   │   ├── ai/          # AI相关功能
│   │   │   │   │   └── Main.java    # 程序入口
├── data/                # 数据存储目录
├── resources/           # 资源文件
└── README.md            # 项目说明
```

## 设置与运行

### 先决条件
在开始之前，请确保您的系统已安装以下软件：
- **Java Development Kit (JDK)**：版本 11 或更高版本。您可以从 [Oracle JDK](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) 或 [OpenJDK](https://openjdk.java.net/) 下载。
- **Apache Maven**：用于项目构建和依赖管理。您可以从 [Apache Maven 官网](https://maven.apache.org/download.cgi) 下载并安装。

### 配置
1.  **API 密钥配置**：
    项目需要一个 API 密钥才能使用 AI 功能。请打开 `config.properties` 文件（如果该文件不存在，请在项目根目录下创建它）。
    ```properties
    # API配置
    api.key=YOUR_API_KEY
    ```
    将 `YOUR_API_KEY` 替换为您自己的有效 API 密钥。当前的 `config.properties` 文件中有一个示例密钥 `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`，请务必替换它。

### 构建项目
1.  打开终端或命令提示符。
2.  导航到项目的根目录（包含 `pom.xml` 文件的目录）。
3.  运行以下 Maven 命令来编译代码并打包项目：
    ```bash
    mvn clean package
    ```
    此命令将清理先前的构建，编译源代码，运行测试（如果有），并将项目打包成一个可执行的 JAR 文件（包含所有依赖项），通常位于 `target/` 目录下，例如 `target/finance-manager-1.0-SNAPSHOT-jar-with-dependencies.jar`。

### 运行应用程序
您可以通过以下任一方式运行应用程序：

1.  **使用 Maven JavaFX 插件（推荐用于开发）**：
    在项目根目录下，运行以下命令：
    ```bash
    mvn javafx:run
    ```
    这将直接启动应用程序。

2.  **运行打包后的 JAR 文件**：
    构建成功后，您可以在 `target/` 目录中找到一个名为 `finance-manager-1.0-SNAPSHOT-jar-with-dependencies.jar` (或类似名称) 的文件。
    使用以下命令运行该 JAR 文件：
    ```bash
    java -jar target/finance-manager-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

## 敏捷开发方法
本项目采用敏捷开发方法，优先实现核心功能，然后逐步迭代添加更多功能。开发优先级如下：

1. 基础交易记录和数据结构
2. 简单的用户界面
3. 数据导入/导出功能
4. 基本分类系统
5. AI增强的分类和分析
6. 本地化功能

## 未来扩展
- 多用户支持
- 更复杂的财务报告
- 投资追踪和分析
- 移动应用程序同步
