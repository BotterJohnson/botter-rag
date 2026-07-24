import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, App } from 'antd';
import { UserOutlined, LockOutlined, RobotOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { authApi } from '@/api';
import { useAuthStore } from '@/store/useAuthStore';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { message } = App.useApp();
  const setAuth = useAuthStore((s) => s.setAuth);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const res = await authApi.login(values);
      const token = res.data.data;

      if (res.data.code !== 200 || typeof token !== 'string' || !token.trim()) {
        throw new Error(res.data.message || '登录失败，请检查用户名和密码');
      }

      setAuth(
        { userId: 0, username: values.username, departmentId: '', role: '', token },
        token,
      );
      message.success('登录成功');
      navigate('/chat', { replace: true });
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-surface min-h-screen grid grid-cols-[minmax(0,1fr)] lg:grid-cols-[minmax(320px,1fr)_520px]">
      <section className="hidden lg:flex flex-col justify-between p-12 xl:p-16 text-white">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-md bg-primary-700 flex items-center justify-center">
            <RobotOutlined className="text-xl" />
          </div>
          <div>
            <div className="text-xl font-semibold">Botter</div>
            <div className="text-[10px] uppercase text-[#82918b]">Knowledge AI</div>
          </div>
        </div>

        <div className="max-w-xl pb-8">
          <div className="mb-6 h-px w-12 bg-amber-500" />
          <h1 className="text-4xl xl:text-5xl font-semibold leading-tight">
            企业知识，随问随用
          </h1>
          <p className="mt-5 max-w-md text-base leading-7 text-[#9caaa5]">
            连接团队知识库，在一个工作空间中完成检索、问答与质量评估。
          </p>
        </div>

        <div className="text-xs text-[#68766f]">Botter RAG · Enterprise Workspace</div>
      </section>

      <section className="min-w-0 flex min-h-screen items-center bg-[#f3f5f4] px-5 py-10 sm:px-12 lg:px-16">
        <div className="min-w-0 w-full max-w-sm mx-auto animate-fade-in">
          <div className="flex items-center gap-3 mb-10 lg:hidden">
            <div className="w-9 h-9 rounded-md bg-primary-700 text-white flex items-center justify-center">
              <RobotOutlined />
            </div>
            <span className="text-xl font-semibold text-dark-text">Botter</span>
          </div>

          <div className="mb-8">
            <div className="text-xs font-semibold uppercase text-primary-700 mb-3">Secure access</div>
            <h2 className="text-3xl font-semibold text-dark-text">登录工作空间</h2>
            <p className="text-sm text-dark-muted mt-2">使用您的账号继续访问 Botter</p>
          </div>

          <Form name="login" onFinish={onFinish} size="large" layout="vertical" autoComplete="off">
            <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input prefix={<UserOutlined className="text-gray-400" />} placeholder="请输入用户名" autoFocus />
            </Form.Item>
            <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password prefix={<LockOutlined className="text-gray-400" />} placeholder="请输入密码" />
            </Form.Item>
            <Form.Item className="!mt-7 !mb-5">
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                iconPosition="end"
                icon={<ArrowRightOutlined />}
                className="h-11 !bg-primary-700 hover:!bg-primary-600 !border-none font-medium"
              >
                登录
              </Button>
            </Form.Item>
          </Form>

          <div className="border-t border-gray-200 pt-5 text-xs text-dark-muted leading-6">
            <div>演示账号：<span className="font-medium text-dark-text">admin</span> / demo123</div>
            <div>部门账号：hr001 或 tech001 / demo123</div>
          </div>
        </div>
      </section>
    </main>
  );
}
