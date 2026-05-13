#!/usr/bin/env python3
"""
Generate two 1080x1080 LinkedIn graphics using Pillow (no Mermaid CLI needed).

Run:
    pip install pillow
    python3 docs/presentation/generate_linkedin_images.py
"""

from PIL import Image, ImageDraw, ImageFont
import os, textwrap

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
SIZE = (1080, 1080)


def _font(size, bold=False):
    candidates = [
        "/System/Library/Fonts/Helvetica.ttc",
        "/Library/Fonts/Arial Bold.ttf" if bold else "/Library/Fonts/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for p in candidates:
        if os.path.exists(p):
            try: return ImageFont.truetype(p, size)
            except: pass
    return ImageFont.load_default()


def rr(draw, xy, r=18, fill=None, outline=None, w=2):
    draw.rounded_rectangle(xy, radius=r, fill=fill, outline=outline, width=w)


# ─── NON-TECHNICAL ────────────────────────────────────────────────────────────
def generate_non_technical():
    img = Image.new("RGB", SIZE, "#F8FAFC")
    draw = ImageDraw.Draw(img)

    # gradient background
    for y in range(SIZE[1]):
        t = y / SIZE[1]
        draw.line([(0,y),(1080,y)], fill=(
            int(248+(220-248)*t), int(250+(238-250)*t), int(252+(255-252)*t)))

    ft  = _font(44, True); fs = _font(22); fl = _font(20, True)
    fd  = _font(18);       ff = _font(17)

    # header
    title = "AI Agent Development Workflow"
    tw = draw.textbbox((0,0), title, font=ft)[2]
    draw.text((540-tw//2, 32), title, font=ft, fill="#0F172A")
    sub = "Jira → Copilot Build → Gemini Review  |  Zero Manual Handoffs"
    sw = draw.textbbox((0,0), sub, font=fs)[2]
    draw.text((540-sw//2, 86), sub, font=fs, fill="#475569")
    draw.line([(80,122),(1000,122)], fill="#CBD5E1", width=2)

    # 5 stage cards
    stages = [
        ("🧑‍💻","PLAN","./jira-sync\n--implement KAN-XX","#DBEAFE","#2563EB","#EFF6FF","#93C5FD"),
        ("🤖","BUILD","Copilot Agent builds\nfeature autonomously","#DCFCE7","#16A34A","#F0FDF4","#86EFAC"),
        ("📤","SHIP","Developer raises\nPR on GitHub","#FEF9C3","#CA8A04","#FEFCE8","#FDE047"),
        ("🔍","REVIEW","Gemini 2.5 Flash\nreviews code","#F3E8FF","#7C3AED","#FAF5FF","#C4B5FD"),
        ("✅","RESULT","Line-level feedback\non PR automatically","#CCFBF1","#0D9488","#F0FDFA","#5EEAD4"),
    ]
    bw, bh, gap = 168, 162, 28
    total = len(stages)*bw + (len(stages)-1)*gap
    sx = (1080-total)//2
    BY = 148

    for i,(icon,label,desc,bbg,bborder,boxbg,boxborder) in enumerate(stages):
        bx = sx + i*(bw+gap)
        draw.rounded_rectangle([bx+4,BY+4,bx+bw+4,BY+bh+4], radius=18, fill="#E2E8F0")
        rr(draw, [bx,BY,bx+bw,BY+bh], r=18, fill=boxbg, outline=boxborder, w=2)
        cx = bx+bw//2
        draw.ellipse([cx-28,BY+10,cx+28,BY+66], fill=bbg, outline=bborder, width=2)
        iw = draw.textbbox((0,0),icon,font=_font(32))[2]
        draw.text((cx-iw//2, BY+18), icon, font=_font(32), fill=bborder)
        lw = draw.textbbox((0,0),label,font=fl)[2]
        draw.text((cx-lw//2, BY+72), label, font=fl, fill="#0F172A")
        dy = BY+100
        for ln in desc.split("\n"):
            lw2 = draw.textbbox((0,0),ln,font=fd)[2]
            draw.text((cx-lw2//2, dy), ln, font=fd, fill="#334155"); dy += 25
        if i < len(stages)-1:
            ax = bx+bw+4; ay = BY+bh//2
            draw.line([(ax,ay),(ax+gap-6,ay)], fill="#94A3B8", width=3)
            draw.polygon([(ax+gap-8,ay-6),(ax+gap-8,ay+6),(ax+gap,ay)], fill="#94A3B8")

    # pipeline banner
    by2 = 358
    rr(draw,[80,by2,1000,by2+64], r=14, fill="#0F172A", outline="#0F172A", w=0)
    bt = "⚡  Jira Story  →  Autonomous Code  →  AI Code Review  →  PR Feedback"
    bw2 = draw.textbbox((0,0),bt,font=fs)[2]
    draw.text((540-bw2//2, by2+18), bt, font=fs, fill="#FFFFFF")

    # 3 detail boxes
    details = [
        ("📌  Step 1","Run: ./jira-sync\nFetches story into\ndocs/jira/current_story.md","#EFF6FF","#2563EB"),
        ("🤖  Step 2","Copilot Agent reads context\nImplements feature:\nMVVM + Compose + Tests","#F0FDF4","#16A34A"),
        ("🔍  Step 3","GitHub Action triggers\nGemini 2.5 reviews PR\nPosts line-level comments","#FAF5FF","#7C3AED"),
    ]
    dby = 448; dbw = 270; dbh = 172; dgap = 40
    dtotal = 3*dbw + 2*dgap
    dsx = (1080-dtotal)//2
    for i,(dtitle,dbody,dbg,dborder) in enumerate(details):
        dx = dsx + i*(dbw+dgap)
        draw.rounded_rectangle([dx+3,dby+3,dx+dbw+3,dby+dbh+3], radius=14, fill="#E2E8F0")
        rr(draw,[dx,dby,dx+dbw,dby+dbh], r=14, fill=dbg, outline=dborder, w=2)
        tw2 = draw.textbbox((0,0),dtitle,font=fl)[2]
        draw.text((dx+dbw//2-tw2//2, dby+12), dtitle, font=fl, fill="#0F172A")
        draw.line([(dx+16,dby+42),(dx+dbw-16,dby+42)], fill=dborder, width=1)
        yy = dby+52
        for ln in dbody.split("\n"):
            lw3 = draw.textbbox((0,0),ln,font=fd)[2]
            draw.text((dx+dbw//2-lw3//2, yy), ln, font=fd, fill="#334155"); yy += 26
        if i < 2:
            ax2 = dx+dbw+6; ay2 = dby+dbh//2
            draw.line([(ax2,ay2),(ax2+dgap-8,ay2)], fill="#94A3B8", width=3)
            draw.polygon([(ax2+dgap-8,ay2-6),(ax2+dgap-8,ay2+6),(ax2+dgap,ay2)], fill="#94A3B8")

    # insight
    iy = 650
    rr(draw,[80,iy,1000,iy+84], r=16, fill="#FFFBEB", outline="#F59E0B", w=3)
    draw.text((108, iy+12), "💡  Key Insight", font=fl, fill="#92400E")
    draw.text((108, iy+46),
              "No human writes code manually. The agent does it all — from story description to reviewed PR.",
              font=fd, fill="#78350F")

    # tech pills
    py2 = 764; draw.text((80,py2), "Stack:", font=fl, fill="#0F172A")
    pills = [("Jira REST","#DBEAFE","#2563EB"),("GitHub Copilot","#DCFCE7","#16A34A"),
             ("GitHub Actions","#F3F4F6","#374151"),("Gemini 2.5","#F3E8FF","#7C3AED"),
             ("Android/Kotlin","#CCFBF1","#0D9488")]
    px2 = 172
    for (pill,pbg,pb) in pills:
        pw2 = draw.textbbox((0,0),pill,font=ff)[2]+24
        rr(draw,[px2,py2-4,px2+pw2,py2+32], r=14, fill=pbg, outline=pb, w=2)
        draw.text((px2+12,py2+2), pill, font=ff, fill=pb); px2 += pw2+10

    # footer
    draw.line([(80,826),(1000,826)], fill="#CBD5E1", width=1)
    ht = "#AgenticAI  #GitHubCopilot  #AndroidDev  #AIEngineer  #DevAutomation"
    hw = draw.textbbox((0,0),ht,font=ff)[2]
    draw.text((540-hw//2, 844), ht, font=ff, fill="#64748B")
    ct = "Built by Meet Janani  •  Agentic AI Development Flow"
    cw2 = draw.textbbox((0,0),ct,font=ff)[2]
    draw.text((540-cw2//2, 878), ct, font=ff, fill="#94A3B8")

    path = os.path.join(OUT_DIR, "linkedin-non-technical-1080x1080.png")
    img.save(path, "PNG", dpi=(300,300))
    print(f"✅  Saved: {path}")


# ─── TECHNICAL ────────────────────────────────────────────────────────────────
def generate_technical():
    img = Image.new("RGB", SIZE, "#F8FAFC")
    draw = ImageDraw.Draw(img)
    for y in range(SIZE[1]):
        t = y/SIZE[1]
        draw.line([(0,y),(1080,y)], fill=(
            int(248+(240-248)*t), int(250+(245-250)*t), int(252+(255-252)*t)))

    ft = _font(40,True); fs = _font(19); fsc = _font(21,True)
    fl = _font(17,True); fc = _font(15); ff = _font(15)

    # header
    t = "Agentic SDLC Pipeline"
    tw = draw.textbbox((0,0),t,font=ft)[2]
    draw.text((540-tw//2,28), t, font=ft, fill="#0F172A")
    s = "Jira → Copilot Generation → Gemini AI Review  |  No Manual Handoffs"
    sw = draw.textbbox((0,0),s,font=fs)[2]
    draw.text((540-sw//2,78), s, font=fs, fill="#475569")
    draw.line([(80,108),(1000,108)], fill="#CBD5E1", width=2)

    # LOCAL panel
    rr(draw,[40,120,516,528], r=18, fill="#F0FDF4", outline="#16A34A", w=3)
    draw.text((68,130),"🧩  Local Machine", font=fsc, fill="#14532D")
    draw.line([(68,162),(488,162)], fill="#86EFAC", width=2)

    local_steps = [
        ("🖥️","./jira-sync --implement KAN-XX","Triggers story implementation"),
        ("📜","implement_story.py","Fetches issue via Jira REST API"),
        ("🗂️","docs/jira/current_story.md","Context files written locally"),
        ("🤖","Copilot @workspace","Reads story + architecture rules"),
        ("🛠️","Code Generated","MVVM + Compose + StateFlow\nModel / Repo / ViewModel / UI / Tests"),
    ]
    ly = 174
    for icon,title,desc in local_steps:
        rr(draw,[58,ly,496,ly+60], r=10, fill="#FFFFFF", outline="#BBF7D0", w=1)
        draw.text((74,ly+8), icon, font=fl, fill="#15803D")
        draw.text((116,ly+8), title, font=fl, fill="#0F172A")
        for di,dl in enumerate(desc.split("\n")):
            draw.text((116,ly+29+di*18), dl, font=fc, fill="#475569")
        ly += 68

    # handoff arrow
    draw.line([(516,340),(564,340)], fill="#2563EB", width=4)
    draw.polygon([(552,330),(552,350),(568,340)], fill="#2563EB")
    hl = "PR Raised"
    hw = draw.textbbox((0,0),hl,font=fc)[2]
    draw.text((540-hw//2,316), hl, font=fc, fill="#2563EB")

    # GITHUB panel
    rr(draw,[564,120,1040,528], r=18, fill="#FFF4E5", outline="#D97706", w=3)
    draw.text((590,130),"☁️  GitHub Pipeline", font=fsc, fill="#78350F")
    draw.line([(590,162),(1012,162)], fill="#FDE68A", width=2)

    gh_steps = [
        ("📤","Push Branch + Open PR","Triggers review workflow"),
        ("⚙️","ai-pr-reviewer.yml","GitHub Action runs on PR event"),
        ("🧪","Build Signals","PR diff + Tests + Lint + Coverage"),
        ("🧠","Prompt Builder","Adds human PR comment history"),
        ("🔍","Gemini 2.5 Flash","Full AI code review pass"),
        ("💬","Line-Level Comments","Posted directly on PR by Action"),
    ]
    gy = 174
    for icon,title,desc in gh_steps:
        rr(draw,[582,gy,1022,gy+54], r=10, fill="#FFFFFF", outline="#FDE68A", w=1)
        draw.text((598,gy+8), icon, font=fl, fill="#D97706")
        draw.text((640,gy+8), title, font=fl, fill="#0F172A")
        draw.text((640,gy+30), desc, font=fc, fill="#475569")
        gy += 62

    # timeline bar
    bar_y = 544
    phases = [("#2563EB","1 ./jira-sync"),("#16A34A","2 Copilot"),
              ("#D97706","3 PR + CI"),("#7C3AED","4 Gemini"),("#0D9488","5 Feedback")]
    segw = (960)//len(phases)
    for i,(col,lbl) in enumerate(phases):
        sx2 = 80+i*segw
        rr(draw,[sx2+2,bar_y,sx2+segw-2,bar_y+42], r=10, fill=col, outline=col, w=0)
        lw2 = draw.textbbox((0,0),lbl,font=fc)[2]
        draw.text((sx2+segw//2-lw2//2, bar_y+12), lbl, font=fc, fill="#FFFFFF")

    # outcome
    oy = 600
    rr(draw,[80,oy,1000,oy+72], r=14, fill="#0F172A", outline="#0F172A", w=0)
    ot = "✅  Every PR gets structured AI feedback with exact file + line references"
    ow = draw.textbbox((0,0),ot,font=fs)[2]
    draw.text((540-ow//2, oy+20), ot, font=fs, fill="#FFFFFF")

    # file map
    fm_y = 688
    rr(draw,[80,fm_y,1000,fm_y+178], r=14, fill="#F1F5F9", outline="#CBD5E1", w=2)
    draw.text((100,fm_y+12),"📁  Key Files in This Pipeline", font=fl, fill="#0F172A")
    files = [
        ("tools/jira_context_sync/implement_story.py","→ Fetches story, writes context"),
        ("docs/jira/current_story.md","→ Rich Copilot @workspace context"),
        (".github/scripts/story_agent_prompt.md","→ Architecture rules for Copilot"),
        (".github/workflows/ai-pr-reviewer.yml","→ GitHub Action: diff + Gemini review"),
        (".github/scripts/run_agent.py","→ Orchestrates review + PR comment posting"),
    ]
    fy = fm_y+44
    for fname,fdesc in files:
        draw.text((108,fy), f"• {fname}", font=fc, fill="#1E40AF")
        fw3 = draw.textbbox((0,0),f"• {fname}",font=fc)[2]
        draw.text((108+fw3+10,fy), fdesc, font=fc, fill="#475569"); fy+=26

    # footer
    draw.line([(80,886),(1000,886)], fill="#CBD5E1", width=1)
    ht = "#AgenticAI  #GitHubCopilot  #AndroidDev  #AIEngineer  #DevAutomation"
    hw3 = draw.textbbox((0,0),ht,font=ff)[2]
    draw.text((540-hw3//2,904), ht, font=ff, fill="#64748B")
    ct = "Built by Meet Janani  •  Agentic AI SDLC Pipeline"
    cw3 = draw.textbbox((0,0),ct,font=ff)[2]
    draw.text((540-cw3//2,938), ct, font=ff, fill="#94A3B8")

    path = os.path.join(OUT_DIR, "linkedin-technical-1080x1080.png")
    img.save(path, "PNG", dpi=(300,300))
    print(f"✅  Saved: {path}")


if __name__ == "__main__":
    generate_non_technical()
    generate_technical()
    print("\n🎉  Both 1080x1080 images saved in docs/presentation/")