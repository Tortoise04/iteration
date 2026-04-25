
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout, Menu, Card, Button, Input, DatePicker, Form, Table, message, Space } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import moment from 'moment';
import request, { removeToken } from '../utils/request';

const { Header, Content, Sider } = Layout;
const { TextArea } = Input;
const { RangePicker } = DatePicker;

const MainApp = () => {
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
  const navigate = useNavigate();

  const handleLogout = () => {
    removeToken();
    navigate('/login');
  };

  const loadData = async () => {
    try {
      const [phoneUsageRes, goalRes, periodSummaryRes, achievementRes, dailyActivityRes] = await Promise.all([
        request.get('/phone-usage'),
        request.get('/goals'),
        request.get('/period-summaries'),
        request.get('/achievements'),
        request.get('/daily-activities')
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

  const getStatistics = async (type) => {
    try {
      const startDate = dateRange[0].format('YYYY-MM-DD');
      const endDate = dateRange[1].format('YYYY-MM-DD');
      let res;
      if (type === 'weekly') {
        res = await request.get(`/statistics/weekly?startDate=${startDate}&endDate=${endDate}`);
      } else {
        res = await request.get(`/statistics/monthly?startDate=${startDate}&endDate=${endDate}`);
      }
      setStatisticsData(res.data);
    } catch (error) {
      message.error('获取统计数据失败');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handlePhoneUsageSubmit = async (values) => {
    try {
      await request.post('/phone-usage', {
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

  const handleGoalSubmit = async (values) => {
    try {
      await request.post('/goals', {
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

  const handlePeriodSummarySubmit = async (values) => {
    try {
      await request.post('/period-summaries', {
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

  const handleAchievementSubmit = async (values) => {
    try {
      await request.post('/achievements', {
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

  const handleDeletePhoneUsage = async (id) => {
    try {
      await request.delete(`/phone-usage/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDeleteGoal = async (id) => {
    try {
      await request.delete(`/goals/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDeletePeriodSummary = async (id) => {
    try {
      await request.delete(`/period-summaries/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDeleteAchievement = async (id) => {
    try {
      await request.delete(`/achievements/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDailyActivitySubmit = async (values) => {
    try {
      await request.post('/daily-activities', {
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

  const handleDeleteDailyActivity = async (id) => {
    try {
      await request.delete(`/daily-activities/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

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

  const goalColumns = [
    { title: '目标', dataIndex: 'goal', key: 'goal' },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
    { title: '结束时间', dataIndex: 'endTime', key: 'endTime' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Button danger onClick={() => handleDeleteGoal(record.id)}>删除</Button>
      ) 
    }
  ];

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
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ color: 'white', fontSize: '20px' }}>数字健康助手</span>
        <Button
          type="text"
          icon={<LogoutOutlined />}
          onClick={handleLogout}
          style={{ color: 'white' }}
        >
          登出
        </Button>
      </Header>
      <Layout>
        <Sider width={200}>
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
        <Content style={{ padding: '24px', minHeight: '80vh' }}>
          {activeTab === 'phone-usage' && (
            <div>
              <Card title="记录手机使用情况" style={{ marginBottom: '16px' }}>
                <Form form={phoneUsageForm} onFinish={handlePhoneUsageSubmit} layout="vertical">
                  <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="usageTime" label="使用时间(分钟)" rules={[{ required: true, message: '请输入使用时间' }]}>
                    <Input type="number" placeholder="请输入使用时间" />
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
              <Card title="目标管理" style={{ marginBottom: '16px' }}>
                <Form form={goalForm} onFinish={handleGoalSubmit} layout="vertical">
                  <Form.Item name="goal" label="目标" rules={[{ required: true, message: '请输入目标' }]}>
                    <Input placeholder="请输入目标" />
                  </Form.Item>
                  <Form.Item name="startTime" label="开始时间" rules={[{ required: true, message: '请选择开始时间' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="endTime" label="结束时间" rules={[{ required: true, message: '请选择结束时间' }]}>
                    <DatePicker style={{ width: '100%' }} />
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

          {activeTab === 'daily-activities' && (
            <div>
              <Card title="每日活动" style={{ marginBottom: '16px' }}>
                <Form form={dailyActivityForm} onFinish={handleDailyActivitySubmit} layout="vertical">
                  <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="activity" label="活动" rules={[{ required: true, message: '请输入活动' }]}>
                    <TextArea rows={4} placeholder="请输入活动" />
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

          {activeTab === 'period-summaries' && (
            <div>
              <Card title="周期总结" style={{ marginBottom: '16px' }}>
                <Form form={periodSummaryForm} onFinish={handlePeriodSummarySubmit} layout="vertical">
                  <Form.Item name="period" label="周期" rules={[{ required: true, message: '请输入周期' }]}>
                    <Input placeholder="请输入周期" />
                  </Form.Item>
                  <Form.Item name="summary" label="总结" rules={[{ required: true, message: '请输入总结' }]}>
                    <TextArea rows={4} placeholder="请输入总结" />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
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
              <Card title="成果记录" style={{ marginBottom: '16px' }}>
                <Form form={achievementForm} onFinish={handleAchievementSubmit} layout="vertical">
                  <Form.Item name="achievement" label="成果" rules={[{ required: true, message: '请输入成果' }]}>
                    <Input placeholder="请输入成果" />
                  </Form.Item>
                  <Form.Item name="time" label="时间" rules={[{ required: true, message: '请选择时间' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit">保存</Button>
                  </Form.Item>
                </Form>
              </Card>
              <Card title="成果记录列表">
                <Table dataSource={achievementData} columns={achievementColumns} rowKey="id" />
              </Card>
            </div>
          )}

          {activeTab === 'statistics' && (
            <div>
              <Card title="统计分析" style={{ marginBottom: '16px' }}>
                <Space>
                  <RangePicker value={dateRange} onChange={(dates) => setDateRange(dates)} />
                  <Button type="primary" onClick={() => getStatistics('weekly')}>周统计</Button>
                  <Button type="primary" onClick={() => getStatistics('monthly')}>月统计</Button>
                </Space>
              </Card>
              {statisticsData && (
                <Card title="统计结果">
                  <pre>{JSON.stringify(statisticsData, null, 2)}</pre>
                </Card>
              )}
            </div>
          )}
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainApp;
