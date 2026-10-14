import { MENU } from './menu';

describe('MENU — menú dinámico por permisos', () => {
  const items = MENU.flatMap((s) => s.items);

  it('todas las secciones tienen ítems', () => {
    expect(MENU.length).toBeGreaterThan(0);
    for (const seccion of MENU) {
      expect(seccion.items.length).toBeGreaterThan(0);
    }
  });

  it('todo ítem (salvo el dashboard) declara el permiso que lo gobierna', () => {
    const sinPermiso = items.filter(
      (i) => i.path !== '/dashboard' && !i.permiso && !i.permisosAlternativos?.length,
    );
    expect(sinPermiso.map((i) => i.path)).toEqual([]);
  });

  it('las rutas son únicas', () => {
    const rutas = items.map((i) => i.path);
    expect(new Set(rutas).size).toBe(rutas.length);
  });

  it('el dashboard siempre es visible (es el destino tras el login)', () => {
    const dashboard = items.find((i) => i.path === '/dashboard');
    expect(dashboard?.permiso).toBeUndefined();
  });

  it('los ítems usan códigos de permiso en mayúsculas con guion bajo', () => {
    for (const item of items) {
      if (item.permiso) expect(item.permiso).toMatch(/^[A-Z][A-Z0-9_]*$/);
    }
  });
});
