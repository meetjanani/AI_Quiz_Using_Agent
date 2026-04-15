from pathlib import Path
from textwrap import wrap

from PIL import Image, ImageDraw, ImageFont
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_AUTO_SHAPE_TYPE
from pptx.enum.text import PP_ALIGN
from pptx.util import Inches, Pt

ROOT = Path(__file__).resolve().parent
PPTX_PATH = ROOT / "Agentic_AI_Code_Review_Demo.pptx"
DIAGRAM_PATH = ROOT / "agentic-review-flow.png"

PRIMARY = RGBColor(14, 44, 84)
ACCENT = RGBColor(0, 129, 167)
SUCCESS = RGBColor(44, 120, 87)
TEXT = RGBColor(34, 34, 34)
MUTED = RGBColor(90, 90, 90)
BG = RGBColor(248, 250, 252)
LIGHT = RGBColor(229, 238, 246)
WHITE = RGBColor(255, 255, 255)


SLIDES = [
    {
        "title": "Agentic AI for Android PR Reviews",
        "subtitle": "From static checks to workflow-aware review comments in GitHub",
        "footer": "Demo pack generated from this project's current GitHub Action + review-agent design",
    },
    {
        "title": "Problem We Are Solving",
        "bullets": [
            "Manual PR review is high-value but repetitive: lint, hardcoded values, architecture drift, security checks, and test quality.",
            "Important issues are often found late, after reviewer time is already spent.",
            "Generic AI summaries are helpful, but they do not automatically operate inside the delivery workflow.",
            "We need review assistance that is contextual, consistent, and actionable at the exact line of change.",
        ],
    },
    {
        "title": "Why This Is Agentic — Not Just Prompting",
        "bullets": [
            "Trigger-aware: starts automatically when a pull request is opened or updated.",
            "Context-aware: reads PR diff, Android lint output, and unit-test coverage context.",
            "Role-aware: evaluates the change through specialist reviewer personas.",
            "Action-oriented: converts findings into structured output and posts review comments back into GitHub.",
            "Feedback-loop ready: developers fix issues in the same PR flow and re-run the agent on the next push.",
        ],
    },
    {
        "title": "End-to-End Flow",
        "type": "diagram",
        "caption": "Current workflow: PR event → context collection → master reviewer → specialist reasoning → structured findings → inline PR comments",
    },
    {
        "title": "Architecture of the Solution",
        "columns": [
            {
                "heading": "1. Orchestration Layer",
                "items": [
                    "GitHub Actions workflow (`.github/workflows/ai-pr-reviewer.yml`)",
                    "Diff generation (`pr_diff.patch`)",
                    "Lint + coverage context collection",
                    "Python controller (`.github/scripts/run_agent.py`)",
                ],
            },
            {
                "heading": "2. Intelligence Layer",
                "items": [
                    "`master-reviewer-agent.md` as lead reviewer",
                    "Architecture / Logic sub-agent",
                    "Compose UI sub-agent",
                    "Security / Config sub-agent",
                    "Test / QA sub-agent",
                ],
            },
            {
                "heading": "3. Output Layer",
                "items": [
                    "Structured XML findings",
                    "Severity ordering",
                    "Exact file + line extraction",
                    "Inline GitHub review comments",
                    "Coverage alerts for business-logic files below 75%",
                ],
            },
        ],
    },
    {
        "title": "What This Agent Checks",
        "bullets": [
            "Android lint and Kotlin code quality rules",
            "Hardcoded strings, colors, numbers, and static literals",
            "Correct use of BuildConfig vs Android resources",
            "Compose UI quality, accessibility, and recomposition hygiene",
            "Architecture boundaries across ViewModel / Repository / data layers",
            "Security and configuration risks in build scripts and API usage",
            "Testability and coverage gaps in business logic",
        ],
    },
    {
        "title": "What the Demo Will Show Live",
        "bullets": [
            "Step 1: Open a PR with intentional issues in Android/Kotlin files.",
            "Step 2: GitHub Action generates diff, runs tests, coverage, and lint.",
            "Step 3: Agent reviews only changed code and returns structured findings.",
            "Step 4: Findings appear as line-level PR comments, not only as a generic summary.",
            "Step 5: Coverage alerts are added separately for repository / use-case logic when below threshold.",
        ],
        "callout": "Best demo line: 'The agent behaves like a staff engineer that reads context, reasons across specialties, and comments directly where the developer needs to act.'",
    },
    {
        "title": "Business Impact",
        "bullets": [
            "Faster feedback on every pull request",
            "More consistent review quality across reviewers and repos",
            "Less reviewer time spent on repetitive checks",
            "Earlier detection of Android-specific quality and security issues",
            "Reusable review policy encoded as version-controlled agent prompts",
        ],
        "metrics": [
            ("Cycle time", "Reduce back-and-forth by shifting common issues earlier"),
            ("Quality signal", "Lint + coverage + structured review in one pass"),
            ("Scalability", "Apply same pattern across multiple Android projects"),
        ],
    },
    {
        "title": "Rollout Recommendation",
        "bullets": [
            "Start in review-only mode on one Android repository.",
            "Track false positives and tune the prompt files for 1–2 sprints.",
            "Keep humans in the loop; use the agent to accelerate, not replace, engineering judgment.",
            "Then templatize the workflow and prompt set for additional mobile projects.",
        ],
    },
    {
        "title": "Close / Ask",
        "bullets": [
            "Approve a pilot for one repo or one team.",
            "Use this as a standard AI agent use case inside the SDLC.",
            "Measure review turnaround time, comment usefulness, and escaped defect reduction.",
        ],
        "quote": "This is a practical example of agentic AI: not just generating text, but participating in an engineering workflow and producing accountable review actions.",
    },
]


