import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, DatePicker, Select, message, Space, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
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
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
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
    </div>
  );
};

export default GoalPage;
