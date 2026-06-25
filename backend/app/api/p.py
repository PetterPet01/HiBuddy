from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import cm, mm
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, HRFlowable, KeepTogether
)
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus.flowables import Flowable
import copy

# ── Fonts ──────────────────────────────────────────────────────────────
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

pdfmetrics.registerFont(
    TTFont("DV", r"C:\Windows\Fonts\arial.ttf")
)
pdfmetrics.registerFont(
    TTFont("DV-B", r"C:\Windows\Fonts\arialbd.ttf")
)
pdfmetrics.registerFont(
    TTFont("DV-I", r"C:\Windows\Fonts\ariali.ttf")
)
pdfmetrics.registerFont(
    TTFont("DV-BI", r"C:\Windows\Fonts\arialbi.ttf")
)
pdfmetrics.registerFont(
    TTFont("DV-M", r"C:\Windows\Fonts\cour.ttf")
)
# ── Color Palette ───────────────────────────────────────────────────────
C_NAVY    = colors.HexColor('#1a2e4a')
C_BLUE    = colors.HexColor('#2563a8')
C_LBLUE   = colors.HexColor('#dbeafe')
C_TEAL    = colors.HexColor('#0f766e')
C_LTEAL   = colors.HexColor('#ccfbf1')
C_RED     = colors.HexColor('#dc2626')
C_LRED    = colors.HexColor('#fee2e2')
C_AMBER   = colors.HexColor('#b45309')
C_LAMBER  = colors.HexColor('#fef3c7')
C_PURPLE  = colors.HexColor('#7c3aed')
C_LPURPLE = colors.HexColor('#ede9fe')
C_GRAY    = colors.HexColor('#374151')
C_LGRAY   = colors.HexColor('#f3f4f6')
C_MGRAY   = colors.HexColor('#d1d5db')
C_WHITE   = colors.white
C_CODE_BG = colors.HexColor('#1e293b')
C_CODE_FG = colors.HexColor('#e2e8f0')

W, H = A4

# ── Custom Flowables ────────────────────────────────────────────────────
class ColorBox(Flowable):
    """Colored background box for section headers."""
    def __init__(self, text, bg=C_NAVY, fg=C_WHITE, size=14, pad=8, radius=4):
        super().__init__()
        self.text = text; self.bg = bg; self.fg = fg
        self.size = size; self.pad = pad; self.radius = radius
        self.width = W - 4*cm
        self.height = size + pad*2 + 2

    def draw(self):
        c = self.canv
        c.setFillColor(self.bg)
        c.roundRect(0, 0, self.width, self.height, self.radius, fill=1, stroke=0)
        c.setFillColor(self.fg)
        c.setFont('DV-B', self.size)
        c.drawString(self.pad, self.pad + 2, self.text)

class StepBox(Flowable):
    """Numbered step indicator."""
    def __init__(self, num, text, color=C_BLUE):
        super().__init__()
        self.num = str(num); self.text = text; self.color = color
        self.width = W - 4*cm; self.height = 24

    def draw(self):
        c = self.canv
        # Circle
        c.setFillColor(self.color)
        c.circle(12, 10, 11, fill=1, stroke=0)
        c.setFillColor(C_WHITE)
        c.setFont('DV-B', 9)
        c.drawCentredString(12, 6.5, self.num)
        # Text
        c.setFillColor(C_GRAY)
        c.setFont('DV-B', 10)
        c.drawString(30, 6.5, self.text)

class InfoBox(Flowable):
    """Colored info/warning/tip box."""
    def __init__(self, lines, color=C_BLUE, bg=C_LBLUE, icon='ℹ', width=None):
        super().__init__()
        self.lines = lines if isinstance(lines, list) else [lines]
        self.color = color; self.bg = bg; self.icon = icon
        self.box_width = width or (W - 4*cm)
        line_h = 14
        self.height = max(32, len(self.lines) * line_h + 14)

    def draw(self):
        c = self.canv
        c.setFillColor(self.bg)
        c.roundRect(0, 0, self.box_width, self.height, 4, fill=1, stroke=0)
        c.setStrokeColor(self.color)
        c.setLineWidth(2.5)
        c.line(0, 0, 0, self.height)
        c.setFillColor(self.color)
        c.setFont('DV-B', 10)
        c.drawString(8, self.height - 14, self.icon)
        c.setFont('DV', 9)
        y = self.height - 14
        for i, line in enumerate(self.lines):
            c.setFillColor(C_GRAY)
            c.setFont('DV', 9 if i > 0 else 9)
            if i > 0: y -= 13
            c.drawString(22, y, line)

class SectionDivider(Flowable):
    """Section number + title decorative divider."""
    def __init__(self, num, title, color=C_BLUE):
        super().__init__()
        self.num = num; self.title = title; self.color = color
        self.width = W - 4*cm; self.height = 28

    def draw(self):
        c = self.canv
        # Number badge
        c.setFillColor(self.color)
        c.roundRect(0, 2, 26, 24, 3, fill=1, stroke=0)
        c.setFillColor(C_WHITE)
        c.setFont('DV-B', 11)
        c.drawCentredString(13, 7, self.num)
        # Title
        c.setFillColor(C_NAVY)
        c.setFont('DV-B', 12)
        c.drawString(34, 8, self.title)
        # Line
        c.setStrokeColor(self.color)
        c.setLineWidth(1)
        c.line(0, 1, self.width, 1)

# ── Styles ──────────────────────────────────────────────────────────────
def make_styles():
    return {
        'normal': ParagraphStyle('N', fontName='DV', fontSize=9, leading=14,
                                 textColor=C_GRAY, spaceAfter=3),
        'bold':   ParagraphStyle('B', fontName='DV-B', fontSize=9, leading=14,
                                 textColor=C_NAVY),
        'h1':     ParagraphStyle('H1', fontName='DV-B', fontSize=15, leading=20,
                                 textColor=C_WHITE, spaceAfter=4),
        'h2':     ParagraphStyle('H2', fontName='DV-B', fontSize=12, leading=16,
                                 textColor=C_NAVY, spaceBefore=10, spaceAfter=4),
        'h3':     ParagraphStyle('H3', fontName='DV-B', fontSize=10, leading=14,
                                 textColor=C_BLUE, spaceBefore=6, spaceAfter=3),
        'code':   ParagraphStyle('C', fontName='DV-M', fontSize=8, leading=12,
                                 textColor=C_CODE_FG, backColor=C_CODE_BG,
                                 leftIndent=8, rightIndent=8,
                                 spaceBefore=2, spaceAfter=2,
                                 borderPad=4),
        'label':  ParagraphStyle('L', fontName='DV-B', fontSize=8, leading=12,
                                 textColor=C_WHITE),
        'small':  ParagraphStyle('S', fontName='DV', fontSize=8, leading=12,
                                 textColor=C_GRAY),
        'bullet': ParagraphStyle('BU', fontName='DV', fontSize=9, leading=13,
                                 textColor=C_GRAY, leftIndent=14,
                                 bulletIndent=4, bulletFontName='DV',
                                 spaceAfter=2),
        'bullet2':ParagraphStyle('BU2', fontName='DV', fontSize=8.5, leading=13,
                                 textColor=C_GRAY, leftIndent=26,
                                 bulletIndent=16, bulletFontName='DV',
                                 spaceAfter=1),
        'mono':   ParagraphStyle('MN', fontName='DV-M', fontSize=8, leading=12,
                                 textColor=C_NAVY, leftIndent=14,
                                 spaceAfter=1),
        'tag':    ParagraphStyle('TG', fontName='DV-B', fontSize=8,
                                 textColor=C_TEAL),
    }

S = make_styles()
sp = lambda n=4: Spacer(1, n)
hr = lambda c=C_MGRAY, t=0.5: HRFlowable(width='100%', thickness=t, color=c, spaceAfter=4, spaceBefore=4)
def P(txt, s='normal'): return Paragraph(txt, S[s])
def B(txt): return Paragraph(f'<font name="DV-B" color="{C_NAVY.hexval()}">{txt}</font>', S['normal'])
def code(txt): return Paragraph(txt.replace('\n','<br/>').replace(' ','&nbsp;'), S['code'])

