/**
 * El CDK cuelga el contenedor de los overlays (menús de acciones, popups) de
 * <body>, fuera de los landmarks de la página (banner / main / footer), así que
 * la regla `region` de axe lo señalaría como "contenido fuera de landmarks"
 * en cuanto se abre un menú.
 *
 * Se observa la creación de ese contenedor —siempre una hija directa de <body>—
 * para darle un landmark con etiqueta y que también el contenido emergente
 * quede dentro de un landmark. Sin importar CDK: el módulo pesa y sólo se usa
 * un atributo.
 */
export function marcarLandmarkDeOverlays(): void {
  const marcar = (nodo: Node): void => {
    if (!(nodo instanceof HTMLElement)) return;
    if (nodo.classList.contains('cdk-overlay-container')) {
      nodo.setAttribute('role', 'region');
      nodo.setAttribute('aria-label', 'Ventanas emergentes');
    }
  };

  const observador = new MutationObserver((mutaciones) => {
    for (const mutacion of mutaciones) {
      for (const nodo of mutacion.addedNodes) marcar(nodo);
    }
  });
  observador.observe(document.body, { childList: true });

  document.querySelectorAll('.cdk-overlay-container').forEach(marcar);
}
