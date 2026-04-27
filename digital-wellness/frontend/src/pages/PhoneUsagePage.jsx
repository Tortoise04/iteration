import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, DatePicker, message, Space, Popconfirm, InputNumber, Select, Divider } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, AppstoreAddOutlined, SettingOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import request from '../utils/request';

const PhoneUsagePage = () => {
  const [form] = Form.useForm();
  const [presetForm] = Form.useForm();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [appPresets, setAppPresets] = useState([]);
  const [appDetails, setAppDetails] = useState([]);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [currentPhoneUsageId, setCurrentPhoneUsageId] = useState(null);
  const [presetModalVisible, setPresetModalVisible] = useState(false);
  const [editingPreset, setEditingPreset] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await request.get('/phone-usage');
      setData(res.data);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const loadAppPresets = async () => {
    try {
      const res = await request.get('/app-presets');
      setAppPresets(res.data || []);
    } catch (error) {
      console.error('加载应用预设失败', error);
    }
  };

  useEffect(() => {
    loadData();
    loadAppPresets();
  }, []);

  const handleAdd = () => {
    setEditingRecord(null);
    setAppDetails([]);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue({
      date: record.date ? dayjs(record.date) : null,
      usageTime: record.usageTime
    });
    setAppDetails(record.appDetails || []);
    setModalVisible(true);
  };

  const handleSubmit = async (values) => {
    try {
      const payload = {
        id: editingRecord?.id,
        date: values.date ? values.date.format('YYYY-MM-DD') : null,
        usageTime: values.usageTime,
        appDetails: appDetails.map(d => ({
          appName: d.appName,
          appPresetId: d.appPresetId,
          usageTime: d.usageTime
        }))
      };

      if (editingRecord) {
        await request.put(`/phone-usage/${editingRecord.id}`, payload);
        message.success('更新成功');
      } else {
        await request.post('/phone-usage', payload);
        message.success('新增成功');
      }
      setModalVisible(false);
      form.resetFields();
      setAppDetails([]);
      loadData();
    } catch (error) {
      message.error(editingRecord ? '更新失败' : '新增失败');
    }
  };

  const handleDelete = async (id) => {
    try {
      await request.delete(`/phone-usage/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleAddAppDetail = () => {
    setAppDetails([...appDetails, { key: Date.now(), appName: '', usageTime: 0 }]);
  };

  const handleRemoveAppDetail = (index) => {
    const newDetails = [...appDetails];
    newDetails.splice(index, 1);
    setAppDetails(newDetails);
  };

  const handleAppDetailChange = (index, field, value) => {
    const newDetails = [...appDetails];
    newDetails[index] = { ...newDetails[index], [field]: value };

    // 如果选择的是预设应用，自动填充名称
    if (field === 'appPresetId') {
      const preset = appPresets.find(p => p.id === value);
      if (preset) {
        newDetails[index].appName = preset.appName;
      }
    }

    setAppDetails(newDetails);
  };

  const handleViewDetails = (record) => {
    setCurrentPhoneUsageId(record.id);
    setDetailModalVisible(true);
  };

  const formatMinutes = (minutes) => {
    if (!minutes) return '0分钟';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0 && mins > 0) {
      return `${hours}小时${mins}分钟`;
    } else if (hours > 0) {
      return `${hours}小时`;
    } else {
      return `${mins}分钟`;
    }
  };

  const handleOpenPresetModal = () => {
    setEditingPreset(null);
    presetForm.resetFields();
    setPresetModalVisible(true);
  };

  const handleEditPreset = (preset) => {
    setEditingPreset(preset);
    presetForm.setFieldsValue({
      appName: preset.appName,
      sortOrder: preset.sortOrder,
      icon: preset.icon
    });
    setPresetModalVisible(true);
  };

  const handleSavePreset = async (values) => {
    try {
      if (editingPreset) {
        await request.put(`/app-presets/${editingPreset.id}`, values);
        message.success('更新预设成功');
      } else {
        await request.post('/app-presets', values);
        message.success('新增预设成功');
      }
      setPresetModalVisible(false);
      presetForm.resetFields();
      loadAppPresets();
    } catch (error) {
      message.error(editingPreset ? '更新预设失败' : '新增预设失败');
    }
  };

  const handleDeletePreset = async (id) => {
    try {
      await request.delete(`/app-presets/${id}`);
      message.success('删除预设成功');
      loadAppPresets();
    } catch (error) {
      message.error('删除预设失败');
    }
  };

  const expandedRowRender = (record) => {
    const detailColumns = [
      {
        title: '应用名称',
        dataIndex: 'appName',
        key: 'appName',
      },
      {
        title: '使用时长(分钟)',
        dataIndex: 'usageTime',
        key: 'usageTime',
        render: (text) => formatMinutes(text)
      },
    ];

    return (
      <Table
        columns={detailColumns}
        dataSource={record.appDetails || []}
        rowKey="id"
        pagination={false}
        size="small"
      />
    );
  };

  const columns = [
    {
      title: '日期',
      dataIndex: 'date',
      key: 'date',
      render: (text) => text ? dayjs(text).format('YYYY-MM-DD') : '-'
    },
    {
      title: '总使用时间',
      dataIndex: 'usageTime',
      key: 'usageTime',
      render: (text) => formatMinutes(text)
    },
    {
      title: '应用数量',
      key: 'appCount',
      render: (_, record) => record.appDetails?.length || 0
    },
    {
      title: '操作',
      key: 'action',
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

  const presetColumns = [
    {
      title: '应用名称',
      dataIndex: 'appName',
      key: 'appName',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
    },
    {
      title: '状态',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active) => active ? '启用' : '禁用'
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEditPreset(record)}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDeletePreset(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card 
        title="手机使用情况" 
        extra={
          <Space>
            <Button icon={<SettingOutlined />} onClick={handleOpenPresetModal}>管理预设</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增记录</Button>
          </Space>
        }
      >
        <Table
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          expandable={{
            expandedRowRender,
            rowExpandable: (record) => record.appDetails && record.appDetails.length > 0,
          }}
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑记录' : '新增记录'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); setAppDetails([]); }}
        footer={null}
        width={700}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="date" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="usageTime" label="总使用时间(分钟)" rules={[{ required: true, message: '请输入使用时间' }]}>
            <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入使用时间" />
          </Form.Item>

          <Divider orientation="left">
            应用使用详情
            <Button type="link" icon={<PlusOutlined />} onClick={handleAddAppDetail}>添加应用</Button>
          </Divider>

          {appDetails.map((detail, index) => (
            <div key={detail.key || detail.id || index} style={{ marginBottom: 16, padding: 12, background: '#fafafa', borderRadius: 4 }}>
              <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
                <Space>
                  <Select
                    placeholder="选择预设应用"
                    style={{ width: 150 }}
                    value={detail.appPresetId}
                    onChange={(value) => handleAppDetailChange(index, 'appPresetId', value)}
                    allowClear
                  >
                    {appPresets.map(preset => (
                      <Select.Option key={preset.id} value={preset.id}>{preset.appName}</Select.Option>
                    ))}
                  </Select>
                  <Input
                    placeholder="应用名称"
                    style={{ width: 150 }}
                    value={detail.appName}
                    onChange={(e) => handleAppDetailChange(index, 'appName', e.target.value)}
                  />
                  <InputNumber
                    placeholder="时长(分钟)"
                    style={{ width: 120 }}
                    min={0}
                    value={detail.usageTime}
                    onChange={(value) => handleAppDetailChange(index, 'usageTime', value)}
                  />
                </Space>
                <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleRemoveAppDetail(index)} />
              </Space>
            </div>
          ))}

          {appDetails.length === 0 && (
            <div style={{ textAlign: 'center', color: '#999', padding: 20 }}>
              暂无应用详情，点击上方"添加应用"按钮添加
            </div>
          )}

          <Form.Item style={{ marginTop: 24 }}>
            <Space>
              <Button type="primary" htmlType="submit">{editingRecord ? '更新' : '保存'}</Button>
              <Button onClick={() => { setModalVisible(false); setAppDetails([]); }}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="应用预设管理"
        open={presetModalVisible}
        onCancel={() => { setPresetModalVisible(false); setEditingPreset(null); }}
        footer={null}
        width={800}
      >
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingPreset(null); presetForm.resetFields(); }}>
            新增预设
          </Button>
        </div>
        <Table
          dataSource={appPresets}
          columns={presetColumns}
          rowKey="id"
          pagination={false}
          size="small"
        />
        
        <Divider />
        
        <Form form={presetForm} onFinish={handleSavePreset} layout="vertical">
          <Form.Item name="appName" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}>
            <Input placeholder="请输入应用名称" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} placeholder="数字越小越靠前" />
          </Form.Item>
          <Form.Item name="icon" label="图标URL">
            <Input placeholder="可选，输入图标URL" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">{editingPreset ? '更新' : '保存'}</Button>
              <Button onClick={() => { presetForm.resetFields(); setEditingPreset(null); }}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PhoneUsagePage;
