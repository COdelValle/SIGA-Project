import { Component, Input } from '@angular/core';
import { MenuIconName } from './menu-item.model';

/**
 * Iconos del menu lateral (SVG inline, sin dependencias externas).
 * Cada caso devuelve un <svg> completo para preservar el namespace SVG.
 */
@Component({
  selector: 'siga-menu-icon',
  template: `
    @switch (name) {
      @case ('home') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M3 10.6 12 3l9 7.6" />
          <path d="M5.5 9.5V21h13V9.5" />
          <path d="M10 21v-6h4v6" />
        </svg>
      }
      @case ('clock') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 2" />
        </svg>
      }
      @case ('notes') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
          <path d="M14 3v5h5" />
          <path d="M9 13h6" />
          <path d="M9 17h6" />
        </svg>
      }
      @case ('attendance') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M16 20v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1" />
          <circle cx="9.5" cy="8" r="3.5" />
          <path d="m16 12 2 2 4-4" />
        </svg>
      }
      @case ('courses') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z" />
          <path d="M4 5.5V20.5" />
          <path d="M9 7h7" />
        </svg>
      }
      @case ('requests') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z" />
          <path d="M8 5H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
          <path d="M8 12h8" />
          <path d="M8 16h5" />
        </svg>
      }
      @case ('users') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M16 20v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1" />
          <circle cx="9.5" cy="8" r="3.5" />
          <path d="M21 20v-1a4 4 0 0 0-3-3.87" />
          <path d="M16 4.5a3.5 3.5 0 0 1 0 7" />
        </svg>
      }
      @case ('roles') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M12 3 5 6v5c0 4.4 3 8.3 7 9.6 4-1.3 7-5.2 7-9.6V6z" />
          <path d="m9 12 2 2 4-4" />
        </svg>
      }
      @case ('subjects') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M12 6.5C10.5 5 8.5 4.5 5 4.5V19c3.5 0 5.5.5 7 2 1.5-1.5 3.5-2 7-2V4.5c-3.5 0-5.5.5-7 2z" />
          <path d="M12 6.5V21" />
        </svg>
      }
      @case ('progress') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M4 20V10" />
          <path d="M10 20V4" />
          <path d="M16 20v-7" />
          <path d="M22 20H2" />
        </svg>
      }
      @case ('register-notes') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
          <path d="M14 3v5h5" />
          <path d="m11 14 3-3 2 2-3 3h-2z" />
        </svg>
      }
      @case ('register-attendance') {
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="h-full w-full" aria-hidden="true">
          <path d="M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z" />
          <path d="M8 5H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
          <path d="m8.5 13.5 2 2 4-4" />
        </svg>
      }
    }
  `,
})
export class MenuIconComponent {
  @Input() name: MenuIconName = 'home';
}
