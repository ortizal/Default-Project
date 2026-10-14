-- =====================================================================
-- V38: Caja — Fase F (§3.4)
-- ---------------------------------------------------------------------
-- Sesiones de caja con apertura, movimientos de ingreso y egreso por
-- forma de pago, cierre con monto declarado y conciliación por
-- diferencia. El dinero que entra por la cuenta del paciente (abonos,
-- anticipos y devoluciones) queda vinculado en movimientos_caja cuando
-- hay una sesión abierta: una sola verdad para cobrar y para cuadrar.
--
-- Sólo puede existir UNA caja abierta por tenant a la vez (índice
-- único parcial), que es como trabaja una sola ventanilla.
-- =====================================================================

CREATE TABLE cajas (
    id                     BIGSERIAL PRIMARY KEY,
    tenant_id              BIGINT NOT NULL REFERENCES tenants (id),
    nombre                 VARCHAR(80) NOT NULL DEFAULT 'Caja',
    estado                 VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                           CHECK (estado IN ('ABIERTA', 'CERRADA')),
    fecha                  DATE NOT NULL DEFAULT CURRENT_DATE,
    -- Fondo inicial: siempre en efectivo.
    monto_apertura         NUMERIC(14,2) NOT NULL DEFAULT 0
                           CHECK (monto_apertura >= 0),
    -- Efectivo contado al cerrar (lo que hay en la gaveta).
    monto_declarado        NUMERIC(14,2)
                           CHECK (monto_declarado IS NULL OR monto_declarado >= 0),
    -- declarado − esperado (esperado = apertura + ingresos − egresos en efectivo).
    diferencia             NUMERIC(14,2),
    usuario_id             BIGINT REFERENCES usuarios (id),
    abierto_por            BIGINT REFERENCES usuarios (id),
    cerrado_por            BIGINT REFERENCES usuarios (id),
    abierto_en             TIMESTAMPTZ,
    cerrado_en             TIMESTAMPTZ,
    observaciones_apertura  VARCHAR(500),
    observaciones_cierre   VARCHAR(500),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Una sola caja abierta a la vez dentro del tenant.
CREATE UNIQUE INDEX uq_caja_abierta ON cajas (tenant_id) WHERE estado = 'ABIERTA';
CREATE INDEX idx_cajas_fecha ON cajas (fecha DESC, id DESC);

CREATE TABLE movimientos_caja (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    caja_id            BIGINT NOT NULL REFERENCES cajas (id) ON DELETE CASCADE,
    tipo               VARCHAR(20) NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
    concepto           VARCHAR(255) NOT NULL,
    forma_pago         VARCHAR(20) NOT NULL
                       CHECK (forma_pago IN ('EFECTIVO', 'TRANSFERENCIA',
                                             'TARJETA', 'OTRO')),
    monto              NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    -- Cobro/anticipo/devolución que originó este movimiento (§3.3 ↔ §3.4).
    cuenta_detalle_id  BIGINT REFERENCES cuenta_detalles (id),
    paciente_id        BIGINT REFERENCES pacientes (id),
    fecha              DATE NOT NULL DEFAULT CURRENT_DATE,
    anulado            BOOLEAN NOT NULL DEFAULT FALSE,
    anulado_motivo     VARCHAR(255),
    created_by         BIGINT REFERENCES usuarios (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_mov_caja_origen
        CHECK ((cuenta_detalle_id IS NULL) OR (paciente_id IS NOT NULL))
);

CREATE INDEX idx_movimientos_caja_caja ON movimientos_caja (caja_id, fecha, id);
CREATE INDEX idx_movimientos_caja_fecha ON movimientos_caja (fecha);
CREATE INDEX idx_movimientos_caja_cuenta_detalle
    ON movimientos_caja (cuenta_detalle_id);
CREATE INDEX idx_movimientos_caja_tenant ON movimientos_caja (tenant_id);
