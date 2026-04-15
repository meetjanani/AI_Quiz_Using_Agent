# Agentic AI Review Demo Pack

This folder contains a same-day presentation pack for demonstrating the Android PR review agent.

## Files
- `Agentic_AI_Code_Review_Demo.pptx` — ready-to-present PowerPoint deck
- `speaker-notes.md` — short talk track for each slide
- `agentic-review-flow.mmd` — editable Mermaid flow diagram source
- `generate_agent_demo_ppt.py` — script that generates the PPT and diagram image
- `agentic-review-flow.png` — generated flow diagram used in the deck

## Regenerate the deck

```bash
python3 docs/presentation/generate_agent_demo_ppt.py
```

## Presentation angle
This deck is optimized for:
- Manager/stakeholder audience
- Engineering audience
- Large-room demo focused on agentic AI use cases

## Core message
This is not just an LLM answering questions. It behaves like an **agentic PR reviewer**:
1. observes the PR context,
2. enriches it with lint + coverage signals,
3. reasons about which specialist reviewers are needed,
4. produces structured findings,
5. posts actionable GitHub review comments back into the delivery workflow.

