import React, { useState } from 'react';
import { Layout, Menu, Card, Button, Input, DatePicker, TimePicker, Form, Table, message } from 'antd';
import axios from 'axios';
import moment from 'moment';

const { Header, Content, Sider } = Layout;
const { TextArea } = Input;
const { RangePicker } = DatePicker;

function App() {
  const [activeTab, setActiveTab] = useState('phone-usage');
  const [phoneUsageForm] = Form.useForm();
  const [goalForm] = Form.useForm();
  const [periodSummaryForm] = Form.useForm();
  const [achievementForm] = Form.useForm();
  const [dailyActivityForm] = Form.useForm();
  const [phoneUsageData, setPhoneUsageData] = useState([]);
  const [goalData, setGoalData] = useState([]);
  const [periodSummaryData, setPeriodSummaryData] = useState([]);
  const [achievementData, setAchievementData] = useState([]);
  const [dailyActivityData, setDailyActivityData] = useState([]);
  const [statisticsData, setStatisticsData] = useState(null);
  const [dateRange, setDateRange] = useState([moment().subtract(7, 'days'), moment()]);

  // 加载数据
  const loadData = async () => {
    try {
      const [phoneUsageRes, goalRes, periodSummaryRes, achievementRes, dailyActivityRes] = await Promise.all([
        axios.get('/api/phone-usage'),
        axios.get('/api/goals'),
        axios.get('/api/period-summaries'),
        axios.get('/api/achievements'),
        axios.get('/api/daily-activities')
      ]);
      setPhoneUsageData(phoneUsageRes.data);
      setGoalData(goalRes.data);
      setPeriodSummaryData(periodSummaryRes.data);
      setAchievementData(achievementRes.data);
      setDailyActivityData(dailyActivityRes.data);
    } catch (error) {
      message.error('加载数据失败');
    }
  };

  // 获取统计数据
  const getStatistics = async (type) => {
    try {
      const startDate = dateRange[0].format('YYYY-MM-DD');
      const endDate = dateRange[1].format('YYYY-MM-DD');
      let res;
      if (type === 'weekly') {
        res = await axios.get(`/api/statistics/weekly?startDate=${startDate}&endDate=${endDate}`);
      } else {
        res = await axios.get(`/api/statistics/monthly?startDate=${startDate}&endDate=${endDate}`);
      }
      setStatisticsData(res.data);
    } catch (error) {
      message.error('获取统计数据失败');
    }
  };

  // 初始化加载数据
  React.useEffect(() => {
    loadData();
  }, []);

  // 提交手机使用情况
  const handlePhoneUsageSubmit = async (values) => {
    try {
      await axios.post('/api/phone-usage', {
        date: values.date.toDate(),
        usageTime: values.usageTime
      });
      message.success('保存成功');
      phoneUsageForm.resetFields();
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 提交目标
  const handleGoalSubmit = async (values) => {
    try {
      await axios.post('/api/goals', {
        goal: values.goal,
        startTime: values.startTime.toDate(),
        endTime: values.endTime.toDate()
      });
      message.success('保存成功');
      goalForm.resetFields();
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 提交周期总结
  const handlePeriodSummarySubmit = async (values) => {
    try {
      await axios.post('/api/period-summaries', {
        period: values.period,
        summary: values.summary
      });
      message.success('保存成功');
      periodSummaryForm.resetFields();
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 提交成果
  const handleAchievementSubmit = async (values) => {
    try {
      await axios.post('/api/achievements', {
        achievement: values.achievement,
        time: values.time.toDate()
      });
      message.success('保存成功');
      achievementForm.resetFields();
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除手机使用情况
  const handleDeletePhoneUsage = async (id) => {
    try {
      await axios.delete(`/api/phone-usage/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 删除目标
  const handleDeleteGoal = async (id) => {
    try {
      await axios.delete(`/api/goals/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 删除周期总结
  const handleDeletePeriodSummary = async (id) => {
    try {
      await axios.delete(`/api/period-summaries/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 删除成果
  const handleDeleteAchievement = async (id) => {
    try {
      await axios.delete(`/api/achievements/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 提交每日活动
  const handleDailyActivitySubmit = async (values) => {
    try {
      await axios.post('/api/daily-activities', {
        date: values.date.toDate(),
        activity: values.activity
      });
      message.success('保存成功');
      dailyActivityForm.resetFields();
      loadData();
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 删除每日活动
  const handleDeleteDailyActivity = async (id) => {
    try {
      await axios.delete(`/api/daily-activities/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 手机使用情况表格列
  const phoneUsageColumns = [
    { title: '日期', dataIndex: 'date', key: 'date' },
    { title: '使用时间', dataIndex: 'usageTime', key: 'usageTime' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Button danger onClick={() => handleDeletePhoneUsage(record.id)}>删除</Button>
      ) 
    }
  ];

  // 目标表格列
  const goalColumns = [
    { title: '目标', dataIndex: 'goal', key: 'goal' },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
    { title: '完成时间', dataIndex: 'endTime', key: 'endTime' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Button danger onClick={() => handleDeleteGoal(record.id)}>删除</Button>
      ) 
    }
  ];

  // 周期总结表格列
  const periodSummaryColumns = [
    { title: '周期', dataIndex: 'period', key: 'period' },
    { title: '总结', dataIndex: 'summary', key: 'summary' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Button danger onClick={() => handleDeletePeriodSummary(record.id)}>删除</Button>
      ) 
    }
  ];

  // 成果表格列
  const achievementColumns = [
    { title: '成果', dataIndex: 'achievement', key: 'achievement' },
    { title: '时间', dataIndex: 'time', key: 'time' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Button danger onClick={() => handleDeleteAchievement(record.id)}>删除</Button>
      ) 
    }
  ];

  // 每日活动表格列
  const dailyActivityColumns = [
    { title: '日期', dataIndex: 'date', key: 'date' },
    { title: '活动', dataIndex: 'activity', key: 'activity' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Button danger onClick={() => handleDeleteDailyActivity(record.id)}>删除</Button>
      ) 
    }
  ];

  return (
    <Layout>
      <Header style={{ color: 'white', textAlign: 'center', fontSize: '24px' }}>
        数字健康助手
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[activeTab]}
            style={{ height: '100%', borderRight: 0 }}
            onSelect={({ key }) => setActiveTab(key)}
          >
            <Menu.Item key="phone-usage">手机使用情况</Menu.Item>
            <Menu.Item key="goals">目标管理</Menu.Item>
            <Menu.Item key="daily-activities">每日活动</Menu.Item>
            <Menu.Item key="period-summaries">周期总结</Menu.Item>
            <Menu.Item key="achievements">成果记录</Menu.Item>
            <Menu.Item key="statistics">统计分析</Menu.Item>
          </Menu>
        </Sider>
        <Content style={{ padding: '24px' }}>
          {activeTab === 'phone-usage' && (
            <div>
              <Card title="记录手机使用情况">
                <Form form={phoneUsageForm} onFinish={handlePhoneUsageSubmit} layout="vertical">
                  <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="usageTime" label="使用时间（分钟）" rules={[{ required: true, message: '请输入使用时间' }]}>
                    <Input type="number" placeholder="请输入使用时间（分钟）" />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
                  </Form.Item>
                </Form>
              </Card>
              <Card title="手机使用情况列表">
                <Table dataSource={phoneUsageData} columns={phoneUsageColumns} rowKey="id" />
              </Card>
            </div>
          )}

          {activeTab === 'goals' && (
            <div>
              <Card title="添加目标">
                <Form form={goalForm} onFinish={handleGoalSubmit} layout="vertical">
                  <Form.Item name="goal" label="目标" rules={[{ required: true, message: '请输入目标' }]}>
                    <Input placeholder="请输入目标" />
                  </Form.Item>
                  <Form.Item name="startTime" label="开始时间" rules={[{ required: true, message: '请选择开始时间' }]}>
                    <DatePicker showTime style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="endTime" label="完成时间" rules={[{ required: true, message: '请选择完成时间' }]}>
                    <DatePicker showTime style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
                  </Form.Item>
                </Form>
              </Card>
              <Card title="目标列表">
                <Table dataSource={goalData} columns={goalColumns} rowKey="id" />
              </Card>
            </div>
          )}

          {activeTab === 'period-summaries' && (
            <div>
              <Card title="添加周期总结">
                <Form form={periodSummaryForm} onFinish={handlePeriodSummarySubmit} layout="vertical">
                  <Form.Item name="period" label="周期" rules={[{ required: true, message: '请输入周期' }]}>
                    <Input placeholder="例如：第1周，2024年1月" />
                  </Form.Item>
                  <Form.Item name="summary" label="总结" rules={[{ required: true, message: '请输入总结' }]}>
                    <TextArea rows={4} placeholder="请输入总结内容" />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
                  </Form.Item>
                </Form>
              </Card>
              
              <Card title="生成周期总结">
                <Form layout="vertical">
                  <Form.Item label="选择日期范围">
                    <RangePicker value={dateRange} onChange={setDateRange} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item label="选择周期类型">
                    <select style={{ width: '100%', padding: '8px' }} id="periodType">
                      <option value="周">周</option>
                      <option value="月">月</option>
                    </select>
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" onClick={async () => {
                      try {
                        const startDate = dateRange[0].format('YYYY-MM-DD');
                        const endDate = dateRange[1].format('YYYY-MM-DD');
                        const periodType = document.getElementById('periodType').value;
                        await axios.post(`/api/period-summaries/generate?startDate=${startDate}&endDate=${endDate}&periodType=${periodType}`);
                        message.success('生成总结成功');
                        loadData();
                      } catch (error) {
                        message.error('生成总结失败');
                      }
                    }}>生成总结</Button>
                  </Form.Item>
                </Form>
              </Card>
              
              <Card title="周期总结列表">
                <Table dataSource={periodSummaryData} columns={periodSummaryColumns} rowKey="id" />
              </Card>
            </div>
          )}

          {activeTab === 'achievements' && (
            <div>
              <Card title="添加成果">
                <Form form={achievementForm} onFinish={handleAchievementSubmit} layout="vertical">
                  <Form.Item name="achievement" label="成果" rules={[{ required: true, message: '请输入成果' }]}>
                    <Input placeholder="请输入成果" />
                  </Form.Item>
                  <Form.Item name="time" label="时间" rules={[{ required: true, message: '请选择时间' }]}>
                    <DatePicker showTime style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
                  </Form.Item>
                </Form>
              </Card>
              <Card title="成果列表">
                <Table dataSource={achievementData} columns={achievementColumns} rowKey="id" />
              </Card>
            </div>
          )}

          {activeTab === 'daily-activities' && (
            <div>
              <Card title="添加每日活动">
                <Form form={dailyActivityForm} onFinish={handleDailyActivitySubmit} layout="vertical">
                  <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="activity" label="活动" rules={[{ required: true, message: '请输入活动' }]}>
                    <TextArea rows={4} placeholder="请输入今天做了什么" />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
                  </Form.Item>
                </Form>
              </Card>
              <Card title="每日活动列表">
                <Table dataSource={dailyActivityData} columns={dailyActivityColumns} rowKey="id" />
              </Card>
            </div>
          )}

          {activeTab === 'statistics' && (
            <div>
              <Card title="统计分析">
                <Form layout="vertical">
                  <Form.Item label="选择日期范围">
                    <RangePicker value={dateRange} onChange={setDateRange} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" onClick={() => getStatistics('weekly')}>获取周统计</Button>
                    <Button type="primary" style={{ marginLeft: '16px' }} onClick={() => getStatistics('monthly')}>获取月统计</Button>
                  </Form.Item>
                </Form>
              </Card>
              {statisticsData && (
                <Card title="统计结果">
                  <div style={{ fontSize: '16px' }}>
                    {Object.entries(statisticsData).map(([key, value]) => (
                      <p key={key}>{key}: {value} 分钟</p>
                    ))}
                  </div>
                </Card>
              )}
            </div>
          )}
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;