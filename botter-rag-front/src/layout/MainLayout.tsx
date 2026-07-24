import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Button, Tooltip } from 'antd';
import {
  MessageOutlined,
  DatabaseOutlined,
  BarChartOutlined,
  DashboardOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/store/useAuthStore';
import { authApi } from '@/api';

const { Sider, Content, Header } = Layout;

const menuItems = [
  { key: '/chat', icon: <MessageOutlined />, label: '智能问答' },
  { key: '/kb', icon: <DatabaseOutlined />, label: '知识库' },
  { key: '/eval', icon: <BarChartOutlined />, label: '效果评估' },
  { key: '/dashboard', icon: <DashboardOutlined />, label: '用量监控' },
];

const pageTitles: Record<string, string> = {
  '/chat': '智能问答',
  '/kb': '知识库',
  '/eval': '效果评估',
  '/dashboard': '用量监控',
};

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(() => window.innerWidth < 1024);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();

  const selectedKey = '/' + location.pathname.split('/')[1];

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Clear local auth even if the server session has expired.
    }
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Layout className="h-screen bg-dark-bg">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={232}
        collapsedWidth={72}
        className="!bg-[#101916] border-r border-[#26332f]"
      >
        <div className={`h-16 flex items-center border-b border-[#26332f] ${collapsed ? 'justify-center' : 'px-5'}`}>
          <div className="w-8 h-8 rounded-md bg-primary-700 flex items-center justify-center flex-shrink-0">
            <RobotOutlined className="text-base text-white" />
          </div>
          {!collapsed && (
            <div className="ml-3 min-w-0">
              <div className="text-white text-base font-semibold leading-5">Botter</div>
              <div className="text-[10px] text-[#82918b] uppercase">Knowledge AI</div>
            </div>
          )}
        </div>

        {!collapsed && (
          <div className="px-5 pt-5 pb-2 text-[10px] font-semibold uppercase text-[#68766f]">
            Workspace
          </div>
        )}
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          inlineIndent={20}
          className="!bg-transparent !border-none px-2"
        />

        <div className="absolute bottom-0 left-0 right-0 border-t border-[#26332f] p-3">
          <div className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3 px-2'}`}>
            <Avatar size={32} icon={<UserOutlined />} className="!bg-[#25443d] !text-primary-300" />
            {!collapsed && (
              <div className="min-w-0 flex-1">
                <div className="truncate text-xs font-medium text-[#d9e2de]">{user?.username || 'User'}</div>
                <div className="truncate text-[10px] text-[#82918b]">{user?.role || 'Member'}</div>
              </div>
            )}
          </div>
        </div>
      </Sider>

      <Layout className="min-w-0">
        <Header className="h-16 !leading-[64px] flex items-center justify-between px-4 sm:px-6 border-b border-[#dfe5e2] !bg-white">
          <div className="flex items-center gap-3 min-w-0">
            <Tooltip title={collapsed ? '展开导航' : '收起导航'}>
              <Button
                type="text"
                aria-label={collapsed ? '展开导航' : '收起导航'}
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed(!collapsed)}
                className="!text-gray-500"
              />
            </Tooltip>
            <div className="h-5 w-px bg-gray-200 hidden sm:block" />
            <span className="text-sm font-semibold text-dark-text truncate">
              {pageTitles[selectedKey] || 'Botter'}
            </span>
          </div>

          <Dropdown
            menu={{
              items: [
                {
                  key: 'info',
                  label: (
                    <div className="px-2 py-1 min-w-36">
                      <div className="font-medium text-gray-800">{user?.username}</div>
                      <div className="text-xs text-gray-500">{user?.departmentId || 'ALL'} · {user?.role || 'MEMBER'}</div>
                    </div>
                  ),
                  disabled: true,
                },
                { type: 'divider' },
                { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout, danger: true },
              ],
            }}
            placement="bottomRight"
          >
            <Button type="text" className="!h-10 !px-2">
              <span className="flex items-center gap-2">
                <span className="hidden sm:inline text-sm text-gray-600">{user?.username}</span>
                <Avatar size={30} icon={<UserOutlined />} className="!bg-primary-700" />
              </span>
            </Button>
          </Dropdown>
        </Header>

        <Content className="app-content overflow-auto p-4 sm:p-6 lg:p-7">
          <div className="mx-auto h-full w-full max-w-[1600px]">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