def font(size: int):
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Supplemental/Helvetica.ttc",
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


FONT_TITLE = font(34)
FONT_BOX = font(20)
FONT_SMALL = font(17)


def draw_box(draw, xy, text, fill, outline=(180, 196, 214), text_fill=(20, 20, 20), radius=18):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=2)
    x1, y1, x2, y2 = xy
    max_width = x2 - x1 - 24
    lines = []
    for paragraph in text.split("\n"):
        words = paragraph.split()
        current = ""
        for word in words:
            trial = word if not current else f"{current} {word}"
            if draw.textlength(trial, font=FONT_BOX) <= max_width:
                current = trial
            else:
                if current:
                    lines.append(current)
                current = word
        if current:
            lines.append(current)
    line_height = 24
    total_h = len(lines) * line_height
    current_y = y1 + ((y2 - y1 - total_h) / 2)
    for line in lines:
        text_w = draw.textlength(line, font=FONT_BOX)
        draw.text((x1 + (x2 - x1 - text_w) / 2, current_y), line, font=FONT_BOX, fill=text_fill)
        current_y += line_height


def arrow(draw, start, end, fill=(73, 96, 122), width=5, head=10):
    draw.line([start, end], fill=fill, width=width)
    x1, y1 = start
    x2, y2 = end
    if abs(x2 - x1) >= abs(y2 - y1):
        direction = 1 if x2 >= x1 else -1
        draw.polygon([
            (x2, y2),
            (x2 - direction * head * 2, y2 - head),
            (x2 - direction * head * 2, y2 + head),
        ], fill=fill)
    else:
        direction = 1 if y2 >= y1 else -1
        draw.polygon([
            (x2, y2),
            (x2 - head, y2 - direction * head * 2),
            (x2 + head, y2 - direction * head * 2),
        ], fill=fill)


def create_flow_diagram(path: Path):
    image = Image.new("RGB", (1600, 900), color=(248, 250, 252))
    draw = ImageDraw.Draw(image)

    draw.text((55, 30), "Agentic AI Review Flow", font=FONT_TITLE, fill=(14, 44, 84))
    draw.text((55, 78), "PR event → context collection → specialist reasoning → GitHub review comments", font=FONT_SMALL, fill=(80, 96, 114))

    boxes = {
        "pr": (60, 150, 290, 240),
        "action": (350, 150, 610, 240),
        "diff": (680, 90, 920, 180),
        "lint": (680, 220, 920, 310),
        "coverage": (680, 350, 920, 440),
        "runner": (990, 180, 1250, 270),
        "master": (1310, 180, 1540, 270),
        "arch": (1110, 380, 1310, 470),
        "ui": (1330, 380, 1530, 470),
        "sec": (1110, 520, 1310, 610),
        "qa": (1330, 520, 1530, 610),
        "xml": (990, 680, 1250, 770),
        "comments": (1310, 680, 1540, 770),
    }

    fill_main = (230, 240, 250)
    fill_agent = (223, 245, 236)
    fill_output = (255, 244, 214)

    draw_box(draw, boxes["pr"], "Developer opens\nor updates PR", fill_main)
    draw_box(draw, boxes["action"], "GitHub Action\nstarts workflow", fill_main)
    draw_box(draw, boxes["diff"], "Generate\npr_diff.patch", fill_main)
    draw_box(draw, boxes["lint"], "Run Android lint\ncollect lint context", fill_main)
    draw_box(draw, boxes["coverage"], "Run unit tests\ncollect coverage context", fill_main)
    draw_box(draw, boxes["runner"], "run_agent.py\norchestrator", fill_main)
    draw_box(draw, boxes["master"], "Master reviewer\nagent", fill_agent)
    draw_box(draw, boxes["arch"], "Architecture /\nLogic agent", fill_agent)
    draw_box(draw, boxes["ui"], "Compose UI\nagent", fill_agent)
    draw_box(draw, boxes["sec"], "Security / Config\nagent", fill_agent)
    draw_box(draw, boxes["qa"], "Test / QA\nagent", fill_agent)
    draw_box(draw, boxes["xml"], "Structured XML\nfindings", fill_output)
    draw_box(draw, boxes["comments"], "GitHub inline review\ncomments + coverage alerts", fill_output)

    arrow(draw, (290, 195), (350, 195))
    arrow(draw, (610, 195), (680, 135))
    arrow(draw, (610, 195), (680, 265))
    arrow(draw, (610, 195), (680, 395))
    arrow(draw, (920, 135), (990, 210))
    arrow(draw, (920, 265), (990, 225))
    arrow(draw, (920, 395), (990, 240))
    arrow(draw, (1250, 225), (1310, 225))

    arrow(draw, (1425, 270), (1210, 380))
    arrow(draw, (1425, 270), (1430, 380))
    arrow(draw, (1425, 270), (1210, 520))
    arrow(draw, (1425, 270), (1430, 520))

    arrow(draw, (1210, 470), (1120, 680))
    arrow(draw, (1430, 470), (1120, 680))
    arrow(draw, (1210, 610), (1120, 725))
    arrow(draw, (1430, 610), (1120, 725))
    arrow(draw, (1250, 725), (1310, 725))

    draw.text((1060, 835), "Outcome: exact file + line findings appear where developers already work", font=FONT_SMALL, fill=(56, 72, 88))
    image.save(path)


