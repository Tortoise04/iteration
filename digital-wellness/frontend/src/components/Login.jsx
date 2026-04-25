import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Card, Typography, message, ConfigProvider, theme } from 'antd';
import { UserOutlined, LockOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import request from '../utils/request';

const { Title, Text } = Typography;
const { darkAlgorithm, defaultAlgorithm } = theme;

const TOKEN_KEY = 'digital_wellness_token';

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [currentTheme, setCurrentTheme] = useState('dark');
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm();

  // 初始化主题
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    setCurrentTheme(savedTheme);
    document.documentElement.classList.toggle('dark', savedTheme === 'dark');
  }, []);

  // 主题切换函数
  const handleToggleTheme = () => {
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setCurrentTheme(newTheme);
    localStorage.setItem('theme', newTheme);
    document.documentElement.classList.toggle('dark', newTheme === 'dark');
  };

  // 处理登录
  const handleLogin = async (values) => {
    setLoading(true);
    try {
      const response = await request.post('/auth/login', values);

      // === 调试：打印后端真实返回值 ===
      console.log('后端真实返回值:', response.data);
      console.log('response.data.token:', response.data.token);
      console.log('response.data.data:', response.data.data);

      // 尝试从不同位置获取 token
      let token = response.data.token;
      let username = response.data.username;

      // 如果 token 在 data 对象里
      if (!token && response.data.data) {
        token = response.data.data.token;
        username = response.data.data.username;
      }

      // 最终检查
      if (!token) {
        console.error('无法从响应中提取 token，响应结构:', JSON.stringify(response.data, null, 2));
        message.error('登录失败：无法获取 Token');
        return;
      }

      console.log('最终提取的 token:', token);

      // 1. 先保存 Token 到 localStorage
      localStorage.setItem(TOKEN_KEY, token);

      // 2. 显示成功消息
      message.success(`登录成功，欢迎 ${username || '用户'}！`);

      // 3. 获取登录前想访问的页面，默认跳转首页
      const from = location.state?.from?.pathname || '/';

      // 4. 执行跳转
      navigate(from, { replace: true });
    } catch (error) {
      console.error('登录失败:', error);
      message.error('登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ConfigProvider
      theme={{
        algorithm: currentTheme === 'dark' ? darkAlgorithm : defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          colorBgContainer: currentTheme === 'dark' ? '#1f1f1f' : '#ffffff',
          colorText: currentTheme === 'dark' ? '#ffffffd9' : '#333333',
          colorBorder: currentTheme === 'dark' ? '#434343' : '#e8e8e8',
        }
      }}
    >
      <div className="login-container" style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: currentTheme === 'dark' ? '#141414' : '#f5f5f5',
        padding: '24px',
        position: 'relative',
        margin: 0,
        width: '100%',
        transition: 'background-color 0.3s ease'
      }}>
        <Button
          type="text"
          icon={currentTheme === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          onClick={handleToggleTheme}
          style={{
            position: 'absolute',
            top: '24px',
            right: '24px',
            color: currentTheme === 'dark' ? '#ffffffd9' : '#333',
            fontSize: '24px'
          }}
        />

        <Card
          className="login-card"
          style={{
            width: '100%',
            maxWidth: '400px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
            backgroundColor: currentTheme === 'dark' ? '#1f1f1f' : '#fff',
            border: currentTheme === 'dark' ? '1px solid #434343' : '1px solid #e8e8e8',
            borderRadius: '8px'
          }}
        >
          <div style={{ textAlign: 'center', marginBottom: '32px' }}>
            <Title level={3} style={{
              color: currentTheme === 'dark' ? '#ffffffd9' : '#333',
              marginBottom: '8px'
            }}>
              数字健康助手
            </Title>
            <Text style={{
              color: currentTheme === 'dark' ? '#ffffff73' : '#666'
            }}>
              请登录您的账户
            </Text>
          </div>

          <Form
            form={form}
            name="login"
            onFinish={handleLogin}
            layout="vertical"
            autoComplete="off"
          >
            <Form.Item
              name="username"
              label="用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined style={{
                  color: currentTheme === 'dark' ? '#ffffff73' : '#999'
                }} />}
                placeholder="请输入用户名"
                size="large"
              />
            </Form.Item>

            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined style={{
                  color: currentTheme === 'dark' ? '#ffffff73' : '#999'
                }} />}
                placeholder="请输入密码"
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                size="large"
                block
              >
                登录
              </Button>
            </Form.Item>

            <div style={{ textAlign: 'center' }}>
              <Text type="secondary" style={{
                color: currentTheme === 'dark' ? '#ffffff73' : '#666'
              }}>
                还没有账号？请联系管理员
              </Text>
            </div>
          </Form>
        </Card>
      </div>
    </ConfigProvider>
  );
};

export default Login;
