import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, DatePicker, message, Space, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import request from '../utils/request';

const { TextArea } = Input;

const DailyActivityPage = () => {
  const [form] = Form.useForm();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await request.get('/daily-activities');
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
      date: record.date ? dayjs(record.date) : null,
      activity: record.activity,
      duration: record.duration,
      location: record.location
    });
    setModalVisible(true);
  };

  const handleSubmit = async (values) => {
    try {
      const payload = {
        date: values.date ? values.date.format('YYYY-MM-DD') : null,
        activity: values.activity,
        duration: values.duration,
        location: values.location
      };

      if (editingRecord) {
        await request.put(`/daily-activities/${editingRecord.id}`, payload);
        message.success('更新成功');
      } else {
        await request.post('/daily-activities', payload);
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
      await request.delete(`/daily-activities/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const columns = [
    { title: '日期', dataIndex: 'date', key: 'date', render: (text) => text ? dayjs(text).format('YYYY-MM-DD') : '-' },
    { title: '活动内容', dataIndex: 'activity', key: 'activity' },
    { title: '时长(分钟)', dataIndex: 'duration', key: 'duration' },
    { title: '地点', dataIndex: 'location', key: 'location' },
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
      <Card title="每日活动" extra={<Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增活动</Button>}>
        <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
      </Card>

      <Modal title={editingRecord ? '编辑活动' : '新增活动'} open={modalVisible} onCancel={() => setModalVisible(false)} footer={null} width={600}>
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="activity" label="活动内容" rules={[{ required: true, message: '请输入活动内容' }]}>
            <TextArea rows={3} placeholder="请输入活动内容" />
          </Form.Item>
          <Form.Item name="duration" label="时长(分钟)">
            <Input type="number" placeholder="请输入时长" />
          </Form.Item>
          <Form.Item name="location" label="地点">
            <Input placeholder="请输入地点" />
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

export default DailyActivityPage;
