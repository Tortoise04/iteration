import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, DatePicker, Select, message, Space, Popconfirm, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import request from '../utils/request';

const { TextArea } = Input;
const { Option } = Select;

const categoryColors = {
  LEARNING: 'blue',
  WORK: 'green',
  HEALTH: 'orange',
  LIFE: 'purple'
};

const categoryLabels = {
  LEARNING: '学习',
  WORK: '工作',
  HEALTH: '健康',
  LIFE: '生活'
};

const AchievementPage = () => {
  const [form] = Form.useForm();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await request.get('/achievements');
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

  const handleAdd = () => {
    setEditingRecord(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue({
      achievement: record.achievement,
      description: record.description,
      category: record.category,
      time: record.time ? dayjs(record.time) : null
    });
    setModalVisible(true);
  };

  const handleSubmit = async (values) => {
    try {
      const payload = {
        achievement: values.achievement,
        description: values.description,
        category: values.category,
        time: values.time ? values.time.toISOString() : null
      };

      if (editingRecord) {
        await request.put(`/achievements/${editingRecord.id}`, payload);
        message.success('更新成功');
      } else {
        await request.post('/achievements', payload);
        message.success('新增成功');
      }
      setModalVisible(false);
      form.resetFields();
      loadData();
    } catch (error) {
      message.error(editingRecord ? '更新失败' : '新增失败');
    }
  };

  const handleDelete = async (id) => {
    try {
      await request.delete(`/achievements/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const columns = [
    { title: '成果', dataIndex: 'achievement', key: 'achievement' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: '分类', dataIndex: 'category', key: 'category',
      render: (cat) => cat ? <Tag color={categoryColors[cat]}>{categoryLabels[cat] || cat}</Tag> : '-'
    },
    { title: '时间', dataIndex: 'time', key: 'time', render: (text) => text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '-' },
    {
      title: '操作', key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card title="成果记录" extra={<Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增成果</Button>}>
        <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
      </Card>

      <Modal title={editingRecord ? '编辑成果' : '新增成果'} open={modalVisible} onCancel={() => setModalVisible(false)} footer={null} width={600}>
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="achievement" label="成果" rules={[{ required: true, message: '请输入成果' }]}>
            <Input placeholder="请输入成果" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="请输入成果详细描述" />
          </Form.Item>
          <Form.Item name="category" label="分类">
            <Select placeholder="请选择分类" allowClear>
              <Option value="LEARNING">学习</Option>
              <Option value="WORK">工作</Option>
              <Option value="HEALTH">健康</Option>
              <Option value="LIFE">生活</Option>
            </Select>
          </Form.Item>
          <Form.Item name="time" label="时间" rules={[{ required: true, message: '请选择时间' }]}>
            <DatePicker showTime style={{ width: '100%' }} format="YYYY-MM-DD HH:mm" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">{editingRecord ? '更新' : '保存'}</Button>
              <Button onClick={() => setModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AchievementPage;
