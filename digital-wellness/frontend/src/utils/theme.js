
// 主题管理工具函数
// 负责主题切换和持久化存储

// 存储键
const THEME_KEY = 'digital_wellness_theme';

// 主题枚举
export const THEMES = {
  LIGHT: 'light',
  DARK: 'dark'
};

// 获取当前主题（默认深色）
export const getCurrentTheme = () => {
  const saved = localStorage.getItem(THEME_KEY);
  return saved || THEMES.DARK; // 默认使用深色主题
};

// 切换主题
export const switchTheme = (theme) => {
  // 更新 localStorage
  localStorage.setItem(THEME_KEY, theme);
  // 更新 HTML 标签的 data-theme 属性
  document.documentElement.setAttribute('data-theme', theme);
  // 如果使用 Ant Design 的 ConfigProvider，也需要同时更新
  return theme;
};

// 初始化主题（页面加载时调用）
export const initTheme = () => {
  const currentTheme = getCurrentTheme();
  document.documentElement.setAttribute('data-theme', currentTheme);
  return currentTheme;
};

// 切换到下一个主题
export const toggleTheme = () => {
  const current = getCurrentTheme();
  const next = current === THEMES.LIGHT ? THEMES.DARK : THEMES.LIGHT;
  return switchTheme(next);
};

