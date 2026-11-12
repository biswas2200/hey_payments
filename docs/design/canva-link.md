# Canva case study

## v2 (current): 42 slides, rebuilt
- **PDF:** [export/Hey-Payments-UIUX-Case-Study-v2.pdf](export/Hey-Payments-UIUX-Case-Study-v2.pdf)
- **In Canva:** uploaded to your Canva **Uploads** as `Hey-Payments-UIUX-Case-Study-v2.pdf`. Click it and choose to edit it as a design. It opens as an editable 1920×1080 presentation.
- **Source:** [deck-source/](deck-source/) holds the HTML/CSS/Python that renders the deck. `render.sh` rebuilds the PDF and runs a layout checker that flags overlaps, overflow and empty space.
- **Structure:** every content slide has a proper topic heading, the Hey Payments work for that topic, and a footer ribbon with three parts: **Syllabus topic · Method/source [R#] · Project artefact**. Slide 41 lists references R1–R31.
- **Sample data:** slides marked "Sample data · replace with your study results" (11, 16, 28, 29, 35) contain illustrative numbers. Replace them with your real research before submitting.

## v1 (superseded)
- Canva AI-generated deck `DAHasxhpzAY`, https://canva.link/pq129yonxgudr32. Kept for reference only.

## Notes for the coding agent
- Use v2 for identity, layout, status language and flows. Use [`tokens.json`](tokens.json) for exact values.
- Slides 31–33 are the hi-fi screens to implement (payer, handshake, merchant/admin).
