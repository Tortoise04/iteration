import React from 'react';
import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ children }) => {
  // 直接从 localStorage 读取 Token，不使用任何包装函数
  const token = localStorage.getItem('digital_wellness_token');

  // 没有 Token，重定向到登录页
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // 有 Token，渲染子组件
  return children;
};

export default ProtectedRoute;
