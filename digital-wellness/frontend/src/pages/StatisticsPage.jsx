import React, { useState } from 'react';
import { Card, DatePicker, Button, Space, Typography, Empty } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import request from '../utils/request';

const { RangePicker } = DatePicker;
const { Title, Paragraph } = Typography;

const StatisticsPage = () => {
  const [dateRange, setDateRange] = useState([dayjs().subtract(7, 'days'), dayjs()]);
  const [statisticsData, setStatisticsData] = useState(null);
  const [loading, setLoading] = useState(false);

  const getStatistics = async (type) => {
    setLoading(true);
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
      console.error('获取统计数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Card title="统计分析" style={{ marginBottom: '24px' }}>
        <Space direction="vertical" size="middle">
          <Space>
            <RangePicker
              value={dateRange}
              onChange={(dates) => setDateRange(dates)}
              format="YYYY-MM-DD"
            />
            <Button type="primary" onClick={() => getStatistics('weekly')} loading={loading}>
              周统计
            </Button>
            <Button onClick={() => getStatistics('monthly')} loading={loading}>
              月统计
            </Button>
          </Space>
          <Paragraph type="secondary">
            选择日期范围后，点击对应按钮查看统计数据
          </Paragraph>
        </Space>
      </Card>

      {statisticsData ? (
        <Card title="统计结果">
          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', backgroundColor: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
            {JSON.stringify(statisticsData, null, 2)}
          </pre>
        </Card>
      ) : (
        <Card>
          <Empty
            image={<LineChartOutlined style={{ fontSize: '64px', color: '#bfbfbf' }} />}
            description="请选择日期范围并点击统计按钮"
          />
        </Card>
      )}
    </div>
  );
};

export default StatisticsPage;
