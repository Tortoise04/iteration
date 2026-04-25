import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, Typography } from 'antd';
import { LogoutOutlined, MoonOutlined, SunOutlined, DashboardOutlined, PhoneOutlined, FlagOutlined, CalendarOutlined, FileTextOutlined, TrophyOutlined, PieChartOutlined } from '@ant-design/icons';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { removeToken } from '../utils/request';

const { Header, Sider, Content } = Layout;
const { Title } = Typography;

const AppLayout = () => {
  const [currentTheme, setCurrentTheme] = useState('dark');
  const [currentMenu, setCurrentMenu] = useState('dashboard');
  const navigate = useNavigate();
  const location = useLocation();

  // 初始化主题
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    setCurrentTheme(savedTheme);
    document.documentElement.classList.toggle('dark', savedTheme === 'dark');
  }, []);

  // 监听路由变化，更新当前菜单
  useEffect(() => {
    const path = location.pathname;
    if (path.includes('/dashboard') || path === '/') {
      setCurrentMenu('dashboard');
    } else if (path.includes('/phone-usage')) {
      setCurrentMenu('phone-usage');
    } else if (path.includes('/goals')) {
      setCurrentMenu('goals');
    } else if (path.includes('/daily-activities')) {
      setCurrentMenu('daily-activities');
    } else if (path.includes('/period-summaries')) {
      setCurrentMenu('period-summaries');
    } else if (path.includes('/achievements')) {
      setCurrentMenu('achievements');
    } else if (path.includes('/statistics')) {
      setCurrentMenu('statistics');
    }
  }, [location.pathname]);

  // 主题切换函数
  const handleToggleTheme = () => {
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setCurrentTheme(newTheme);
    localStorage.setItem('theme', newTheme);
    document.documentElement.classList.toggle('dark', newTheme === 'dark');
  };

  // 登出函数
  const handleLogout = () => {
    removeToken();
    navigate('/login');
  };

  // 菜单点击事件
  const handleMenuClick = ({ key }) => {
    setCurrentMenu(key);
    navigate(`/${key}`);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* 固定左侧导航栏 */}
      <Sider
        width={200}
        style={{
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          overflow: 'auto',
          backgroundColor: 'var(--bg-secondary)',
          borderRight: '1px solid var(--border-color)',
          zIndex: 100
        }}
      >
        <div style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderBottom: '1px solid var(--border-color)',
          padding: '0 24px'
        }}>
          <Title level={5} style={{ color: 'var(--text-primary)', margin: 0 }}>
            数字健康助手
          </Title>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[currentMenu]}
          style={{
            height: 'calc(100% - 64px)',
            borderRight: 0,
            backgroundColor: 'var(--bg-secondary)'
          }}
          onSelect={handleMenuClick}
          items={[
            {
              key: 'dashboard',
              icon: <DashboardOutlined />,
              label: '仪表盘'
            },
            {
              key: 'phone-usage',
              icon: <PhoneOutlined />,
              label: '手机使用情况'
            },
            {
              key: 'goals',
              icon: <FlagOutlined />,
              label: '目标管理'
            },
            {
              key: 'daily-activities',
              icon: <CalendarOutlined />,
              label: '每日活动'
            },
            {
              key: 'period-summaries',
              icon: <FileTextOutlined />,
              label: '周期总结'
            },
            {
              key: 'achievements',
              icon: <TrophyOutlined />,
              label: '成果记录'
            },
            {
              key: 'statistics',
              icon: <PieChartOutlined />,
              label: '统计分析'
            }
          ]}
        />
      </Sider>
      {/* 右侧内容区域，margin-left 为侧边栏宽度 */}
      <Layout style={{ marginLeft: 200 }}>
        <Header style={{
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: 'var(--bg-secondary)',
          borderBottom: '1px solid var(--border-color)',
          position: 'sticky',
          top: 0,
          zIndex: 99
        }}>
          <div style={{ color: 'var(--text-primary)', fontSize: '16px', fontWeight: '500' }}>
            欢迎使用数字健康助手
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <Button
              type="text"
              icon={currentTheme === 'dark' ? <SunOutlined /> : <MoonOutlined />}
              onClick={handleToggleTheme}
              style={{ color: 'var(--text-primary)' }}
            />
            <Button
              type="text"
              icon={<LogoutOutlined />}
              onClick={handleLogout}
              style={{ color: 'var(--text-primary)' }}
            >
              登出
            </Button>
          </div>
        </Header>
        <Content style={{
          padding: '24px',
          minHeight: 'calc(100vh - 64px)',
          backgroundColor: 'var(--bg-primary)'
        }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;