def set_background(slide):
    fill = slide.background.fill
    fill.solid()
    fill.fore_color.rgb = BG


def add_title(slide, title, subtitle=None):
    title_box = slide.shapes.add_textbox(Inches(0.55), Inches(0.35), Inches(12), Inches(0.8))
    tf = title_box.text_frame
    p = tf.paragraphs[0]
    run = p.add_run()
    run.text = title
    run.font.name = "Aptos"
    run.font.size = Pt(28)
    run.font.bold = True
    run.font.color.rgb = PRIMARY
    if subtitle:
        sub_box = slide.shapes.add_textbox(Inches(0.58), Inches(1.1), Inches(11.6), Inches(0.5))
        sub_tf = sub_box.text_frame
        p2 = sub_tf.paragraphs[0]
        r2 = p2.add_run()
        r2.text = subtitle
        r2.font.name = "Aptos"
        r2.font.size = Pt(13)
        r2.font.color.rgb = MUTED


def add_bullets_slide(prs, title, bullets, callout=None, quote=None, metrics=None):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    set_background(slide)
    add_title(slide, title)

    body = slide.shapes.add_textbox(Inches(0.8), Inches(1.45), Inches(7.0), Inches(4.8))
    tf = body.text_frame
    tf.word_wrap = True
    for index, bullet in enumerate(bullets):
        p = tf.paragraphs[0] if index == 0 else tf.add_paragraph()
        p.text = bullet
        p.level = 0
        p.space_after = Pt(10)
        p.font.name = "Aptos"
        p.font.size = Pt(22)
        p.font.color.rgb = TEXT

    if callout:
        shape = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, Inches(8.0), Inches(1.7), Inches(4.7), Inches(1.9))
        shape.fill.solid()
        shape.fill.fore_color.rgb = LIGHT
        shape.line.color.rgb = ACCENT
        tx = shape.text_frame
        tx.word_wrap = True
        p = tx.paragraphs[0]
        p.text = callout
        p.font.name = "Aptos"
        p.font.size = Pt(18)
        p.font.bold = True
        p.font.color.rgb = PRIMARY

    if quote:
        shape = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, Inches(7.9), Inches(4.2), Inches(4.8), Inches(1.7))
        shape.fill.solid()
        shape.fill.fore_color.rgb = LIGHT
        shape.line.color.rgb = ACCENT
        tx = shape.text_frame
        tx.word_wrap = True
        p = tx.paragraphs[0]
        p.text = quote
        p.alignment = PP_ALIGN.LEFT
        p.font.name = "Aptos"
        p.font.size = Pt(16)
        p.font.italic = True
        p.font.color.rgb = PRIMARY

    if metrics:
        top = 4.0
        for idx, (metric, detail) in enumerate(metrics):
            y = Inches(top + idx * 0.75)
            shape = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, Inches(7.9), y, Inches(4.8), Inches(0.62))
            shape.fill.solid()
            shape.fill.fore_color.rgb = WHITE
            shape.line.color.rgb = ACCENT
            tx = shape.text_frame
            tx.word_wrap = True
            p = tx.paragraphs[0]
            p.text = f"{metric}: {detail}"
            p.font.name = "Aptos"
            p.font.size = Pt(14)
            p.font.color.rgb = TEXT


