import {
  Directive,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
} from '@angular/core';

/**
 * Binds a contenteditable element to a string. The text is written with textContent, so markup
 * in a story is never interpreted, and it commits on blur, so re-rendering cannot move the caret
 * mid-edit. Enter commits and Escape reverts.
 */
@Directive({
  selector: '[appEditable]',
  host: {
    role: 'textbox',
    '(blur)': 'commit()',
    '(keydown.enter)': 'finish($event)',
    '(keydown.escape)': 'revert()',
    '(paste)': 'pastePlainText($event)',
  },
})
export class EditableTextDirective implements OnChanges {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  @Input({ alias: 'appEditable', required: true }) text = '';
  @Output() readonly edited = new EventEmitter<string>();

  ngOnChanges(): void {
    this.element.textContent = this.text;
  }

  protected commit(): void {
    const value = (this.element.textContent ?? '').trim();
    // Show the bound value again: an accepted edit comes back through ngOnChanges, and a
    // rejected one must not stay on screen.
    this.element.textContent = this.text;
    if (value !== this.text) {
      this.edited.emit(value);
    }
  }

  protected finish(event: Event): void {
    event.preventDefault();
    this.element.blur();
  }

  protected revert(): void {
    this.element.textContent = this.text;
    this.element.blur();
  }

  protected pastePlainText(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text/plain') ?? '';
    const selection = window.getSelection();
    if (!selection?.rangeCount) {
      return;
    }
    const range = selection.getRangeAt(0);
    range.deleteContents();
    range.insertNode(document.createTextNode(text.replace(/\s+/g, ' ')));
    selection.collapseToEnd();
  }
}
