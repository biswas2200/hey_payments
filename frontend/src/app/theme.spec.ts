import { describe, it, expect, beforeEach } from 'vitest';

describe('Design Tokens & Typography', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });

  it('should define Calm Ledger v1 design tokens on :root', () => {
    const style = getComputedStyle(document.documentElement);
    // Primary brand token
    expect(style.getPropertyValue('--hp-primary').trim()).toBe('#0F766E');
    expect(style.getPropertyValue('--hp-bg').trim()).toBe('#F7F5F0');
  });

  it('should activate dark tokens when dark theme is applied', () => {
    document.documentElement.setAttribute('data-theme', 'dark');
    const style = getComputedStyle(document.documentElement);
    expect(style.getPropertyValue('--hp-bg').trim()).toBe('#0B1420');
    expect(style.getPropertyValue('--hp-primary').trim()).toBe('#2DD4BF');
  });

  it('should verify Inter font family is declared', () => {
    const style = getComputedStyle(document.documentElement);
    const bodyFont = style.getPropertyValue('--hp-font-body');
    expect(bodyFont).toContain('Inter');
  });
});
