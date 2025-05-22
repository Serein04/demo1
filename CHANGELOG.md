# 更新日志


## [2.0.0] - 2023-3-6

### 修复

* 解决了所有四个选项卡（交易记录、预算管理、分析报告、AI助手）中都有返回主界面的按钮，并且位于左上角。
* 完善了setCategoryBudget()方法，添加了完整的NumberFormatException异常处理，确保用户输入无效金额时能够得到适当的错误提示。

### 新增
* 实现了generateAnalysisReport()方法，该方法获取所有交易记录并检查是否为空；清空并重建分析面板，生成并显示多种分析报告，刷新UI以显示新的分析结果。



## [2.1.0] - 2025-3-6

### 修复

* 解决了AI助手聊天功能卡死的问题。使用SwingWorker将API调用放在后台线程中执行，避免阻塞UI线程.



## [2.1.1] - 2025-3-7

### 优化

* 优化了AI助手功能，实现了流式输出并提高了响应速度。
* 使用SwingWorker的publish/process机制在后台线程处理响应，同时在EDT线程更新UI。
* 添加appendToMessage方法，实现在已有消息后追加内容，而不是替换整个消息。

## [2.1.2] - 2025-3-7

### 优化

* 优化了AI助手功能，在界面上添加了四个专用分析按钮：支出分析、预算建议、节省机会和季节性模式，用户可以通过点击这些按钮直接触发特定的分析功能，无需手动输入复杂的分析请求。
* 添加了简单问候识别功能，当用户发送"你好"等简单问候时，AI助手不会立即开始数据分析。避免了在简单交流时不必要的数据处理，提高了响应速度和用户体验。


## [2.2.0] - 2025-3-7

### 修复

* 正确显示预算管理界面，点击设置预算值后即可更新预算界面。


## [2.3.0] - 2025-3-7

### 新增

* 新增了TransactionEditDialog类，提供了编辑交易记录所有属性的界面.用户现在可以修改交易的各类属性。当用户修改类别时，系统会自动学习这种修正，提高未来分类的准确性。
* 改进了分类学习机制，使AI能够从用户的手动修正中持续学习。
* 新增了按类别提供详细分析的功能，包括消费趋势、异常支出和消费频率。

### 优化
* 重构了ExpenseAnalyzer类，使其能够利用TransactionClassifier的功能。

## [2.4.0] - 2025-3-9

### 优化
* 优化了交易记录界面的布局，更改了支出、收入的选择按钮的位置，使界面更加合理、美观。
* 创建了一个空白面板（emptyPanel），与支付方式面板具有相同的尺寸。这样，在支出和收入的不同状态下，保证交易记录表格大小的一致。


分工：

hxp     AIService.java               AIAssistantViewController.java     AIAssistantView.fxml

lzk     Main.java                    MainJavaFX.java                    StartScreenController.java         StartScreenView.fxml

qyh     ExpenseAnalyzer.java         AnalysisViewController.java        AnalysisView.fxml

wzh     BudgetManager.java           BudgetViewController.java          BudgetView.fxml

qsy     TransactionManager.java      TransactionViewController.java     TransactionView.fxml

xhf     Transaction.java             TransactionClassifier.java         TransactionEditDialog.java         
