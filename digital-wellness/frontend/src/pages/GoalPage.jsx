import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, DatePicker, Select, message, Space, Popconfirm, Spin } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, RobotOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import request from '../utils/request';

const { TextArea } = Input;
const { Option } = Select;

const GoalPage = () => {
  const [form] = Form.useForm();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingGoal, setEditingGoal] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiModalVisible, setAiModalVisible] = useState(false);
  const [currentGoal, setCurrentGoal] = useState(null);

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const res = await request.get('/goals');
      setData(res.data);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 打开新增弹窗
  const handleAdd = () => {
    setEditingGoal(null);
    form.resetFields();
    form.setFieldsValue({ status: 'IN_PROGRESS' });
    setModalVisible(true);
  };

  // 打开编辑弹窗
  const handleEdit = (record) => {
    setEditingGoal(record);
    form.setFieldsValue({
      goal: record.goal,
      description: record.description,
      startTime: record.startTime ? dayjs(record.startTime) : null,
      endTime: record.endTime ? dayjs(record.endTime) : null,
      status: record.status || 'IN_PROGRESS'
    });
    setModalVisible(true);
  };

  // 提交表单（新增/编辑）
  const handleSubmit = async (values) => {
    try {
      const payload = {
        goal: values.goal,
        description: values.description,
        startTime: values.startTime ? values.startTime.toISOString() : null,
        endTime: values.endTime ? values.endTime.toISOString() : null,
        status: values.status || 'IN_PROGRESS'
      };

      if (editingGoal) {
        // 编辑：使用 PUT（如果没有 PUT 接口，后端可能需要添加）
        await request.put(`/goals/${editingGoal.id}`, payload);
        message.success('更新成功');
      } else {
        // 新增
        await request.post('/goals', payload);
        message.success('新增成功');
      }
      setModalVisible(false);
      form.resetFields();
      loadData();
    } catch (error) {
      message.error(editingGoal ? '更新失败' : '新增失败');
    }
  };

  // 删除
  const handleDelete = async (id) => {
    try {
      await request.delete(`/goals/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // AI 目标拆解
  const handleAiBreakdown = (record) => {
    setCurrentGoal(record);
    setAiModalVisible(true);
  };

  // 执行 AI 拆解
  const executeAiBreakdown = async () => {
    if (!currentGoal) return;
    
    setAiLoading(true);
    try {
      const res = await request.post(`/goals/${currentGoal.id}/ai-breakdown`);
      message.success(`成功生成 ${res.data.length} 个每日子任务`);
      setAiModalVisible(false);
    } catch (error) {
      message.error('AI 拆解失败，请重试');
      console.error('AI 拆解失败:', error);
    } finally {
      setAiLoading(false);
    }
  };

  // 表格列定义
  const columns = [
    {
      title: '目标',
      dataIndex: 'goal',
      key: 'goal',
      width: 200
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 250,
      ellipsis: true
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 150,
      render: (text) => text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 150,
      render: (text) => text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const statusMap = {
          'IN_PROGRESS': { text: '进行中', color: '#1890ff' },
          'COMPLETED': { text: '已完成', color: '#52c41a' },
          'CANCELLED': { text: '已取消', color: '#ff4d4f' }
        };
        const s = statusMap[status] || { text: status, color: '#666' };
        return <span style={{ color: s.color }}>{s.text}</span>;
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<RobotOutlined />}
            onClick={() => handleAiBreakdown(record)}
          >
            AI 拆解
          </Button>
          <Popconfirm
            title="确定要删除这个目标吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card
        title="目标管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增目标
          </Button>
        }
      >
        <Table
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingGoal ? '编辑目标' : '新增目标'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          onFinish={handleSubmit}
          layout="vertical"
        >
          <Form.Item
            name="goal"
            label="目标"
            rules={[{ required: true, message: '请输入目标' }]}
          >
            <Input placeholder="请输入目标" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <TextArea rows={3} placeholder="请输入目标详细描述（可选）" />
          </Form.Item>

          <Form.Item
            name="startTime"
            label="开始时间"
            rules={[{ required: true, message: '请选择开始时间' }]}
          >
            <DatePicker
              showTime
              style={{ width: '100%' }}
              placeholder="请选择开始时间"
              format="YYYY-MM-DD HH:mm"
            />
          </Form.Item>

          <Form.Item
            name="endTime"
            label="结束时间"
            rules={[{ required: true, message: '请选择结束时间' }]}
          >
            <DatePicker
              showTime
              style={{ width: '100%' }}
              placeholder="请选择结束时间"
              format="YYYY-MM-DD HH:mm"
            />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="IN_PROGRESS">进行中</Option>
              <Option value="COMPLETED">已完成</Option>
              <Option value="CANCELLED">已取消</Option>
            </Select>
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingGoal ? '更新' : '保存'}
              </Button>
              <Button onClick={() => {
                setModalVisible(false);
                form.resetFields();
              }}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* AI 拆解模态框 */}
      <Modal
        title="AI 目标拆解"
        open={aiModalVisible}
        onCancel={() => setAiModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setAiModalVisible(false)}>
            取消
          </Button>,
          <Button
            key="confirm"
            type="primary"
            loading={aiLoading}
            onClick={executeAiBreakdown}
            disabled={aiLoading}
          >
            开始拆解
          </Button>
        ]}
        width={500}
      >
        <div style={{ padding: '20px 0' }}>
          {aiLoading ? (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <Spin size="large" tip="AI 正在分析目标..." />
            </div>
          ) : (
            <div>
              <h4>目标信息</h4>
              <p><strong>目标：</strong>{currentGoal?.goal}</p>
              {currentGoal?.description && (
                <p><strong>描述：</strong>{currentGoal.description}</p>
              )}
              <p><strong>时间范围：</strong>{currentGoal?.startTime ? dayjs(currentGoal.startTime).format('YYYY-MM-DD') : ''} 至 {currentGoal?.endTime ? dayjs(currentGoal.endTime).format('YYYY-MM-DD') : ''}</p>
              <div style={{ marginTop: '20px', padding: '15px', background: '#f5f5f5', borderRadius: '4px' }}>
                <p>AI 将根据目标内容和时间范围，自动生成合理的每日执行计划。</p>
                <p style={{ marginTop: '10px' }}>生成的子任务将自动保存到系统中，您可以在仪表盘查看今日待办。</p>
              </div>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};

export default GoalPage;