def bullet(txt, level=1):
    style = S['bullet'] if level==1 else S['bullet2']
    sym   = '•' if level==1 else '–'
    return Paragraph(f'{sym}&nbsp;&nbsp;{txt}', style)

def kv_table(rows, col1=5.5*cm, col2=None):
    """Key-value 2-col table."""
    avail = W - 4*cm
    c2 = col2 or (avail - col1)
    data = [[Paragraph(f'<font name="DV-B" color="#1a2e4a">{k}</font>', S['small']),
             Paragraph(v, S['small'])] for k,v in rows]
    t = Table(data, colWidths=[col1, c2])
    t.setStyle(TableStyle([
        ('ROWBACKGROUNDS', (0,0), (-1,-1), [C_LGRAY, C_WHITE]),
        ('TOPPADDING',    (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('LEFTPADDING',   (0,0), (-1,-1), 6),
        ('VALIGN',        (0,0), (-1,-1), 'TOP'),
        ('GRID',          (0,0), (-1,-1), 0.3, C_MGRAY),
    ]))
    return t

def header_table(headers, rows, col_w=None):
    """Full-width table with colored header."""
    avail = W - 4*cm
    n = len(headers)
    cw = col_w or [avail/n]*n
    hdr = [Paragraph(f'<font name="DV-B" color="white">{h}</font>', S['small']) for h in headers]
    body = [[Paragraph(str(c), S['small']) for c in row] for row in rows]
    t = Table([hdr]+body, colWidths=cw)
    t.setStyle(TableStyle([
        ('BACKGROUND',    (0,0), (-1,0),  C_NAVY),
        ('ROWBACKGROUNDS',(0,1), (-1,-1), [C_WHITE, C_LGRAY]),
        ('TOPPADDING',    (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('LEFTPADDING',   (0,0), (-1,-1), 6),
        ('VALIGN',        (0,0), (-1,-1), 'TOP'),
        ('GRID',          (0,0), (-1,-1), 0.3, C_MGRAY),
    ]))
    return t

def code_block(lines, title=None):
    """Dark-themed code block."""
    story = []
    if title:
        t = Table([[Paragraph(f'<font name="DV-B" size="8" color="white">{title}</font>', S['small'])]],
                  colWidths=[W-4*cm])
        t.setStyle(TableStyle([
            ('BACKGROUND',   (0,0),(-1,-1), C_BLUE),
            ('LEFTPADDING',  (0,0),(-1,-1), 8),
            ('TOPPADDING',   (0,0),(-1,-1), 4),
            ('BOTTOMPADDING',(0,0),(-1,-1), 4),
        ]))
        story.append(t)
    lines_txt = '<br/>'.join(l.replace(' ','&nbsp;').replace('<','&lt;').replace('>','&gt;') for l in lines)
    t2 = Table([[Paragraph(lines_txt, S['code'])]], colWidths=[W-4*cm])
    t2.setStyle(TableStyle([
        ('BACKGROUND',   (0,0),(-1,-1), C_CODE_BG),
        ('LEFTPADDING',  (0,0),(-1,-1), 0),
        ('RIGHTPADDING', (0,0),(-1,-1), 0),
        ('TOPPADDING',   (0,0),(-1,-1), 0),
        ('BOTTOMPADDING',(0,0),(-1,-1), 0),
        ('BOX',          (0,0),(-1,-1), 0.5, C_MGRAY),
    ]))
    story.append(t2)
    return story

def qna_block(q, a_lines):
    """Q&A formatted block."""
    story = []
    qt = Table([[Paragraph(f'<font name="DV-B" color="white">❓ {q}</font>', S['small'])]],
               colWidths=[W-4*cm])
    qt.setStyle(TableStyle([
        ('BACKGROUND',   (0,0),(-1,-1), C_PURPLE),
        ('TOPPADDING',   (0,0),(-1,-1), 6),
        ('BOTTOMPADDING',(0,0),(-1,-1), 6),
        ('LEFTPADDING',  (0,0),(-1,-1), 10),
    ]))
    story.append(qt)
    rows = [[Paragraph(line, S['small'])] for line in a_lines]
    at = Table(rows, colWidths=[W-4*cm])
    at.setStyle(TableStyle([
        ('BACKGROUND',   (0,0),(-1,-1), C_LPURPLE),
        ('TOPPADDING',   (0,0),(-1,-1), 5),
        ('BOTTOMPADDING',(0,0),(-1,-1), 5),
        ('LEFTPADDING',  (0,0),(-1,-1), 10),
        ('RIGHTPADDING', (0,0),(-1,-1), 10),
        ('LINEAFTER',    (0,0),(0,-1),  2, C_PURPLE),
    ]))
    story.append(at)
    story.append(sp(6))
    return story

# ── Page Template ────────────────────────────────────────────────────────
def on_page(canvas, doc):
    canvas.saveState()
    # Header bar
    canvas.setFillColor(C_NAVY)
    canvas.rect(0, H-1.2*cm, W, 1.2*cm, fill=1, stroke=0)
    canvas.setFillColor(C_WHITE)
    canvas.setFont('DV-B', 8)
    canvas.drawString(2*cm, H-0.8*cm, 'HiBuddy — Tài liệu ôn tập kỹ thuật')
    canvas.setFont('DV', 8)
    canvas.drawRightString(W-2*cm, H-0.8*cm, f'Trang {doc.page}')
    # Footer
    canvas.setFillColor(C_MGRAY)
    canvas.rect(0, 0, W, 0.8*cm, fill=1, stroke=0)
    canvas.setFillColor(C_GRAY)
    canvas.setFont('DV', 7)
    canvas.drawCentredString(W/2, 0.25*cm,
        'Swipe • Matching • Admin/Moderation | FastAPI + PostgreSQL + Milvus + Redis')
    canvas.restoreState()

# ── Build Story ──────────────────────────────────────────────────────────
def build():
    doc = SimpleDocTemplate(
        './HiBuddy_OnTap_KyThuat.pdf',
        pagesize=A4, topMargin=1.6*cm, bottomMargin=1.2*cm,
        leftMargin=2*cm, rightMargin=2*cm,
        title='HiBuddy - Tài liệu ôn tập kỹ thuật',
        author='HiBuddy Team',
    )
    story = []

    # ════════════════════════════════════════════════════════════
    # TRANG BÌA
    # ════════════════════════════════════════════════════════════
    story.append(Spacer(1, 2*cm))
    cover = Table([[
        Paragraph('<font name="DV-B" size="22" color="white">TÀI LIỆU ÔN TẬP</font><br/>'
                  '<font name="DV-B" size="26" color="#fbbf24">VẤN ĐÁP KỸ THUẬT</font><br/>'
                  '<font name="DV" size="16" color="#93c5fd">HiBuddy Application</font>',
                  ParagraphStyle('cvr', fontName='DV-B', leading=32, alignment=TA_CENTER))
    ]], colWidths=[W-4*cm])
    cover.setStyle(TableStyle([
        ('BACKGROUND',   (0,0),(-1,-1), C_NAVY),
        ('TOPPADDING',   (0,0),(-1,-1), 30),
        ('BOTTOMPADDING',(0,0),(-1,-1), 30),
        ('ALIGN',        (0,0),(-1,-1), 'CENTER'),
    ]))
    story.append(cover)
    story.append(sp(12))

    # Subtitle badges
    badges = [
        ('Swipe & Discover', C_BLUE, C_LBLUE),
        ('Matching Algorithm', C_TEAL, C_LTEAL),
        ('Admin / Moderation', C_AMBER, C_LAMBER),
    ]
    badge_data = [[
        Paragraph(f'<font name="DV-B" color="white">{t}</font>',
                  ParagraphStyle('bd', fontName='DV-B', fontSize=10, alignment=TA_CENTER))
        for t,_,_ in badges
    ]]
    bt = Table(badge_data, colWidths=[(W-4*cm)/3]*3)
    bt.setStyle(TableStyle([
        ('BACKGROUND', (0,0),(0,0), C_BLUE),
        ('BACKGROUND', (1,0),(1,0), C_TEAL),
        ('BACKGROUND', (2,0),(2,0), C_AMBER),
        ('TOPPADDING',    (0,0),(-1,-1), 10),
        ('BOTTOMPADDING', (0,0),(-1,-1), 10),
        ('LEFTPADDING',   (0,0),(-1,-1), 4),
        ('RIGHTPADDING',  (0,0),(-1,-1), 4),
        ('ALIGN',         (0,0),(-1,-1), 'CENTER'),
    ]))
    story.append(bt)
    story.append(sp(16))

    # Table of contents
    toc_items = [
        ('0', 'Kiến Trúc Tổng Quan',        'Stack, Folder Structure, Database'),
        ('1', 'Flow Swipe / Discover',        'Discover cards, Swipe action, DB Schema'),
        ('2', 'Thuật Toán Tính Điểm',         'Project score, User score, Score blending'),
        ('3', 'Milvus Vector Database',       'Collections, Upsert vector, Hybrid search'),
        ('4', 'Admin & Moderation',           'Report, Block, Approve/Reject project'),
        ('5', 'Xác Thực Email / OTP',         'Register, OTP verify, Rate limiting'),
        ('6', 'Testing (pytest)',             'test_matching, test_swipe, test_email'),
        ('7', 'Q&A Câu Hỏi Thầy',            'Q1-Q6 kịch bản trả lời đầy đủ'),
        ('8', 'Bảng Số Liệu Quan Trọng',     'Constants, Weights cheatsheet'),
    ]
    toc_hdr = [Paragraph('<font name="DV-B" color="white">PHẦN</font>', S['small']),
               Paragraph('<font name="DV-B" color="white">TÊN PHẦN</font>', S['small']),
               Paragraph('<font name="DV-B" color="white">NỘI DUNG</font>', S['small'])]
    toc_rows = [toc_hdr] + [
        [Paragraph(f'<font name="DV-B" color="white">{n}</font>', S['small']),
         Paragraph(f'<font name="DV-B" color="{C_NAVY.hexval()}">{t}</font>', S['small']),
         Paragraph(d, S['small'])]
        for n,t,d in toc_items
    ]
    toc_bg = [C_NAVY] + [C_LGRAY if i%2==0 else C_WHITE for i in range(len(toc_items))]
    tt = Table(toc_rows, colWidths=[1.2*cm, 5.5*cm, None])
    tt.setStyle(TableStyle([
        ('ROWBACKGROUNDS', (0,0),(-1,-1), toc_bg),
        ('BACKGROUND',     (0,0),(0,0),   C_NAVY),
        ('TOPPADDING',     (0,0),(-1,-1), 6),
        ('BOTTOMPADDING',  (0,0),(-1,-1), 6),
        ('LEFTPADDING',    (0,0),(-1,-1), 8),
        ('VALIGN',         (0,0),(-1,-1), 'MIDDLE'),
        ('GRID',           (0,0),(-1,-1), 0.3, C_MGRAY),
        ('ALIGN',          (0,0),(0,-1),  'CENTER'),
    ]))
    story.append(P('MỤC LỤC', 'h2'))
    story.append(tt)
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 0: KIẾN TRÚC TỔNG QUAN
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 0 — KIẾN TRÚC TỔNG QUAN', C_NAVY))
    story.append(sp(8))

    story.append(SectionDivider('0.1', 'Tech Stack'))
    story.append(sp(6))
    story.append(header_table(
        ['Layer', 'Công Nghệ', 'Ghi Chú'],
        [
            ['Backend Framework', 'FastAPI (Python)', 'async/await, Python 3.11+'],
            ['SQL Database', 'PostgreSQL', 'async via asyncpg + SQLAlchemy'],
            ['Vector Database', 'Milvus', 'Port 19530, pymilvus'],
            ['Cache / Queue', 'Redis', 'Rate limiting, session cache'],
            ['File Storage', 'MinIO', 'S3-compatible (ảnh, tài liệu)'],
            ['AI Model', 'SentenceTransformer all-MiniLM-L6-v2', 'Local, offline, vector 384 chiều'],
            ['Push Notify', 'FCM – Firebase Cloud Messaging', 'Mobile push notifications'],
            ['Email', 'aiosmtplib', 'Async SMTP, gửi OTP'],
        ],
        col_w=[3.5*cm, 5*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('0.2', 'Cấu trúc thư mục Backend'))
    story.append(sp(6))
    story += code_block([
        'backend/',
        '├── app/',
        '│   ├── main.py           ← FastAPI app, gắn routers',
        '│   ├── config.py         ← Tất cả settings (env vars)',
        '│   ├── database.py       ← AsyncSession, engine',
        '│   ├── milvus_client.py  ← Kết nối & khởi tạo Milvus',
        '│   ├── api/',
        '│   │   ├── swipe.py      ← Router: /api/v1/swipe/*',
        '│   │   └── admin.py      ← Router: /api/v1/admin/*',
        '│   ├── services/',
        '│   │   ├── swipe_service.py     ← Logic discover + match (1479 dòng!)',
        '│   │   ├── matching_service.py  ← Thuật toán tính điểm Deterministic',
        '│   │   └── embedding_service.py ← AI vector (SentenceTransformer)',
        '│   └── models/',
        '│       ├── swipe.py      ← SwipeAction, Match, SwipeQueueItem',
        '│       └── trust_safety.py ← UserBlock, Report',
        '└── tests/                ← pytest unit tests',
    ], title='Folder Structure')
    story.append(sp(8))

    story.append(SectionDivider('0.3', 'Database Overview'))
    story.append(sp(4))
    story.append(InfoBox([
        '2 Database chạy song song: PostgreSQL (data nghiệp vụ) + Milvus (vector embeddings)',
    ], C_TEAL, C_LTEAL, '🗄'))
    story.append(sp(6))

    story.append(header_table(
        ['Database', 'Type', 'Nội dung chính'],
        [
            ['PostgreSQL', 'Relational SQL', '20+ bảng: users, projects, swipe_actions, matches, reports, ...'],
            ['Milvus', 'Vector DB', '2 collections: user_profile_vectors, project_vectors (dim=384)'],
        ],
        col_w=[3*cm, 3*cm, None]
    ))
    story.append(sp(6))

    story.append(P('<font name="DV-B">Các bảng SQL quan trọng:</font>', 'normal'))
    sql_tables = [
        'users, user_profiles, user_roles, user_skills, user_interests',
        'projects, project_role_slots, project_role_skill_requirements, project_members',
        'swipe_actions, matches, swipe_queue_items',
        'user_blocks, reports',
        'account_tokens, refresh_tokens, auth_identities',
        'notifications, chats, messages, outbox_events, admin_audit_logs',
    ]
    for t in sql_tables:
        story.append(bullet(t))
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 1: FLOW SWIPE / DISCOVER
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 1 — FLOW SWIPE / DISCOVER', C_BLUE))
    story.append(sp(8))

    # 1.1 Discover
    story.append(SectionDivider('1.1', 'Load Danh Sách Swipe (GET /api/v1/swipe/discover)'))
    story.append(sp(6))
    story.append(InfoBox([
        'Endpoint: GET /api/v1/swipe/discover?mode=CONTRIBUTOR&limit=20',
        'File: app/api/swipe.py → discover_cards() → swipe_service.py → get_discover_cards()',
    ], C_BLUE, C_LBLUE, '📡'))
    story.append(sp(8))

    steps = [
        ('1', 'API Layer', 'swipe.py::discover_cards() → Validate limit (1–50) → Gọi swipe_service'),
        ('2', 'Phân loại Mode', 'CONTRIBUTOR → _discover_projects() | OWNER → _discover_users()'),
        ('3A', 'Build Exclusion List', '_get_exclusion_ids() → Truy SQL lấy danh sách KHÔNG hiển thị'),
        ('3B', 'AI Vector Search', '_search_similar_projects() → Encode user vector → Query Milvus'),
        ('3C', 'SQL Query', 'SELECT projects WHERE status=RECRUITING AND review_status=APPROVED AND ...'),
        ('3D', 'Tính Điểm', 'matching_service::calculate_project_score_details() cho từng project'),
        ('3E', 'Score Blending', 'final_score = (deterministic_score + vec_score×100) / 2'),
        ('3F', 'Sort & Pagination', 'Sort giảm dần theo score, cursor-based pagination'),
        ('4', 'Return JSON', '{ project_cards, next_cursor, daily_likes_remaining, daily_superlikes_remaining }'),
    ]
    for num, title, desc in steps:
        row = Table([[
            Table([[Paragraph(f'<font name="DV-B" size="9" color="white">{num}</font>',
                             ParagraphStyle('sn', fontName='DV-B', alignment=TA_CENTER))]],
                  colWidths=[0.7*cm]),
            Table([[
                Paragraph(f'<font name="DV-B" color="{C_NAVY.hexval()}">{title}</font>', S['small']),
                Paragraph(desc, S['small'])
            ]], colWidths=[3.5*cm, None])
        ]], colWidths=[0.9*cm, None])
        row.setStyle(TableStyle([
            ('BACKGROUND', (0,0),(0,0), C_BLUE),
            ('BACKGROUND', (1,0),(1,0), C_LGRAY),
            ('VALIGN',     (0,0),(-1,-1), 'MIDDLE'),
            ('TOPPADDING', (0,0),(-1,-1), 5),
            ('BOTTOMPADDING',(0,0),(-1,-1), 5),
            ('LEFTPADDING',(0,0),(-1,-1), 4),
        ]))
        story.append(row)
        story.append(sp(2))

    story.append(sp(8))
    story.append(SectionDivider('', 'Chi tiết Bước 3A: Exclusion List (_get_exclusion_ids)'))
    story.append(sp(4))
    excl_data = [
        ['Loại trừ', 'Điều kiện'],
        ['PASS (7 ngày)', 'swipe_actions WHERE action=\'PASS\' AND created_at > now()-7days'],
        ['LIKE / SUPER_LIKE', 'swipe_actions WHERE action IN (\'LIKE\',\'SUPER_LIKE\') (mọi thời gian)'],
        ['Đang trong Queue', 'swipe_queue_items WHERE is_active=True'],
        ['Đã Match', 'matches WHERE is_unmatched=False'],
        ['Đã Block (2 chiều)', 'user_blocks WHERE blocker=user OR blocked=user'],
    ]
    t = Table(excl_data, colWidths=[4*cm, None])
    t.setStyle(TableStyle([
        ('BACKGROUND',  (0,0),(-1,0), C_RED),
        ('TEXTCOLOR',   (0,0),(-1,0), C_WHITE),
        ('FONTNAME',    (0,0),(-1,0), 'DV-B'),
        ('FONTSIZE',    (0,0),(-1,-1), 8),
        ('FONTNAME',    (0,1),(-1,-1), 'DV-M'),
        ('ROWBACKGROUNDS',(0,1),(-1,-1), [C_WHITE, C_LRED]),
        ('TOPPADDING',  (0,0),(-1,-1), 5),
        ('BOTTOMPADDING',(0,0),(-1,-1), 5),
        ('LEFTPADDING', (0,0),(-1,-1), 6),
        ('GRID',        (0,0),(-1,-1), 0.3, C_MGRAY),
    ]))
    story.append(t)
    story.append(sp(8))

    story.append(SectionDivider('', 'Chi tiết Bước 3B: Milvus Vector Search'))
    story.append(sp(4))
    story += code_block([
        '# 1. Build text từ user profile',
        'text = "full_name | bio | roles | skills(level) | university"',
        '',
        '# 2. Encode → vector 384 chiều',
        'vector = SentenceTransformer("all-MiniLM-L6-v2").encode(text)',
        '',
        '# 3. Search Milvus collection "project_vectors"',
        'results = collection.search(',
        '    data=[vector],',
        '    anns_field="vector",',
        '    param={"metric_type": "COSINE", "params": {"ef": 64}},',
        '    limit=limit*2,          # lấy gấp đôi để dự phòng',
        '    expr="status == \'RECRUITING\'",',
        '    output_fields=["project_id"]',
        ')',
    ], title='Milvus Search Query')
    story.append(sp(6))

    story.append(SectionDivider('', 'Công thức Cosine Similarity (tự implement)'))
    story.append(sp(4))
    story += code_block([
        'dot_product = sum(a[i] * b[i]  for i in range(384))',
        'norm_a      = sqrt(sum(a[i]**2 for i in range(384)))',
        'norm_b      = sqrt(sum(b[i]**2 for i in range(384)))',
        'cosine      = dot / (norm_a * norm_b)   # clamp → [0.0, 1.0]',
    ], title='Cosine Similarity')
    story.append(PageBreak())

    # 1.2 Swipe Action
    story.append(ColorBox('PHẦN 1 (tiếp) — FLOW SWIPE ACTION', C_BLUE))
    story.append(sp(8))
    story.append(SectionDivider('1.2', 'Thực Hiện Swipe (POST /api/v1/swipe/action)'))
    story.append(sp(4))
    story.append(InfoBox([
        'Endpoint: POST /api/v1/swipe/action',
        'Body: { target_type: "PROJECT"|"USER", target_id, action: "PASS"|"LIKE"|"SUPER_LIKE" }',
        'File: app/services/swipe_service.py → perform_swipe_action()',
    ], C_BLUE, C_LBLUE, '👆'))
    story.append(sp(8))

    swipe_steps = [
        ('B1', 'Validate Input', 'target_type ∈ {PROJECT, USER}, action ∈ {PASS, LIKE, SUPER_LIKE}'),
        ('B2', 'Validate Target', '_validate_swipe_target() → kiểm tra tồn tại, status, không tự swipe mình, UserBlock'),
        ('B3', 'Check Trùng Lặp', 'SELECT swipe_actions WHERE swiper_id+target+context+is_active=True'),
        ('B4', 'Giới Hạn Ngày', 'LIKE: max 50/ngày | SUPER_LIKE: max 3/ngày → raise 400 nếu vượt'),
        ('B5', 'Lưu DB', 'INSERT SwipeAction → db.flush()'),
        ('B6', 'PASS Early Return', 'Nếu action=PASS → return {matched: False} ngay'),
        ('B7', 'Check Match', '_check_match() → 2 chiều: cả contributor và owner đều LIKE nhau chưa?'),
        ('B8', 'Tạo Match', '_create_match() → INSERT: matches + chats + 2 notifications + 2 outbox_events'),
        ('B9', 'Super Like Notify', 'Nếu SUPER_LIKE nhưng chưa match → _notify_super_like() → FCM'),
    ]
    for num, title, desc in swipe_steps:
        is_match = num in ['B7', 'B8']
        is_done = num in ['B6']
        bg = C_LTEAL if is_match else (C_LRED if is_done else C_LGRAY)
        nb = C_TEAL if is_match else (C_RED if is_done else C_BLUE)
        row = Table([[
            Table([[Paragraph(f'<font name="DV-B" size="8" color="white">{num}</font>',
                             ParagraphStyle('sn2', fontName='DV-B', alignment=TA_CENTER))]],
                  colWidths=[0.8*cm]),
            Paragraph(f'<font name="DV-B" color="{C_NAVY.hexval()}">{title}  </font>{desc}', S['small'])
        ]], colWidths=[1*cm, None])
        row.setStyle(TableStyle([
            ('BACKGROUND', (0,0),(0,0), nb),
            ('BACKGROUND', (1,0),(1,0), bg),
            ('VALIGN',     (0,0),(-1,-1), 'MIDDLE'),
            ('TOPPADDING', (0,0),(-1,-1), 5),
            ('BOTTOMPADDING',(0,0),(-1,-1), 5),
            ('LEFTPADDING',(1,0),(1,0), 8),
        ]))
        story.append(row)
        story.append(sp(2))

    story.append(sp(8))
    story.append(SectionDivider('', 'Logic Check Match 2 Chiều'))
    story.append(sp(4))
    story += code_block([
        '# Contributor LIKE project:',
        '#   → Kiểm tra owner đã LIKE contributor này chưa?',
        'SELECT swipe_actions WHERE',
        '    swiper_id = project.owner_id',
        '    AND target_type = "USER"',
        '    AND target_id   = contributor.id',
        '    AND action IN ("LIKE", "SUPER_LIKE")',
        '    AND context_project_id = project.id',
        '    AND is_active = True',
        '',
        '# Nếu có → MATCH! → _create_match()',
        '#   INSERT: matches + chats + notifications (x2) + outbox_events (x2)',
    ], title='Match Logic')
    story.append(sp(8))

    story.append(SectionDivider('1.3', 'Schema Bảng Dữ Liệu Swipe'))
    story.append(sp(4))
    story.append(header_table(
        ['Bảng', 'Cột Quan Trọng', 'Ghi Chú'],
        [
            ['swipe_actions', 'swiper_id, target_type, target_id, action, context_project_id, context_key, is_active',
             'UNIQUE on (swiper_id, target_type, target_id, context_key) WHERE is_active=True'],
            ['matches', 'user_id (contributor), project_id, owner_id, match_score, role_matched, is_unmatched',
             'UNIQUE on (user_id, project_id) WHERE is_unmatched=False'],
            ['swipe_queue_items', 'expires_at (24h TTL), is_active, resolution',
             'resolution: PASS|LIKE|SUPER_LIKE|EXPIRED|REMOVED'],
        ],
        col_w=[3.5*cm, 7*cm, None]
    ))
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 2: THUẬT TOÁN TÍNH ĐIỂM
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 2 — THUẬT TOÁN TÍNH ĐIỂM (matching_service.py)', C_TEAL))
    story.append(sp(8))

    story.append(SectionDivider('2.1', 'Tính Điểm Project (Contributor xem Project)'))
    story.append(sp(4))
    story.append(P('Hàm: <font name="DV-M">calculate_project_score_details(user, project, owner)</font>', 'normal'))
    story.append(sp(4))

    story.append(InfoBox(['⚠ slot_fit chiếm 80% tổng điểm → đây là yếu tố quan trọng nhất!'],
                         C_AMBER, C_LAMBER, '⚠'))
    story.append(sp(6))

    story.append(header_table(
        ['Yếu Tố', 'Trọng Số', 'Cách Tính'],
        [
            ['slot_fit (role + skill)', '× 0.80', 'best_role_slot() → role_score×0.80 + skill_score×0.20'],
            ['interest_score', '× 0.08', 'Jaccard Index(user.interests, project.fields) × 100'],
            ['owner_quality', '× 0.08', 'reputation_pct + 15 bonus nếu verified_student (max 100)'],
            ['recency', '× 0.04', '<7 ngày=100, <14=70, <30=40, ≥30=10'],
        ],
        col_w=[4*cm, 2.5*cm, None]
    ))
    story.append(sp(4))
    story += code_block([
        'score = slot_fit      * 0.80',
        '      + interest_score * 0.08',
        '      + owner_quality  * 0.08',
        '      + recency        * 0.04',
    ], title='Công thức tổng hợp điểm Project')
    story.append(sp(8))

    story.append(SectionDivider('', 'Chi tiết tính slot_fit'))
    story.append(sp(4))

    story.append(header_table(
        ['Sub-yếu tố', 'Trọng số trong slot', 'Logic'],
        [
            ['role_score', '× 0.80', 'normalize_name(user.roles) == normalize_name(slot.role_name) → 100 | else 0\nVD: "Android Developer" → alias → "mobile developer"'],
            ['skill_score', '× 0.20', '(chỉ tính khi role match)\nfuzzy_match(required_skill, user_skills, threshold=85%)\nrequired=True → weight=2.0 | optional → weight=1.0\nskill_score = earned/total × 100'],
        ],
        col_w=[3*cm, 3*cm, None]
    ))
    story.append(sp(6))

    story.append(P('<font name="DV-B">Ví dụ minh họa:</font>', 'normal'))
    story += code_block([
        'User: "Backend Developer" | skills: Python(ADVANCED), PostgreSQL(INTERMEDIATE)',
        'Project slot: "Backend Developer" needs:',
        '  → Python (INTERMEDIATE, required=True,  weight=2.0) ← matched ✓',
        '  → PostgreSQL (BEGINNER, optional=False, weight=1.0) ← matched ✓',
        '',
        'role_score  = 100 (match)',
        'skill_score = (2.0+1.0)/(2.0+1.0) × 100 = 100.0',
        'slot_fit    = 100×0.80 + 100×0.20 = 100.0',
    ], title='Ví dụ tính slot_fit')
    story.append(sp(8))

    story.append(SectionDivider('2.2', 'Tính Điểm User (Owner xem Ứng Viên)'))
    story.append(sp(4))
    story.append(P('Hàm: <font name="DV-M">calculate_user_score_details(owner, target_user, project)</font>', 'normal'))
    story.append(sp(4))
    story.append(header_table(
        ['Yếu Tố', 'Trọng Số', 'Cách Tính'],
        [
            ['project_score', '× 0.70', 'Gọi lại calculate_project_score_details(target_user, project, owner)'],
            ['reputation_pct', '× 0.12', 'target_user.reputation_score / 5.0 × 100'],
            ['experience_pct', '× 0.08', 'min(projects_completed / 10, 1.0) × 100'],
            ['verified_student', '× 0.10', '100.0 nếu verified_student=True, else 0.0'],
        ],
        col_w=[3.5*cm, 2.5*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('2.3', 'Score Blending (AI + Deterministic)'))
    story.append(sp(4))
    story += code_block([
        'if ENABLE_EMBEDDINGS:',
        '    vec_score   = cosine_similarity(user_vec, project_vec) × 100',
        '    final_score = (deterministic_score + vec_score) / 2',
        'else:',
        '    final_score = deterministic_score',
    ], title='Score Blending')
    story.append(sp(6))
    story.append(header_table(
        ['Ngưỡng', 'Nhãn'],
        [
            ['≥ 85', 'Excellent 🌟'],
            ['≥ 70', 'Very Good ✨'],
            ['≥ 50', 'Good 👍'],
            ['≥ 30', 'Fair'],
            ['< 30',  'Low'],
        ],
        col_w=[3*cm, None]
    ))
    story.append(sp(6))
    story.append(InfoBox([
        'Newbie Boost: User < 30 ngày HOẶC chưa làm project nào',
        '→ reputation giả định 3.8/5.0 = 76% (thay vì 0)',
        '→ Tránh new user bị điểm thấp ngay từ đầu',
    ], C_AMBER, C_LAMBER, '🚀'))
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 3: MILVUS
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 3 — MILVUS VECTOR DATABASE', C_PURPLE))
    story.append(sp(8))

    story.append(SectionDivider('3.1', 'Cấu Trúc Collections'))
    story.append(sp(4))
    story.append(header_table(
        ['Field', 'Type', 'Chi tiết'],
        [
            ['Collection: user_profile_vectors', '', ''],
            ['id', 'INT64', 'Primary key, auto_id=True'],
            ['user_id', 'VARCHAR(36)', 'UUID dạng string'],
            ['vector', 'FLOAT_VECTOR(384)', 'Output của all-MiniLM-L6-v2'],
            ['mode', 'VARCHAR(20)', '"CONTRIBUTOR" | "OWNER" | "BOTH"'],
            ['is_active', 'BOOL', '—'],
            ['reputation_score', 'FLOAT', '—'],
            ['Collection: project_vectors', '', ''],
            ['project_id', 'VARCHAR(36)', 'UUID'],
            ['vector', 'FLOAT_VECTOR(384)', '—'],
            ['field', 'VARCHAR(50)', 'AI, Web, Mobile, ...'],
            ['status', 'VARCHAR(20)', '"RECRUITING" | "CLOSED"'],
            ['work_mode', 'VARCHAR(20)', 'REMOTE / ONSITE / ...'],
        ],
        col_w=[5*cm, 3.5*cm, None]
    ))
    story.append(sp(4))
    story.append(InfoBox([
        'Index: HNSW trên field "vector" | M=16, efConstruction=200 | Metric: COSINE',
        'Search param: ef=64 (accuracy vs speed trade-off)',
    ], C_PURPLE, C_LPURPLE, '📊'))
    story.append(sp(8))

    story.append(SectionDivider('3.2', 'Upsert Vector Khi Có Dữ Liệu Mới'))
    story.append(sp(4))
    story += code_block([
        '# Chạy dưới dạng BackgroundTask (không block response)',
        'async def upsert_project_vector(project):',
        '    # 1. Build text từ project',
        '    text = f"{title} | {description} | {field} | {role_names} | {work_mode}"',
        '',
        '    # 2. Encode → vector 384 chiều',
        '    vector = SentenceTransformer("all-MiniLM-L6-v2").encode(text).tolist()',
        '',
        '    # 3. Xóa embedding cũ (nếu có)',
        '    if project.embedding_id:',
        '        collection.delete(f"id == {project.embedding_id}")',
        '',
        '    # 4. Insert embedding mới',
        '    result = collection.insert([{"project_id": str(project.id), "vector": vector, ...}])',
        '    collection.flush()  # ghi xuống disk',
        '',
        '    # 5. Lưu embedding_id mới vào PostgreSQL',
        '    project.embedding_id = result.primary_keys[0]',
    ], title='upsert_project_vector()')
    story.append(sp(8))

    story.append(SectionDivider('3.3', 'Luồng Hybrid Search (Milvus + PostgreSQL)'))
    story.append(sp(4))
    story.append(InfoBox([
        'Tại sao phức tạp? → Milvus KHÔNG biết lịch sử swipe, PostgreSQL KHÔNG làm được semantic search',
        '→ Phải kết hợp 2 nguồn thủ công (choreograph)',
    ], C_RED, C_LRED, '⚡'))
    story.append(sp(6))

    hybrid_steps = [
        ('1', 'Encode', 'SentenceTransformer → user_vector (384 chiều)'),
        ('2', 'Milvus Query', 'top (limit×2=40) project có COSINE similarity cao nhất, filter status=RECRUITING'),
        ('3', 'Filter', 'Loại bỏ IDs có trong exclude_project_ids (từ swipe history PostgreSQL)'),
        ('4', 'SQL Query', 'SELECT projects WHERE id IN (filtered_ids) AND review_status=APPROVED AND ...'),
        ('5', 'Deterministic Score', 'matching_service → tính điểm cho từng project'),
        ('6', 'Vec Score', 'cosine(user_vec, project_vec)×100 (re-compute, dùng _embedding_cache)'),
        ('7', 'Blend', 'final_score = (deterministic + vec_score) / 2'),
        ('8', 'Sort', 'Sắp xếp giảm dần theo final_score'),
    ]
    for n, t, d in hybrid_steps:
        story.append(InfoBox([f'[{n}] {t}: {d}'],
                             C_PURPLE if int(n)%2==0 else C_TEAL,
                             C_LPURPLE if int(n)%2==0 else C_LTEAL, f'  '))
        story.append(sp(2))

    story.append(sp(4))
    story.append(InfoBox([
        'Tại sao tính cosine lại lần 2?',
        '→ Milvus trả về distance nhưng đã mất sau query',
        '→ Cần tính lại để blend với deterministic score',
        '→ Dùng in-memory cache _embedding_cache để tránh encode lại',
    ], C_AMBER, C_LAMBER, '❓'))
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 4: ADMIN & MODERATION
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 4 — ADMIN & MODERATION', colors.HexColor('#92400e')))
    story.append(sp(8))

    story.append(SectionDivider('4.1', 'Cấu Trúc Dữ Liệu'))
    story.append(sp(4))
    story.append(header_table(
        ['Model', 'Cột Quan Trọng', 'Ghi Chú'],
        [
            ['Report', 'reporter_id, reported_id, reason (500), description (1000), status (PENDING/RESOLVED), evidence_url', 'File: models/trust_safety.py'],
            ['UserBlock', 'blocker_id, blocked_id', 'UNIQUE(blocker_id, blocked_id) | CHECK(blocker_id ≠ blocked_id)'],
            ['Project', 'review_status (APPROVED/FLAGGED/REJECTED), moderation_categories, moderation_reasons', 'Default: "APPROVED" → xuất hiện ngay'],
        ],
        col_w=[2.5*cm, 7*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('4.2', 'Flow Admin Duyệt Project'))
    story.append(sp(4))
    story.append(InfoBox([
        'Project mới tạo: review_status = "APPROVED" (default) → xuất hiện ngay trong Discover',
        '→ Khi bị report đủ lần → chuyển "FLAGGED" → Admin mới cần can thiệp',
    ], C_AMBER, C_LAMBER, '⚠'))
    story.append(sp(6))

    story.append(header_table(
        ['Endpoint', 'Action', 'Flow'],
        [
            ['GET /admin/projects/flagged', 'List cần duyệt', 'SELECT projects WHERE review_status=FLAGGED ORDER BY created_at DESC (yêu cầu Admin role)'],
            ['POST /admin/projects/{id}/approve', 'APPROVE', 'review_status=APPROVED → INSERT Notification → INSERT AdminAuditLog → INSERT OutboxEvent (FCM)'],
            ['POST /admin/projects/{id}/reject', 'REJECT', 'review_status=REJECTED + status=CLOSED → Notification + AuditLog + OutboxEvent (FCM)'],
        ],
        col_w=[4.5*cm, 2.5*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('4.3', 'UserBlock – Tác Động Lên Hệ Thống'))
    story.append(sp(4))
    story.append(header_table(
        ['Nơi áp dụng', 'Logic'],
        [
            ['Khi tải Discover\n(_get_exclusion_ids)', 'SELECT user_blocks WHERE blocker_id=user OR blocked_id=user\n→ Thêm vào exclude_user_ids (2 chiều)\n→ Block người nào → họ biến mất khỏi Explore của nhau'],
            ['Khi thực hiện Swipe\n(_validate_swipe_target)', 'SELECT user_blocks WHERE (blocker=me AND blocked=target) OR (blocker=target AND blocked=me)\n→ Nếu có block record → raise ValueError("Target is unavailable")\n→ Không thể swipe người đã block'],
        ],
        col_w=[4.5*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('4.4', 'Discovery Filter'))
    story.append(sp(4))
    story += code_block([
        '# swipe_service.py → _discover_projects() line ~938',
        '.where(',
        '    Project.status        == "RECRUITING",',
        '    Project.review_status == "APPROVED",   # ← CHỈ hiện APPROVED',
        '    Project.owner_id      != user.id,',
        '    User.is_active        == True,',
        '    User.email_verified   == True,',
        '    Project.id.not_in(exclude_project_ids)',
        ')',
    ], title='SQL Filter trong _discover_projects()')
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 5: XÁC THỰC EMAIL / OTP
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 5 — XÁC THỰC EMAIL / OTP SINH VIÊN', C_TEAL))
    story.append(sp(8))

    story.append(SectionDivider('5.1', 'Flow Đăng Ký + Gửi OTP'))
    story.append(sp(4))
    story.append(InfoBox(['Endpoint: POST /api/v1/auth/register | File: app/services/auth_service.py'],
                         C_TEAL, C_LTEAL, '📝'))
    story.append(sp(6))

    reg_steps = [
        ('1', 'Validate', 'Email/username không trùng trong DB'),
        ('2', 'Hash Password', 'bcrypt.hash(password) → lưu vào DB'),
        ('3', 'INSERT User', 'users table, email_verified=False'),
        ('4', 'INSERT Profile', 'user_profiles: display_name, reputation_score=0.0, projects_completed=0'),
        ('5', 'Tạo OTP', '_create_account_code() → generate_numeric_code() (6 chữ số) → bcrypt hash → INSERT account_tokens (TTL 15 phút)'),
        ('6', 'Gửi Email', 'BackgroundTask: aiosmtplib.send() → không block response'),
        ('7', 'Return JWT', 'access_token + refresh_token + requires_email_verification=True'),
    ]
    for n, t, d in reg_steps:
        story.append(bullet(f'<font name="DV-B">[{n}] {t}:</font> {d}'))
    story.append(sp(8))

    story.append(SectionDivider('5.2', 'Flow Verify OTP'))
    story.append(sp(4))
    story.append(InfoBox(['Endpoint: POST /api/v1/auth/verify-email | OTP chỉ dùng được 1 lần (1-time use)'],
                         C_TEAL, C_LTEAL, '✅'))
    story.append(sp(6))
    story += code_block([
        '# _consume_account_code(db, email, purpose, code):',
        '1. SELECT account_tokens WHERE email=email AND purpose=EMAIL_VERIFICATION AND consumed_at IS NULL',
        '2. Kiểm tra: token.expires_at <= now  → raise 400 "Invalid or expired code"',
        '3. Kiểm tra: token.attempts >= 5      → raise 429 (too many attempts)',
        '4. So sánh: hash_token(code) == token.code_hash',
        '   → Sai: token.attempts += 1, commit, raise 400',
        '   → Đúng: token.consumed_at = now (vô hiệu hóa)',
        '           user.email_verified = True',
    ], title='Logic _consume_account_code()')
    story.append(sp(8))

    story.append(SectionDivider('5.3', 'Rate Limiting OTP'))
    story.append(sp(4))
    story.append(header_table(
        ['Giới Hạn', 'Giá Trị', 'Behavior'],
        [
            ['OTP TTL', '15 phút', 'AUTH_CODE_TTL_MINUTES = 15'],
            ['Max sai', '5 lần', 'AUTH_CODE_MAX_ATTEMPTS = 5 → sau đó OTP bị consumed ngay'],
            ['Cooldown gửi lại', '60 giây', 'AUTH_CODE_RESEND_SECONDS = 60 → raise HTTP 429 + header "Retry-After: N"'],
        ],
        col_w=[4*cm, 2.5*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('5.4', 'Feature Gating'))
    story.append(sp(4))
    story.append(header_table(
        ['Flag', 'Ý Nghĩa', 'Tác Động'],
        [
            ['email_verified = True', 'Email xác thực qua OTP', 'Bắt buộc mới được xuất hiện trong Discover của Owner'],
            ['verified_student = True', 'Admin duyệt thẻ sinh viên', '+10 điểm trong calculate_user_score, +15 điểm bonus owner_quality'],
        ],
        col_w=[3.5*cm, 4*cm, None]
    ))
    story.append(sp(4))
    story.append(InfoBox([
        'email_verified ≠ verified_student:',
        '→ email_verified: xác nhận email thật (OTP tự động)',
        '→ verified_student: xác nhận sinh viên thật (Admin duyệt thẻ SV)',
    ], C_AMBER, C_LAMBER, '⚠'))
    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 6: TESTING
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 6 — TESTING (pytest)', colors.HexColor('#065f46')))
    story.append(sp(8))
    story.append(InfoBox(['Chạy: cd backend && pytest tests/ -v | File: backend/tests/'],
                         C_TEAL, C_LTEAL, '🧪'))
    story.append(sp(8))

    test_files = [
        ('test_matching.py', [
            'test_role_aliases_are_normalized: "Android Developer" → "mobile developer", "Full-Stack" → "fullstack developer"',
            'test_matching_uses_skills_from_the_matched_role_only: không lấy skill của role khác (VD: Figma không áp vào Backend slot)',
            'test_newbie_bootstrap_reputation: user < 30 ngày → reputation giả định 3.8/5.0 = 76%',
            'test_verified_student_influence: verified owner → +15 bonus | verified user → factors["verified_student"] = 100.0',
        ]),
        ('test_swipe_project.py', [
            'test_swipe_project_scenarios_with_varied_owner_quality: 5 kịch bản',
            'Assert thứ tự: high_fit_high_owner > high_fit_low_owner',
            'Assert: no_fit → điểm thấp dù owner tốt đến đâu',
        ]),
        ('test_email_verification.py', [
            'test_email_verification_flow: Tạo OTP 6 chữ số → verify sai email → HTTP 403 → verify đúng → email_verified=True, OTP consumed',
            'test_register_duplicate_email_or_username_returns_conflict: HTTP 409',
            'test_public_resend_verification: AccountToken insert + email gửi đúng + code 6 chữ số',
        ]),
        ('test_swipe_queue.py & test_swipe_user.py', [
            'Queue 24h TTL: card vào queue → sau 24h tự PASS',
            'Giới hạn queue: tối đa 3 card/loại',
            'Owner swipe user trong context project cụ thể',
        ]),
    ]
    for fname, cases in test_files:
        story.append(P(f'<font name="DV-B" color="{C_TEAL.hexval()}">📄 {fname}</font>', 'normal'))
        for c in cases:
            story.append(bullet(c))
        story.append(sp(4))

    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 7: CÂU HỎI THẦY
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 7 — CÂU HỎI THẦY & KỊCH BẢN TRẢ LỜI', C_PURPLE))
    story.append(sp(8))

    qnas = [
        ('Q1: Phần khó nhất về kỹ thuật là gì?', [
            'Dạ, phần khó nhất là hàm get_discover_cards trong swipe_service.py, cụ thể là bài toán Hybrid Query.',
            '(1) Tính vector 384 chiều của user bằng SentenceTransformer.',
            '(2) Query Milvus → top 40 project ngữ nghĩa gần nhất (COSINE + HNSW index).',
            '(3) Cùng lúc, query PostgreSQL để lấy exclusion list (đã swipe, block, queue).',
            '(4) Lọc kết quả Milvus theo exclusion list PostgreSQL.',
            '(5) Tính deterministic score + blend với vector cosine score.',
            'Cái khó: 2 database không nói chuyện trực tiếp được. Phải choreograph thủ công.',
            'Fallback: Nếu Milvus down → graceful fallback về PostgreSQL-only mode.',
        ]),
        ('Q2: Nếu thêm dữ liệu mới thì xử lý sao?', [
            'Khi user tạo project mới hoặc cập nhật profile, hệ thống xử lý 2 bước:',
            'BƯỚC 1 - Sync: Lưu text data vào PostgreSQL ngay lập tức. embedding_id = None.',
            'BƯỚC 2 - Async (BackgroundTask): upsert_project_vector():',
            '  → build_project_text() → encode_text() → SentenceTransformer → vector[384]',
            '  → Milvus: delete old → insert new → flush()',
            '  → PostgreSQL: update projects.embedding_id = new_milvus_id',
            'Tại sao async? Encode AI model tốn ~100-200ms, không muốn user đợi.',
            'Trong thời gian chờ: project vẫn xuất hiện (SQL filter), nhưng chưa được AI xếp hạng chuẩn.',
        ]),
        ('Q3: Database có bao nhiêu bảng, bao nhiêu DB?', [
            '2 database chạy song song:',
            'PostgreSQL (data nghiệp vụ): 20+ bảng.',
            'Milvus (vector): 2 collections – user_profile_vectors, project_vectors.',
            'Mỗi collection: HNSW index, COSINE metric, vector 384 chiều.',
        ]),
        ('Q4: Chỉ ra flow cho phần Swipe - từng file?', [
            '[1] app/api/swipe.py → discover_cards(): nhận request, validate',
            '[2] app/services/swipe_service.py → get_discover_cards(): phân loại mode',
            '[3] app/services/swipe_service.py → _get_exclusion_ids(): SQL exclusion list',
            '[4] app/services/swipe_service.py → _search_similar_projects(): Milvus query',
            '[5] app/services/swipe_service.py → _discover_projects(): SQL + score',
            '[6] app/services/matching_service.py → calculate_project_score_details(): tính điểm',
            '[7] app/services/swipe_service.py (line ~966): Score Blending → final_score',
        ]),
        ('Q5: Cái nút này gọi hàm nào?', [
            'Nút "Explore" / Load cards:  GET /api/v1/swipe/discover → discover_cards() → get_discover_cards()',
            'Nút "Like" (tim):             POST /api/v1/swipe/action {action:"LIKE"} → swipe_action() → perform_swipe_action()',
            'Nút "Pass" (x):               POST /api/v1/swipe/action {action:"PASS"} → return ngay sau lưu PASS',
            'Nút "Super Like" (sao):       POST /api/v1/swipe/action {action:"SUPER_LIKE"} → lưu + _notify_super_like()',
        ]),
        ('Q6: Có test các chức năng này không?', [
            'Dạ có, dùng pytest. File trong backend/tests/.',
            'test_matching.py: role alias, skill matching, newbie boost, verified student.',
            'test_swipe_project.py: 5 kịch bản score, so sánh thứ tự đúng.',
            'test_email_verification.py: OTP flow, duplicate email 409, resend OTP.',
            'test_swipe_queue.py: queue 24h TTL, giới hạn 3 card/loại.',
        ]),
    ]
    for q, lines in qnas:
        story += qna_block(q, lines)

    story.append(PageBreak())

    # ════════════════════════════════════════════════════════════
    # PHẦN 8: BẢNG SỐ LIỆU QUAN TRỌNG
    # ════════════════════════════════════════════════════════════
    story.append(ColorBox('PHẦN 8 — BẢNG SỐ LIỆU QUAN TRỌNG (Cheatsheet)', C_RED))
    story.append(sp(8))

    story.append(SectionDivider('8.1', 'Giới Hạn Hệ Thống'))
    story.append(sp(4))
    story.append(header_table(
        ['Hằng Số (config.py)', 'Giá Trị', 'Ý Nghĩa'],
        [
            ['SWIPE_DAILY_LIKE_LIMIT',      '50',       'Số LIKE tối đa mỗi ngày'],
            ['SWIPE_DAILY_SUPERLIKE_LIMIT', '3',        'Số SUPER_LIKE tối đa mỗi ngày'],
            ['PASS_COOLDOWN_DAYS',          '7 ngày',   'Sau khi PASS, cooldown 7 ngày mới thấy lại'],
            ['QUEUE_TTL',                   '24h',      'SwipeQueueItem tự PASS sau 24h'],
            ['QUEUE_LIMIT_PER_TYPE',        '3',        'Tối đa 3 card mỗi loại trong queue'],
            ['AUTH_CODE_TTL_MINUTES',       '15 phút',  'OTP hết hạn sau 15 phút'],
            ['AUTH_CODE_MAX_ATTEMPTS',      '5 lần',    'Nhập sai tối đa 5 lần, sau đó OTP bị consumed'],
            ['AUTH_CODE_RESEND_SECONDS',    '60 giây',  'Cooldown giữa các lần gửi lại OTP'],
        ],
        col_w=[6*cm, 2.5*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('8.2', 'Thông Số Milvus'))
    story.append(sp(4))
    story.append(header_table(
        ['Thông Số', 'Giá Trị'],
        [
            ['EMBEDDING_DIM', '384 (output của all-MiniLM-L6-v2)'],
            ['Milvus Port',   '19530'],
            ['Index Type',    'HNSW'],
            ['HNSW M',        '16'],
            ['efConstruction','200'],
            ['ef (search)',   '64'],
            ['Metric',        'COSINE'],
            ['Collections',   'user_profile_vectors, project_vectors'],
        ],
        col_w=[4*cm, None]
    ))
    story.append(sp(8))

    story.append(SectionDivider('8.3', 'Trọng Số Điểm'))
    story.append(sp(4))

    # Side by side: Project score | User score
    left_data = [
        [Paragraph('<font name="DV-B" color="white">Điểm Project (Contributor → Project)</font>', S['small'])],
        [Paragraph('slot_fit (role + skill)  80%', S['small'])],
        [Paragraph('interest_score           8%', S['small'])],
        [Paragraph('owner_quality            8%', S['small'])],
        [Paragraph('recency                  4%', S['small'])],
    ]
    right_data = [
        [Paragraph('<font name="DV-B" color="white">Điểm User (Owner → Ứng Viên)</font>', S['small'])],
        [Paragraph('project_score            70%', S['small'])],
        [Paragraph('reputation_pct           12%', S['small'])],
        [Paragraph('experience_pct           8%', S['small'])],
        [Paragraph('verified_student         10%', S['small'])],
    ]
    lt = Table(left_data,  colWidths=[(W-4*cm)/2 - 4])
    rt = Table(right_data, colWidths=[(W-4*cm)/2 - 4])
    for t, c in [(lt, C_BLUE),(rt, C_TEAL)]:
        t.setStyle(TableStyle([
            ('BACKGROUND',   (0,0),(0,0), c),
            ('ROWBACKGROUNDS',(0,1),(-1,-1), [C_LGRAY, C_WHITE]),
            ('TOPPADDING',   (0,0),(-1,-1), 5),
            ('BOTTOMPADDING',(0,0),(-1,-1), 5),
            ('LEFTPADDING',  (0,0),(-1,-1), 8),
            ('FONTNAME',     (0,1),(-1,-1), 'DV-M'),
            ('FONTSIZE',     (0,1),(-1,-1), 9),
            ('GRID',         (0,0),(-1,-1), 0.3, C_MGRAY),
        ]))
    combo = Table([[lt, Spacer(8,1), rt]], colWidths=[(W-4*cm)/2-4, 8, (W-4*cm)/2-4])
    combo.setStyle(TableStyle([
        ('VALIGN',(0,0),(-1,-1),'TOP'),
        ('LEFTPADDING',(0,0),(-1,-1), 0),
        ('RIGHTPADDING',(0,0),(-1,-1), 0),
        ('BOTTOMPADDING',(0,0),(-1,-1), 0),
        ('TOPPADDING',(0,0),(-1,-1), 0),
    ]))
    story.append(combo)
    story.append(sp(8))

    story.append(SectionDivider('8.4', 'Rank Labels'))
    story.append(sp(4))
    rank_data = [
        ['Ngưỡng', 'Nhãn', 'Màu'],
        ['≥ 85',   'Excellent 🌟', 'Xanh lá đậm'],
        ['≥ 70',   'Very Good ✨', 'Xanh dương'],
        ['≥ 50',   'Good 👍',       'Xanh lá nhạt'],
        ['≥ 30',   'Fair',           'Cam'],
        ['< 30',   'Low',            'Đỏ'],
    ]
    rt2 = Table(rank_data, colWidths=[3*cm, 5*cm, None])
    rt2.setStyle(TableStyle([
        ('BACKGROUND',   (0,0),(-1,0), C_NAVY),
        ('TEXTCOLOR',    (0,0),(-1,0), C_WHITE),
        ('FONTNAME',     (0,0),(-1,0), 'DV-B'),
        ('FONTSIZE',     (0,0),(-1,-1), 9),
        ('ROWBACKGROUNDS',(0,1),(-1,-1), [C_WHITE, C_LGRAY]),
        ('TOPPADDING',   (0,0),(-1,-1), 5),
        ('BOTTOMPADDING',(0,0),(-1,-1), 5),
        ('LEFTPADDING',  (0,0),(-1,-1), 6),
        ('GRID',         (0,0),(-1,-1), 0.3, C_MGRAY),
    ]))
    story.append(rt2)
    story.append(sp(10))

    story.append(InfoBox([
        'ENABLE_MILVUS = True (config.py)  |  ENABLE_EMBEDDINGS = True (config.py)',
        'Project mới: review_status = "APPROVED" (default) → xuất hiện ngay',
        'Phân quyền Admin: get_current_admin() middleware trong admin.py',
    ], C_NAVY, C_LBLUE, '📌'))

    # ── Build ──────────────────────────────────────────────────────
    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
    print('PDF created successfully!')

if __name__ == '__main__':
    build()