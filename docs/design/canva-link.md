# Canva case study

## v3 (current): 43 slides
- **PDF:** [export/Hey-Payments-UIUX-Case-Study-v3.pdf](export/Hey-Payments-UIUX-Case-Study-v3.pdf). Also uploaded to Canva Uploads; open it as a design.
- **Changes from v2:** the syllabus map is replaced by a 5-phase case-study roadmap (slide 2); new Market research slide (12); ribbon label is now "Topic covered"; section footers show slide ranges.

## v2: 42 slides, rebuilt
- **PDF:** [export/Hey-Payments-UIUX-Case-Study-v2.pdf](export/Hey-Payments-UIUX-Case-Study-v2.pdf)
- **In Canva:** uploaded to your Canva **Uploads** as `Hey-Payments-UIUX-Case-Study-v2.pdf`. Click it and choose to edit it as a design. It opens as an editable 1920×1080 presentation.
- **Source:** [deck-source/](deck-source/) holds the HTML/CSS/Python that renders the deck. `render.sh` rebuilds the PDF and runs a layout checker that flags overlaps, overflow and empty space.
- **Structure:** every content slide has a proper topic heading, the Hey Payments work for that topic, and a footer ribbon with three parts: **Syllabus topic · Method/source [R#] · Project artefact**. Slide 41 lists references R1–R31.
- **Sample data:** slides marked "Sample data · replace with your study results" (v3: 11, 12, 17, 29, 30, 36) contain illustrative numbers. Replace them with your real research before submitting.

## v1 (superseded)
- Canva AI-generated deck `DAHasxhpzAY`, https://canva.link/pq129yonxgudr32. Kept for reference only.

## Notes for the coding agent
- Start with [ui-spec.md](ui-spec.md) and [reference/screens.html](reference/screens.html). The deck explains the reasoning; tokens.scss and tokens.json hold the exact values.
- In the v3 deck, slides 32–34 are the hi-fi screens (payer, handshake, merchant/admin) and slide 35 covers motion.
