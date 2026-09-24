import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

function actualizarTooltipsDeTabla(): void {
  document.querySelectorAll<HTMLTableCellElement>('table.tbl td').forEach((cell) => {
    if (cell.querySelector('button, a, input, select, textarea')) {
      return;
    }
    const texto = cell.textContent?.replace(/\s+/g, ' ').trim() ?? '';
    if (!texto) {
      return;
    }
    if (cell.scrollWidth > cell.clientWidth + 1) {
      cell.dataset['tooltip'] = texto;
      cell.classList.add('cell-tooltip');
      cell.title = texto;
    } else {
      delete cell.dataset['tooltip'];
      cell.classList.remove('cell-tooltip');
      cell.removeAttribute('title');
    }
  });
}

bootstrapApplication(App, appConfig)
  .then(() => {
    const actualizar = () => requestAnimationFrame(actualizarTooltipsDeTabla);
    actualizar();
    const observer = new MutationObserver(actualizar);
    observer.observe(document.body, { childList: true, subtree: true });
    window.addEventListener('resize', actualizar, { passive: true });
  })
  .catch((err) => console.error(err));
