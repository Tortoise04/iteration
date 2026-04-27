import axios from 'axios';

// Token 存储键
const TOKEN_KEY = 'digital_wellness_token';

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 120000  // 超时时间 120 秒（AI 生成可能较慢）
});

// 请求拦截器 - 自动在请求头中添加 Token
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 Token
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      // 将 Token 添加到 Authorization 头中
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 打印详细错误日志，方便排查是哪个接口返回了什么状态码
    console.error('[request 拦截器]', error?.config?.url, error?.response?.status, error?.response?.data);
    console.error('[request 拦截器] 完整 error:', error);
    console.error('[request 拦截器] 完整 error.response:', error?.response);

    // 401 处理逻辑（暂时注释，方便开发
    /*
    if (error?.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = '/login';
    }
    */

    return Promise.reject(error);
  }
);

// 保存 Token
export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
};

// 清除 Token
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};

// 获取 Token
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY);
};

export default request;
