import pandas as pd
import numpy as np
import uuid
from datetime import datetime, timedelta

# 配置参数
start_date = datetime.now() - timedelta(days=365)  # 开始日期：当前时间往前推一年
end_date = datetime.now()  # 结束日期：当前时间
num_records = 800  # 记录数量：每天约 4 条记录

# 交易类别权重（与 Java 的 TransactionClassifier 对齐）
category_weights = {
    "餐饮": 0.25,           # 餐饮（从 Dining 重命名）
    "交通": 0.15, # 交通（从 Transport 重命名）
    "购物": 0.15,       # 购物
    "住房": 0.10,        # 住房
    "娱乐": 0.08,  # 娱乐
    "医疗": 0.05,        # 医疗
    "教育": 0.03,      # 教育
    "旅行": 0.03,         # 旅行
    "日用品": 0.03,  # 日用品
    "通讯": 0.03,  # 通讯
    "服装": 0.03,       # 服装
    "礼物": 0.03,          # 礼物
    "其他": 0.04  # 其他支出（用于剩余概率）
}

# Java 默认收入类别（用于随机选择）
DEFAULT_INCOME_CATEGORIES = [
    "工资", "奖金", "投资收益", "兼职收入", "礼金", "退款", "其他收入"
]

# 详细交易描述映射（与 Java 的 TransactionClassifier 对齐）
subcategory_map = {
    # 支出
    "餐饮": ["早餐", "午餐", "晚餐", "咖啡", "零食", "餐厅", "外卖"],
    "交通": ["出租车", "地铁卡充值", "共享单车", "加油", "停车费", "公交车", "火车", "航班"],
    "购物": ["网购", "超市", "服装", "电子产品", "日用品", "商场", "杂货"],
    "住房": ["房租", "物业费", "水电费", "宽带费", "维修费", "房贷"],
    "娱乐": ["电影", "KTV", "游戏充值", "门票", "运动健身", "演唱会"],
    "医疗": ["药品", "门诊", "体检", "医疗保险", "医院"],
    "教育": ["学费", "书籍", "培训课程", "文具"],
    "Travel": ["酒店", "机票", "火车票", "景点门票", "度假"],
    "日用品": ["洗漱用品", "清洁用品", "家居用品"],
    "通讯": ["电话费", "网费", "邮费"],
    "服装": ["衣服", "鞋子", "配饰", "包包"],
    "礼物": ["礼物购买", "红包支出", "捐赠"],
    "其他": ["银行手续费", "罚款", "杂项"],
    # 收入
    "Salary": ["月薪", "基本工资", "佣金"],
    "Bonus": ["年终奖", "绩效奖金", "项目奖金"],
    "Investment Income": ["股票分红", "基金收益", "利息收入"],
    "Part-time Income": ["自由职业收入", "家教费", "咨询费"],
    "Gift Money": ["红包收入", "现金礼物"],
    "Refund": ["退税", "商品退款", "押金返还"],
    "Other Income": ["彩票中奖", "保险理赔", "其他收入"]
}

# 支付方式映射（与 Java 的 TransactionClassifier 对齐）
payment_methods = {
    # 支出
    "餐饮": ["微信支付", "支付宝", "现金", "信用卡"],
    "交通": ["微信支付", "支付宝", "现金", "地铁卡"],
    "购物": ["微信支付", "支付宝", "现金", "信用卡", "借记卡"],
    "住房": ["银行转账", "支付宝", "微信支付", "现金"],
    "娱乐": ["微信支付", "支付宝", "现金", "信用卡"],
    "医疗": ["微信支付", "支付宝", "现金", "医保卡", "银行转账"],
    "教育": ["银行转账", "微信支付", "支付宝", "信用卡"],
    "Travel": ["信用卡", "支付宝", "微信支付", "银行转账"],
    "日用品": ["微信支付", "支付宝", "现金", "超市卡"],
    "通讯": ["支付宝", "微信支付", "银行转账"],
    "服装": ["信用卡", "支付宝", "微信支付", "现金"],
    "礼物": ["现金", "微信支付", "支付宝", "银行转账"],
    "其他": ["现金", "银行转账", "支付宝"],
    # 收入
    "Salary": ["银行转账"],
    "Bonus": ["银行转账"],
    "Investment Income": ["银行转账"],
    "Part-time Income": ["银行转账", "支付宝", "微信支付", "现金"],
    "Gift Money": ["现金", "微信支付", "支付宝"],
    "Refund": ["银行转账", "支付宝", "微信支付"],
    "Other Income": ["现金", "银行转账", "支付宝"]
}

