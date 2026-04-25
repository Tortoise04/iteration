import React, { useState, useEffect } from 'react';
import { Card, Typography, Statistic, Row, Col, Spin, Empty } from 'antd';
import request from '../utils/request';

const { Title, Paragraph } = Typography;

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(null);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const res = await request.get('/statistics/dashboard');
      setStats(res.data);
    } catch (error) {
      console.error('加载统计数据失败:', error);
    } finally {
      setLoading(false);
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
    </div>
  );
};

export default Dashboard;
