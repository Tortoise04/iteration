#!/usr/bin/env python3
"""
将 项目总结报告_SmartSchedule.md 中 2.1.2 主要流程部分
同步写入 项目总结报告_SmartSchedule.docx。
使用 mistune 解析 Markdown，python-docx 构建文档元素。
"""
import re
import json
import mistune
from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn


# ============================================================
# 1. 从 Markdown 提取 2.1.2 部分
# ============================================================
def extract_section_2_1_2(md_path):
    """提取 ### 2.1.2 主要流程 到 ## 2.2 系统实现 之间的内容"""
    with open(md_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    start = None
    end = None
    for i, line in enumerate(lines):
        if line.strip() == '### 2.1.2 主要流程':
            start = i + 1  # 跳过标题行
        if start is not None and line.strip() == '## 2.2 系统实现':
            end = i
            break

    if start is None or end is None:
        raise ValueError(f"找不到边界: start={start}, end={end}")

    content_lines = lines[start:end]
    # 去掉开头和结尾的空行
    while content_lines and content_lines[0].strip() == '':
        content_lines.pop(0)
    while content_lines and content_lines[-1].strip() == '':
        content_lines.pop()
    return ''.join(content_lines)


# ============================================================
# 2. Markdown → python-docx 转换器
# ============================================================
def add_table_to_doc(doc, headers, rows):
    """添加表格到文档"""
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = 'Table Grid'
    # 表头
    for j, h in enumerate(headers):
        cell = table.rows[0].cells[j]
        cell.text = ''
        p = cell.paragraphs[0]
        run = p.add_run(h)
        run.bold = True
        run.font.size = Pt(9)
        run.font.name = '宋体'
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    # 数据行
    for i, row in enumerate(rows):
        for j, val in enumerate(row):
            cell = table.rows[i + 1].cells[j]
            cell.text = ''
            p = cell.paragraphs[0]
            run = p.add_run(val)
            run.font.size = Pt(9)
            run.font.name = '宋体'
            run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    doc.add_paragraph()  # 表后空行


def add_styled_paragraph(doc, text, style='Normal', bold=False, font_size=10.5):
    """添加带样式的段落"""
    p = doc.add_paragraph(style=style)
    run = p.add_run(text)
    run.bold = bold
    run.font.size = Pt(font_size)
    run.font.name = '宋体'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    return p


def process_inline_text(paragraph, text):
    """处理行内格式：**粗体**、`代码`、普通文本"""
    # 先处理代码
    parts = re.split(r'(`[^`]+`)', text)
    for part in parts:
        if part.startswith('`') and part.endswith('`'):
            run = paragraph.add_run(part[1:-1])
            run.font.name = 'Consolas'
            run.font.size = Pt(9)
        else:
            # 处理粗体
            sub_parts = re.split(r'(\*\*[^*]+\*\*)', part)
            for sp in sub_parts:
                if sp.startswith('**') and sp.endswith('**'):
                    run = paragraph.add_run(sp[2:-2])
                    run.bold = True
                    run.font.size = Pt(10.5)
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
                elif sp:
                    run = paragraph.add_run(sp)
                    run.font.size = Pt(10.5)
                    run.font.name = '宋体'
                    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')


def parse_md_table(lines, start_idx):
    """解析 Markdown 表格，返回 (headers, rows, end_idx)"""
    # 第一行是表头
    header_line = lines[start_idx].strip()
    headers = [h.strip() for h in header_line.split('|')[1:-1]]

    # 第二行是分隔符，跳过
    rows = []
    i = start_idx + 2
    while i < len(lines):
        line = lines[i].strip()
        if line.startswith('|') and line.endswith('|'):
            cells = [c.strip() for c in line.split('|')[1:-1]]
            rows.append(cells)
            i += 1
        else:
            break
    return headers, rows, i


def convert_md_to_docx(md_text, doc):
    """将 Markdown 文本转换为 docx 元素并添加到文档"""
    lines = md_text.split('\n')
    i = 0
    while i < len(lines):
        line = lines[i].rstrip()
        stripped = line.strip()

        # 空行
        if stripped == '':
            i += 1
            continue

        # #### 四级标题 → 作为粗体段落（避免破坏 Heading 3 层级）
        if stripped.startswith('#### '):
            title_text = stripped[5:].strip()
            p = doc.add_paragraph()
            run = p.add_run(title_text)
            run.bold = True
            run.font.size = Pt(12)
            run.font.name = '宋体'
            run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            i += 1
            continue

        # 表格（以 | 开头）
        if stripped.startswith('|') and not stripped.startswith('|-'):
            # 检查下一行是否是分隔符
            if i + 1 < len(lines) and re.match(r'^\|[\s\-|]+\|$', lines[i + 1].strip()):
                headers, rows, next_i = parse_md_table(lines, i)
                add_table_to_doc(doc, headers, rows)
                i = next_i
                continue

        # 编号列表 1. 2. 3.
        num_match = re.match(r'^(\d+)\.\s+(.+)', stripped)
        if num_match:
            text = num_match.group(2)
            p = doc.add_paragraph(style='List Number')
            p.clear()
            process_inline_text(p, text)
            i += 1
            # 处理子列表（缩进的 - 开头行）
            while i < len(lines):
                sub = lines[i].rstrip()
                if re.match(r'^\s+-\s+', sub):
                    sub_text = re.sub(r'^\s+-\s+', '', sub)
                    sp = doc.add_paragraph(style='List Bullet 2')
                    sp.clear()
                    process_inline_text(sp, sub_text)
                    i += 1
                elif re.match(r'^\s{2,}-\s+', sub):
                    sub_text = re.sub(r'^\s{2,}-\s+', '', sub)
                    sp = doc.add_paragraph(style='List Bullet 2')
                    sp.clear()
                    process_inline_text(sp, sub_text)
                    i += 1
                elif re.match(r'^\s{4,}-\s+', sub):
                    sub_text = re.sub(r'^\s{4,}-\s+', '', sub)
                    sp = doc.add_paragraph(style='List Bullet 2')
                    sp.clear()
                    process_inline_text(sp, sub_text)
                    i += 1
                else:
                    break
            continue

        # 无序列表 - xxx
        bullet_match = re.match(r'^-\s+\*\*(.+)\*\*[：:]?\s*(.*)', stripped)
        if bullet_match:
            bold_part = bullet_match.group(1)
            rest = bullet_match.group(2)
            p = doc.add_paragraph(style='List Bullet')
            p.clear()
            run_b = p.add_run(bold_part)
            run_b.bold = True
            run_b.font.size = Pt(10.5)
            run_b.font.name = '宋体'
            run_b._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            if rest:
                run_r = p.add_run('：' + rest)
                run_r.font.size = Pt(10.5)
                run_r.font.name = '宋体'
                run_r._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            i += 1
            continue

        # 普通无序列表 - xxx
        bullet_match2 = re.match(r'^-\s+(.+)', stripped)
        if bullet_match2:
            text = bullet_match2.group(1)
            p = doc.add_paragraph(style='List Bullet')
            p.clear()
            process_inline_text(p, text)
            i += 1
            continue

        # **粗体标题** 开头的行 → 粗体段落
        bold_title_match = re.match(r'^\*\*(.+)\*\*$', stripped)
        if bold_title_match:
            text = bold_title_match.group(1)
            p = doc.add_paragraph()
            run = p.add_run(text)
            run.bold = True
            run.font.size = Pt(10.5)
            run.font.name = '宋体'
            run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
            i += 1
            continue

        # 普通段落
        p = doc.add_paragraph()
        process_inline_text(p, stripped)
        i += 1


# ============================================================
# 3. 主流程：替换 docx 中的 2.1.2 部分
# ============================================================
def main():
    md_path = '项目总结报告_SmartSchedule.md'
    docx_path = '项目总结报告_SmartSchedule.docx'

    # 提取 Markdown 中 2.1.2 的内容
    print("提取 Markdown 2.1.2 内容...")
    md_content = extract_section_2_1_2(md_path)
    print(f"  提取了 {len(md_content)} 个字符")

    # 打开现有 docx
    print("打开现有 docx...")
    doc = Document(docx_path)

    # 找到 2.1.2 和 2.2 的段落位置
    idx_212 = None
    idx_22 = None
    for i, p in enumerate(doc.paragraphs):
        if p.text.strip() == '2.1.2 主要流程':
            idx_212 = i
        if idx_212 is not None and p.text.strip() == '2.2 系统实现' and p.style.name.startswith('Heading'):
            idx_22 = i
            break

    if idx_212 is None:
        print("错误：找不到 2.1.2 主要流程 段落")
        return
    if idx_22 is None:
        print("错误：找不到 2.2 系统实现 段落")
        return

    print(f"  2.1.2 在段落 {idx_212}，2.2 在段落 {idx_22}")

    # 删除 2.1.2 标题之后、2.2 之前的旧内容段落
    # 同时检查并删除表格
    paras_to_delete = list(range(idx_212 + 1, idx_22))
    print(f"  删除 {len(paras_to_delete)} 个旧段落")

    # 从后往前删段落，避免索引偏移
    for p_idx in reversed(paras_to_delete):
        p = doc.paragraphs[p_idx]
        p._element.getparent().remove(p._element)

    # 删除表格（2.1.2 和 2.2 之间可能没有表格，但以防万一）
    # 表格的 XML 元素在 body 中，我们需要找到在 2.1.2 和 2.2 段落之间的表格
    body = doc.element.body
    elem_212 = doc.paragraphs[idx_212]._element
    elem_22 = None
    # 重新找 2.2（因为删了段落，索引变了，但 element 没变）
    for p in doc.paragraphs:
        if p.text.strip() == '2.2 系统实现' and p.style.name.startswith('Heading'):
            elem_22 = p._element
            break

    if elem_22 is not None:
        # 删除 elem_212 和 elem_22 之间的所有 tbl 元素
        tbls_to_remove = []
        in_range = False
        for child in body:
            if child is elem_212:
                in_range = True
                continue
            if child is elem_22:
                in_range = False
                continue
            if in_range and child.tag == qn('w:tbl'):
                tbls_to_remove.append(child)

        for tbl in tbls_to_remove:
            body.remove(tbl)
        print(f"  删除 {len(tbls_to_remove)} 个旧表格")

    # 在 2.1.2 段落后插入新内容
    # 找到 2.1.2 段落元素在 body 中的位置
    insert_after_elem = doc.paragraphs[idx_212]._element

    # 创建临时文档来生成新内容
    tmp_doc = Document()
    # 设置默认字体
    style = tmp_doc.styles['Normal']
    font = style.font
    font.name = '宋体'
    font.size = Pt(10.5)
    style.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

    convert_md_to_docx(md_content, tmp_doc)

    # 将临时文档的元素移动到主文档
    # 把 tmp_doc 的 body 中的所有子元素插入到 insert_after_elem 之后
    body = doc.element.body
    tmp_body = tmp_doc.element.body

    insert_pos = list(body).index(insert_after_elem) + 1
    new_elements = list(tmp_body)
    for elem in reversed(new_elements):
        body.insert(insert_pos, elem)

    print(f"  插入 {len(new_elements)} 个新元素")

    # 保存（先尝试原文件名，如被占用则用临时文件名）
    output_path = '项目总结报告_SmartSchedule.docx'
    tmp_path = '项目总结报告_SmartSchedule_更新版.docx'
    try:
        doc.save(output_path)
        print(f"已保存: {output_path}")
    except PermissionError:
        doc.save(tmp_path)
        print(f"原文件被占用，已保存到: {tmp_path}")
        print("请关闭 Word 后，手动用此文件覆盖原文件。")


if __name__ == '__main__':
    main()