def add_columns_slide(prs, title, columns):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    set_background(slide)
    add_title(slide, title)

    positions = [0.6, 4.35, 8.1]
    for idx, column in enumerate(columns):
        x = Inches(positions[idx])
        box = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, x, Inches(1.55), Inches(3.1), Inches(4.8))
        box.fill.solid()
        box.fill.fore_color.rgb = WHITE
        box.line.color.rgb = ACCENT
        tf = box.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = column["heading"]
        p.font.name = "Aptos"
        p.font.size = Pt(20)
        p.font.bold = True
        p.font.color.rgb = PRIMARY
        for item in column["items"]:
            bullet = tf.add_paragraph()
            bullet.text = item
            bullet.level = 0
            bullet.font.name = "Aptos"
            bullet.font.size = Pt(15)
            bullet.font.color.rgb = TEXT


def add_diagram_slide(prs, title, caption):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    set_background(slide)
    add_title(slide, title)
    slide.shapes.add_picture(str(DIAGRAM_PATH), Inches(0.45), Inches(1.45), width=Inches(12.35))
    caption_box = slide.shapes.add_textbox(Inches(0.7), Inches(6.5), Inches(11.6), Inches(0.45))
    tf = caption_box.text_frame
    p = tf.paragraphs[0]
    p.text = caption
    p.font.name = "Aptos"
    p.font.size = Pt(13)
    p.font.color.rgb = MUTED


def add_title_slide(prs, title, subtitle, footer):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    set_background(slide)

    band = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.RECTANGLE, Inches(0), Inches(0), Inches(13.33), Inches(1.2))
    band.fill.solid()
    band.fill.fore_color.rgb = PRIMARY
    band.line.fill.background()

    title_box = slide.shapes.add_textbox(Inches(0.7), Inches(1.6), Inches(12), Inches(1.2))
    tf = title_box.text_frame
    p = tf.paragraphs[0]
    p.text = title
    p.font.name = "Aptos"
    p.font.size = Pt(30)
    p.font.bold = True
    p.font.color.rgb = PRIMARY

    sub_box = slide.shapes.add_textbox(Inches(0.72), Inches(2.55), Inches(10.8), Inches(1.0))
    stf = sub_box.text_frame
    sp = stf.paragraphs[0]
    sp.text = subtitle
    sp.font.name = "Aptos"
    sp.font.size = Pt(21)
    sp.font.color.rgb = TEXT

    highlight = slide.shapes.add_shape(MSO_AUTO_SHAPE_TYPE.ROUNDED_RECTANGLE, Inches(0.72), Inches(3.7), Inches(11.6), Inches(1.6))
    highlight.fill.solid()
    highlight.fill.fore_color.rgb = LIGHT
    highlight.line.color.rgb = ACCENT
    htf = highlight.text_frame
    htf.word_wrap = True
    hp = htf.paragraphs[0]
    hp.text = "Key message: This solution behaves like an agent — it observes PR context, reasons across specialist reviewer roles, and performs a workflow action by posting code-review comments directly into GitHub."
    hp.font.name = "Aptos"
    hp.font.size = Pt(20)
    hp.font.bold = True
    hp.font.color.rgb = PRIMARY

    footer_box = slide.shapes.add_textbox(Inches(0.75), Inches(6.8), Inches(12), Inches(0.3))
    ftf = footer_box.text_frame
    fp = ftf.paragraphs[0]
    fp.text = footer
    fp.font.name = "Aptos"
    fp.font.size = Pt(11)
    fp.font.color.rgb = MUTED


def build_deck():
    ROOT.mkdir(parents=True, exist_ok=True)
    create_flow_diagram(DIAGRAM_PATH)

    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    for slide_data in SLIDES:
        if "subtitle" in slide_data:
            add_title_slide(prs, slide_data["title"], slide_data["subtitle"], slide_data["footer"])
        elif slide_data.get("type") == "diagram":
            add_diagram_slide(prs, slide_data["title"], slide_data["caption"])
        elif "columns" in slide_data:
            add_columns_slide(prs, slide_data["title"], slide_data["columns"])
        else:
            add_bullets_slide(
                prs,
                slide_data["title"],
                slide_data["bullets"],
                callout=slide_data.get("callout"),
                quote=slide_data.get("quote"),
                metrics=slide_data.get("metrics"),
            )

    prs.save(PPTX_PATH)
    print(f"Created {PPTX_PATH}")
    print(f"Created {DIAGRAM_PATH}")


if __name__ == "__main__":
    build_deck()

