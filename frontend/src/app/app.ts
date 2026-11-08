import { Component } from '@angular/core';
import { ComicContainerComponent } from './components/comic-container/comic-container.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ComicContainerComponent],
  template: '<app-comic-container />',
})
export class App {}
