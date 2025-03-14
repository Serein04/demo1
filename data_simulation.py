import pandas as pd
import numpy as np
import uuid
from datetime import datetime, timedelta

# 配置参数
start_date = datetime.now() - timedelta(days=365)
end_date = datetime.now()
num_records = 800  # 平均每天约4条记录

# 交易类别权重
category_weights = {
    "餐饮": 0.3,
    "交通": 0.2,
    "购物": 0.2,
    "住房": 0.15,
    "娱乐": 0.1,
    "医疗": 0.05
}

# 详细交易描述映射
subcategory_map = {
    "餐饮": ["早餐", "午餐", "晚餐", "咖啡", "零食"],
    "交通": ["出租车", "地铁卡充值", "共享单车", "加油", "停车费"],
    "购物": ["网购", "超市", "服装", "电子产品", "生活用品"],
    "住房": ["房租", "物业费", "水电费", "宽带费", "维修"],
    "娱乐": ["电影", "KTV", "游戏充值", "门票", "运动健身"],
    "医疗": ["药品", "门诊", "体检", "医保"],
    "收入": ["工资"]
}

# 支付方式映射
payment_methods = {
    "餐饮": ["微信", "支付宝", "现金"],
    "交通": ["微信", "支付宝", "现金"],
    "购物": ["微信", "支付宝", "现金", "信用卡"],
    "住房": ["微信", "支付宝", "现金", "银行转账"],
    "娱乐": ["微信", "支付宝", "现金"],
    "医疗": ["微信", "支付宝", "现金", "医保卡"],
    "收入": ["现金"]
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
    # 5%概率生成收入记录
    if np.random.rand() < 0.05:  
        types.append(False)  # 收入为False
        category = "收入"
        subcat = np.random.choice(subcategory_map["收入"])
        amount = round(abs(np.random.normal(8000, 2000)), 2)  # 正态分布的工资
    else:
        types.append(True)  # 支出为True
        category = np.random.choice(list(category_weights.keys()), p=list(category_weights.values()))
        amount = abs(round(np.random.gamma(shape=2, scale=50), 2))  # 右偏分布金额
        # 较大金额的特殊处理
        if amount > 500 and category in ["住房", "购物"]:
            amount *= np.random.choice([1, 3, 5], p=[0.7, 0.2, 0.1])
    
    subcat = np.random.choice(subcategory_map.get(category, []))
    
    # 选择交易描述
    description = subcategory_map.get(category, [subcat])[0]
    
    # 随机选择支付方式
    payment_method = np.random.choice(payment_methods.get(category, ["现金"]))
    
    amounts.append(amount)
    categories.append(category)
    subcategories.append(subcat)
    descriptions.append(description)
    payment_methods_list.append(payment_method)


# 构造DataFrame
df = pd.DataFrame({
    "id": [str(uuid.uuid4()) for _ in range(num_records)],
    "amount": amounts,
    "date": dates.date,  
    "category": categories,
    "description": descriptions,
    "isExpense": types,
    "paymentMethod": payment_methods_list
})

# 添加工资日标记（每月5日）
salary_dates = pd.date_range(start_date, end_date, freq='MS') + pd.DateOffset(days=4)
salary_mask = df["date"].isin(salary_dates)
df.loc[salary_mask, ["isExpense", "category", "description", "paymentMethod"]] = [False, "收入", "工资 公司", "银行转账"]

# 保存CSV
df.to_csv("financial_records.csv", index=False, encoding='utf-8-sig')
print("成功生成包含%d条记录的CSV文件" % num_records)