# 生成基础数据
np.random.seed(42)
dates = pd.date_range(start_date, end_date, periods=num_records)
amounts = []
categories = []
subcategories = []
descriptions = []
types = []
payment_methods_list = []

for _ in range(num_records):
    # 5% 的概率生成收入记录
    if np.random.rand() < 0.05:
        types.append(False)  # 收入为 False
        category = np.random.choice(DEFAULT_INCOME_CATEGORIES)  # 从 Java 的收入类别中选择
        # 根据收入类型分配金额（示例逻辑）
        if category == "Salary":
            amount = round(abs(np.random.normal(8000, 1500)), 2)
        elif category == "Bonus":
            amount = round(abs(np.random.normal(5000, 3000)), 2)
        elif category == "Investment Income":
            amount = round(abs(np.random.gamma(shape=1.5, scale=100)), 2)
        else:  # 其他收入类型
            amount = round(abs(np.random.gamma(shape=1, scale=50)), 2)
    else:
        types.append(True)  # 支出为 True
        category = np.random.choice(list(category_weights.keys()), p=list(category_weights.values()))
        amount = abs(round(np.random.gamma(shape=2, scale=50), 2))  # 右偏分布金额
        # 对较大金额进行特殊处理（根据需要调整类别）
        if amount > 500 and category in ["住房", "购物", "Travel", "教育", "医疗"]:
            amount *= np.random.choice([1, 2, 4], p=[0.7, 0.2, 0.1])  # 调整乘数

    # 获取子类别，确保列表不为空
    possible_subcats = subcategory_map.get(category, [])
    if not possible_subcats:  # 如果类别没有定义子类别，则回退
        subcat = category  # 使用类别名称作为子类别
        description = category  # 使用类别名称作为描述
    else:
        subcat = np.random.choice(possible_subcats)
        # 选择交易描述（此处简单使用子类别作为描述）
        description = subcat

    # 随机选择支付方式，确保列表不为空
    possible_payments = payment_methods.get(category, ["现金"])  # 默认使用现金
    if not possible_payments:
        payment_method = "现金"  # 回退
    else:
        payment_method = np.random.choice(possible_payments)

    amounts.append(amount)
    categories.append(category)
    subcategories.append(subcat)
    descriptions.append(description)
    payment_methods_list.append(payment_method)

# 构造 DataFrame
df = pd.DataFrame({
    "id": [str(uuid.uuid4()) for _ in range(num_records)],  # 唯一标识符
    "amount": amounts,  # 金额
    "date": dates.date,  # 日期
    "category": categories,  # 类别
    "description": descriptions,  # 描述
    "isExpense": types,  # 是否为支出
    "paymentMethod": payment_methods_list  # 支付方式
})

# 添加工资日标记（每月 5 日） - 确保使用正确的 "Salary" 类别
salary_dates = pd.date_range(start_date, end_date, freq='MS') + pd.DateOffset(days=4)  # 假设工资在每月 5 日发放
salary_mask = df["date"].isin(salary_dates.date)  # 仅比较日期
# 覆盖工资日期的记录为工资收入
df.loc[salary_mask, ["isExpense", "category", "description", "paymentMethod", "amount"]] = \
    [False, "Salary", "月薪", "银行转账", round(abs(np.random.normal(8000, 1500)), 2)]

# 保存 CSV 文件（考虑更改文件名，例如 transactions_simulated.csv）
df.to_csv("data/transactions.csv", index=False, encoding='utf-8-sig')  # 保存到 data 目录
print("成功生成包含 %d 条记录的 CSV 文件，保存路径为 data/transactions.csv" % num_records)
print("成功生成包含 %d 条记录的 CSV 文件" % num_records)
