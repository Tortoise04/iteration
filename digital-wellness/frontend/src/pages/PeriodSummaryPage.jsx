import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, DatePicker, Select, message, Space, Popconfirm, Spin, Tag, Typography, Divider, Row, Col } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, RobotOutlined, EyeOutlined, DownOutlined, RightOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import request from '../utils/request';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

const { TextArea } = Input;
const { Option } = Select;
const { RangePicker } = DatePicker;
const { Title, Paragraph, Text } = Typography;

// Markdown 渲染组件
const MarkdownRenderer = ({ content }) => {
  if (!content) return <span style={{ color: 'var(--text-secondary)' }}>-</span>;

  return (
    <div className="markdown-body" style={{
      fontSize: '14px',
      lineHeight: '1.8',
    }}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          h1: ({ children }) => <h3 style={{ fontSize: '18px', marginTop: '16px', marginBottom: '8px', color: 'var(--text-primary)' }}>{children}</h3>,
          h2: ({ children }) => <h4 style={{ fontSize: '16px', marginTop: '14px', marginBottom: '8px', color: 'var(--text-primary)' }}>{children}</h4>,
          h3: ({ children }) => <h5 style={{ fontSize: '15px', marginTop: '12px', marginBottom: '6px', color: 'var(--text-primary)' }}>{children}</h5>,
          p: ({ children }) => <p style={{ marginBottom: '12px', color: 'var(--text-primary)' }}>{children}</p>,
          ul: ({ children }) => <ul style={{ marginBottom: '12px', paddingLeft: '24px' }}>{children}</ul>,
          ol: ({ children }) => <ol style={{ marginBottom: '12px', paddingLeft: '24px' }}>{children}</ol>,
          li: ({ children }) => <li style={{ marginBottom: '4px', color: 'var(--text-primary)' }}>{children}</li>,
          strong: ({ children }) => <strong style={{ color: 'var(--text-primary)' }}>{children}</strong>,
          em: ({ children }) => <em style={{ color: 'var(--text-primary)' }}>{children}</em>,
          blockquote: ({ children }) => (
            <blockquote style={{
              borderLeft: '4px solid #1890ff',
              paddingLeft: '16px',
              marginLeft: 0,
              color: 'var(--text-secondary)',
              backgroundColor: 'var(--bg-secondary)',
              padding: '8px 16px',
              borderRadius: '4px',
              marginBottom: '12px'
            }}>
              {children}
            </blockquote>
          ),
          code: ({ inline, children }) =>
            inline ? (
              <code style={{
                backgroundColor: 'var(--bg-secondary)',
                padding: '2px 6px',
                borderRadius: '4px',
                fontSize: '13px',
                color: '#e96900'
              }}>{children}</code>
            ) : (
              <code style={{
                display: 'block',
                backgroundColor: 'var(--bg-secondary)',
                padding: '12px 16px',
                borderRadius: '6px',
                fontSize: '13px',
                overflow: 'auto',
                marginBottom: '12px',
                border: '1px solid var(--border-color)'
              }}>{children}</code>
            ),
          hr: () => <Divider style={{ margin: '16px 0' }} />,
          table: ({ children }) => (
            <div style={{ overflowX: 'auto', marginBottom: '12px' }}>
              <table style={{
                width: '100%',
                borderCollapse: 'collapse',
                border: '1px solid var(--border-color)'
              }}>{children}</table>
            </div>
          ),
          th: ({ children }) => (
            <th style={{
              padding: '8px 12px',
              border: '1px solid var(--border-color)',
              backgroundColor: 'var(--bg-secondary)',
              textAlign: 'left',
              color: 'var(--text-primary)'
            }}>{children}</th>
          ),
          td: ({ children }) => (
            <td style={{
              padding: '8px 12px',
              border: '1px solid var(--border-color)',
              color: 'var(--text-primary)'
            }}>{children}</td>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

const PeriodSummaryPage = () => {
  const [form] = Form.useForm();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiModalVisible, setAiModalVisible] = useState(false);
  const [aiForm] = Form.useForm();
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState(null);
  const [expandedRowKeys, setExpandedRowKeys] = useState([]);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await request.get('/period-summaries');
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
    form.setFieldsValue({ periodType: 'WEEKLY' });
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingRecord(record);
    form.setFieldsValue({
      period: record.period,
      periodType: record.periodType,
      startDate: record.startDate ? dayjs(record.startDate) : null,
      endDate: record.endDate ? dayjs(record.endDate) : null,
      summary: record.summary,
      highlights: record.highlights,
      improvements: record.improvements,
      nextPlan: record.nextPlan
    });
    setModalVisible(true);
  };

  const handleViewDetail = (record) => {
    setCurrentRecord(record);
    setDetailVisible(true);
  };

  const handleSubmit = async (values) => {
    try {
      const payload = {
        period: values.period,
        periodType: values.periodType,
        startDate: values.startDate ? values.startDate.format('YYYY-MM-DD') : null,
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : null,
        summary: values.summary,
        highlights: values.highlights,
        improvements: values.improvements,
        nextPlan: values.nextPlan
      };

      if (editingRecord) {
        await request.put(`/period-summaries/${editingRecord.id}`, payload);
        message.success('更新成功');
      } else {
        await request.post('/period-summaries', payload);
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
      await request.delete(`/period-summaries/${id}`);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // AI 生成总结
  const handleOpenAiModal = () => {
    aiForm.resetFields();
    aiForm.setFieldsValue({
      dateRange: [dayjs().subtract(7, 'days'), dayjs()],
      periodType: 'WEEKLY'
    });
    setAiModalVisible(true);
  };

  const handleAiGenerate = async (values) => {
    setAiGenerating(true);
    const hideLoading = message.loading('AI 正在生成总结，请耐心等待（可能需要 30-60 秒）...', 0);

    try {
      const startDate = values.dateRange[0].format('YYYY-MM-DD');
      const endDate = values.dateRange[1].format('YYYY-MM-DD');

      const res = await request.post(`/period-summaries/generate?startDate=${startDate}&endDate=${endDate}&periodType=${values.periodType}`);

      hideLoading();
      message.success('AI 总结生成成功');
      setAiModalVisible(false);
      loadData();
    } catch (error) {
      hideLoading();
      const errorMsg = error.response?.data?.message || error.response?.data?.error || error.message || '请检查后端服务';
      message.error('AI 生成失败：' + errorMsg);
    } finally {
      setAiGenerating(false);
    }
  };

  // 可展开行渲染
  const expandedRowRender = (record) => {
    return (
      <div style={{ padding: '16px 24px', backgroundColor: 'var(--bg-secondary)', borderRadius: '8px' }}>
        <Row gutter={[24, 16]}>
          <Col span={24}>
            <div style={{ marginBottom: '8px' }}>
              <Text strong style={{ fontSize: '15px', color: 'var(--text-primary)' }}>📝 总结内容</Text>
            </div>
            <div style={{
              padding: '16px',
              backgroundColor: 'var(--bg-container)',
              borderRadius: '6px',
              border: '1px solid var(--border-color)'
            }}>
              <MarkdownRenderer content={record.summary} />
            </div>
          </Col>

          {record.highlights && (
            <Col span={24}>
              <div style={{ marginBottom: '8px' }}>
                <Text strong style={{ fontSize: '15px', color: '#52c41a' }}>✨ 亮点/成就</Text>
              </div>
              <div style={{
                padding: '16px',
                backgroundColor: 'var(--bg-container)',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
                borderLeft: '3px solid #52c41a'
              }}>
                <MarkdownRenderer content={record.highlights} />
              </div>
            </Col>
          )}

          {record.improvements && (
            <Col span={24}>
              <div style={{ marginBottom: '8px' }}>
                <Text strong style={{ fontSize: '15px', color: '#faad14' }}>🔧 改进计划</Text>
              </div>
              <div style={{
                padding: '16px',
                backgroundColor: 'var(--bg-container)',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
                borderLeft: '3px solid #faad14'
              }}>
                <MarkdownRenderer content={record.improvements} />
              </div>
            </Col>
          )}

          {record.nextPlan && (
            <Col span={24}>
              <div style={{ marginBottom: '8px' }}>
                <Text strong style={{ fontSize: '15px', color: '#1890ff' }}>🎯 下一步计划</Text>
              </div>
              <div style={{
                padding: '16px',
                backgroundColor: 'var(--bg-container)',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
                borderLeft: '3px solid #1890ff'
              }}>
                <MarkdownRenderer content={record.nextPlan} />
              </div>
            </Col>
          )}
        </Row>
      </div>
    );
  };

  const columns = [
    {
      title: '周期名称',
      dataIndex: 'period',
      key: 'period',
      width: 200
    },
    {
      title: '类型',
      dataIndex: 'periodType',
      key: 'periodType',
      width: 100,
      render: (t) => (
        <Tag color={t === 'WEEKLY' ? 'blue' : 'green'}>
          {t === 'WEEKLY' ? '周总结' : '月总结'}
        </Tag>
      )
    },
    {
      title: '日期范围',
      key: 'dateRange',
      width: 200,
      render: (_, record) => (
        <span>
          {record.startDate || '-'} ~ {record.endDate || '-'}
        </span>
      )
    },
    {
      title: '总结预览',
      dataIndex: 'summary',
      key: 'summary',
      ellipsis: { showTitle: false },
      render: (text) => (
        <span style={{ color: 'var(--text-secondary)' }}>
          {text ? (text.length > 50 ? text.substring(0, 50) + '...' : text) : '-'}
        </span>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card
        title="周期总结"
        extra={
          <Space>
            <Button icon={<RobotOutlined />} onClick={handleOpenAiModal}>AI 生成</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增总结</Button>
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
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys(keys),
            expandIcon: ({ expanded, onExpand, record }) =>
              expanded ? (
                <DownOutlined onClick={e => onExpand(record, e)} style={{ marginRight: 8 }} />
              ) : (
                <RightOutlined onClick={e => onExpand(record, e)} style={{ marginRight: 8 }} />
              ),
            columnWidth: 40
          }}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title={
          <span>
            📋 {currentRecord?.period}
            <Tag color={currentRecord?.periodType === 'WEEKLY' ? 'blue' : 'green'} style={{ marginLeft: 8 }}>
              {currentRecord?.periodType === 'WEEKLY' ? '周总结' : '月总结'}
            </Tag>
          </span>
        }
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>,
          <Button key="edit" type="primary" onClick={() => {
            setDetailVisible(false);
            handleEdit(currentRecord);
          }}>编辑</Button>
        ]}
        width={800}
      >
        {currentRecord && (
          <div>
            <Text type="secondary">
              📅 {currentRecord.startDate} ~ {currentRecord.endDate}
            </Text>
            <Divider />

            <Title level={5}>📝 总结内容</Title>
            <div style={{
              padding: '16px',
              backgroundColor: 'var(--bg-secondary)',
              borderRadius: '8px',
              marginBottom: '16px'
            }}>
              <MarkdownRenderer content={currentRecord.summary} />
            </div>

            {currentRecord.highlights && (
              <>
                <Title level={5} style={{ color: '#52c41a' }}>✨ 亮点/成就</Title>
                <div style={{
                  padding: '16px',
                  backgroundColor: 'var(--bg-secondary)',
                  borderRadius: '8px',
                  marginBottom: '16px',
                  borderLeft: '4px solid #52c41a'
                }}>
                  <MarkdownRenderer content={currentRecord.highlights} />
                </div>
              </>
            )}

            {currentRecord.improvements && (
              <>
                <Title level={5} style={{ color: '#faad14' }}>🔧 改进计划</Title>
                <div style={{
                  padding: '16px',
                  backgroundColor: 'var(--bg-secondary)',
                  borderRadius: '8px',
                  marginBottom: '16px',
                  borderLeft: '4px solid #faad14'
                }}>
                  <MarkdownRenderer content={currentRecord.improvements} />
                </div>
              </>
            )}

            {currentRecord.nextPlan && (
              <>
                <Title level={5} style={{ color: '#1890ff' }}>🎯 下一步计划</Title>
                <div style={{
                  padding: '16px',
                  backgroundColor: 'var(--bg-secondary)',
                  borderRadius: '8px',
                  borderLeft: '4px solid #1890ff'
                }}>
                  <MarkdownRenderer content={currentRecord.nextPlan} />
                </div>
              </>
            )}
          </div>
        )}
      </Modal>

      {/* 手动新增/编辑弹窗 */}
      <Modal title={editingRecord ? '编辑总结' : '新增总结'} open={modalVisible} onCancel={() => setModalVisible(false)} footer={null} width={700}>
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="period" label="周期名称" rules={[{ required: true, message: '请输入周期名称' }]}>
            <Input placeholder="如：2026年第17周" />
          </Form.Item>
          <Form.Item name="periodType" label="类型" rules={[{ required: true }]}>
            <Select>
              <Option value="WEEKLY">周总结</Option>
              <Option value="MONTHLY">月总结</Option>
            </Select>
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="startDate" label="开始日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="endDate" label="结束日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="summary" label="总结内容（支持 Markdown 格式）" rules={[{ required: true, message: '请输入总结内容' }]}>
            <TextArea rows={6} placeholder="支持 Markdown 格式，如：**加粗**、- 列表、# 标题等" />
          </Form.Item>
          <Form.Item name="highlights" label="亮点/成就">
            <TextArea rows={3} placeholder="本周/本月亮点" />
          </Form.Item>
          <Form.Item name="improvements" label="改进计划">
            <TextArea rows={3} placeholder="需要改进的地方" />
          </Form.Item>
          <Form.Item name="nextPlan" label="下一步计划">
            <TextArea rows={3} placeholder="下周/下月计划" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">{editingRecord ? '更新' : '保存'}</Button>
              <Button onClick={() => setModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* AI 生成弹窗 */}
      <Modal title="AI 生成周期总结" open={aiModalVisible} onCancel={() => setAiModalVisible(false)} footer={null}>
        <Spin spinning={aiGenerating} tip="AI 正在生成总结，请稍候...">
          <Form form={aiForm} onFinish={handleAiGenerate} layout="vertical">
            <Form.Item name="dateRange" label="日期范围" rules={[{ required: true }]}>
              <RangePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="periodType" label="总结类型" rules={[{ required: true }]}>
              <Select>
                <Option value="WEEKLY">周总结</Option>
                <Option value="MONTHLY">月总结</Option>
              </Select>
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={aiGenerating}>
                  开始生成
                </Button>
                <Button onClick={() => setAiModalVisible(false)}>取消</Button>
              </Space>
            </Form.Item>
          </Form>
        </Spin>
      </Modal>
    </div>
  );
};

export default PeriodSummaryPage;
