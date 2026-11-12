import { describe, it, expect, beforeAll, beforeEach } from 'vitest';
import * as fs from 'node:fs';

describe('Design Tokens & Typography', () => {
  beforeAll(() => {
    const tokensPath = fs.existsSync('src/styles/tokens.scss')
      ? 'src/styles/tokens.scss'
      : 'frontend/src/styles/tokens.scss';
    // Strip SCSS single-line comments for JSDOM CSSOM parser compatibility
    const cssContent = fs.readFileSync(tokensPath, 'utf-8').replace(/\/\/.*$/gm, '');

    const styleEl = document.createElement('style');
    styleEl.textContent = cssContent;
    document.head.appendChild(styleEl);
  });

  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });

  it('should define Calm Ledger v1 design tokens on :root', () => {
    const style = getComputedStyle(document.documentElement);
    expect(style.getPropertyValue('--hp-primary').trim().toLowerCase()).toBe('#0f766e');
    expect(style.getPropertyValue('--hp-bg').trim().toLowerCase()).toBe('#f7f5f0');
  });

  it('should activate dark tokens when data-theme is dark', () => {
    document.documentElement.setAttribute('data-theme', 'dark');
    const style = getComputedStyle(document.documentElement);
    expect(style.getPropertyValue('--hp-bg').trim().toLowerCase()).toBe('#0b1420');
    expect(style.getPropertyValue('--hp-primary').trim().toLowerCase()).toBe('#2dd4bf');
  });

  it('should declare Inter in the body font family token', () => {
    const style = getComputedStyle(document.documentElement);
    const bodyFont = style.getPropertyValue('--hp-font-body');
    expect(bodyFont).toContain('Inter');
  });
});
