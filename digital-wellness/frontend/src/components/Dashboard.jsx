import React, { useState, useEffect } from 'react';
import { Card, Typography, Statistic, Row, Col, Spin, Empty, List, Checkbox, Button, Space, message } from 'antd';
import { CheckCircleOutlined, UnorderedListOutlined } from '@ant-design/icons';
import request from '../utils/request';

const { Title, Paragraph } = Typography;

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(null);
  const [todayTasks, setTodayTasks] = useState([]);
  const [tasksLoading, setTasksLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    setTasksLoading(true);
    try {
      // 并行加载统计数据和今日待办
      const [statsRes, tasksRes] = await Promise.all([
        request.get('/statistics/dashboard'),
        request.get('/daily-subtasks/today')
      ]);
      setStats(statsRes.data);
      setTodayTasks(tasksRes.data || []);
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
      setTasksLoading(false);
    }
  };

  // 完成任务
  const handleCompleteTask = async (taskId) => {
    try {
      await request.put(`/daily-subtasks/${taskId}/complete`);
      message.success('任务完成成功');
      // 重新加载今日待办
      const res = await request.get('/daily-subtasks/today');
      setTodayTasks(res.data || []);
    } catch (error) {
      message.error('任务完成失败');
      console.error('完成任务失败:', error);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '48px', textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '0' }}>
      <Card style={{ marginBottom: '24px' }}>
        <Title level={4}>数字健康助手</Title>
        <Paragraph type="secondary">
          欢迎使用数字健康助手系统！这是您的个人仪表盘，展示健康数据概览和关键指标。
        </Paragraph>
      </Card>

      {stats ? (
        <Row gutter={[16, 16]}>
          <Col span={6}>
            <Card>
              <Statistic
                title="今日手机使用"
                value={stats.todayPhoneUsage || 0}
                suffix="分钟"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="本周手机使用"
                value={stats.weekPhoneUsage || 0}
                suffix="分钟"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="进行中目标"
                value={stats.inProgressGoals || 0}
                suffix="个"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="已完成目标"
                value={stats.completedGoals || 0}
                suffix="个"
              />
            </Card>
          </Col>
        </Row>
      ) : (
        <Card>
          <Empty description="暂无数据" />
        </Card>
      )}

      {/* 今日待办 */}
      <Card style={{ marginTop: '24px' }} title={<Space><UnorderedListOutlined />今日待办</Space>}>
        {tasksLoading ? (
          <div style={{ padding: '40px 0', textAlign: 'center' }}>
            <Spin size="small" />
          </div>
        ) : todayTasks.length > 0 ? (
          <List
            dataSource={todayTasks}
            itemLayout="horizontal"
            renderItem={item => (
              <List.Item
                actions={[
                  <Button
                    type="link"
                    icon={<CheckCircleOutlined />}
                    onClick={() => handleCompleteTask(item.id)}
                    disabled={item.isCompleted}
                  >
                    完成
                  </Button>
                ]}
              >
                <List.Item.Meta
                  title={
                    <Checkbox checked={item.isCompleted} disabled>
                      {item.taskContent}
                    </Checkbox>
                  }
                  description={
                    <span style={{ color: '#666', fontSize: '12px' }}>
                      目标：{item.goalName}
                    </span>
                  }
                />
              </List.Item>
            )}
          />
        ) : (
          <div style={{ padding: '40px 0', textAlign: 'center' }}>
            <Empty description="今日暂无规划，去创建目标吧" />
          </div>
        )}
      </Card>
    </div>
  );
};

export default Dashboard;
