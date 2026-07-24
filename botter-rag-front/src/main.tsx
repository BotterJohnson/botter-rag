import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider, App as AntdApp } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <ConfigProvider
        locale={zhCN}
        theme={{
          token: {
            colorPrimary: '#0f766e',
            colorBgContainer: '#ffffff',
            colorBgElevated: '#ffffff',
            colorBorder: '#dfe5e2',
            colorBorderSecondary: '#edf0ee',
            borderRadius: 6,
            colorText: '#18211e',
            colorTextSecondary: '#66736e',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
          },
          components: {
            Menu: {
              itemBg: 'transparent',
              subMenuItemBg: 'transparent',
              itemSelectedBg: '#183d36',
              itemHoverBg: '#182822',
              itemSelectedColor: '#5eead4',
              itemColor: '#aebbb6',
            },
            Card: {
              colorBgContainer: '#ffffff',
            },
            Table: {
              colorBgContainer: '#ffffff',
              headerBg: '#f6f8f7',
              rowHoverBg: '#f4faf8',
            },
            Modal: {
              contentBg: '#ffffff',
              headerBg: '#ffffff',
            },
          },
        }}
      >
        <AntdApp>
          <App />
        </AntdApp>
      </ConfigProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
